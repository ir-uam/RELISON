/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.feature.SimpleFeatureData;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilters;
import es.uam.eps.ir.relison.graph.Adapters;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.io.graph.GraphReader;
import es.uam.eps.ir.relison.io.graph.TextGraphReader;
import es.uam.eps.ir.relison.io.graph.TextMultiGraphReader;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmGridSelector;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmIdentifiers;
import es.uam.eps.ir.relison.grid.links.recommendation.metrics.RecommMetricGridSelector;
import es.uam.eps.ir.relison.grid.links.recommendation.metrics.RecommendationMetricFunction;
import es.uam.eps.ir.relison.links.data.FastGraphIndex;
import es.uam.eps.ir.relison.links.data.GraphIndex;
import es.uam.eps.ir.relison.links.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.relison.links.linkprediction.Prediction;
import es.uam.eps.ir.relison.links.linkprediction.RecommendationLinkPredictor;
import es.uam.eps.ir.relison.links.linkprediction.filter.LinkPredFastFilters;
import es.uam.eps.ir.relison.links.recommendation.SocialFastFilters;
import es.uam.eps.ir.relison.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.jooq.lambda.tuple.Tuple3;
import org.ranksys.core.util.tuples.Tuple2od;
import org.ranksys.formats.parsing.Parsers;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * REST handlers for training a contact-recommendation / link-prediction algorithm over the loaded network and
 * overlaying its links on the session, reusing RELISON's {@code AlgorithmGridSelector} (the same path the
 * {@code Recommendation} / {@code LinkPrediction} examples follow, trimmed to the no-test-set, no-feature-data case).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class RecommendationController
{
    private final GraphStore store;
    private final JobManager jobs;

    public RecommendationController(GraphStore store, JobManager jobs)
    {
        this.store = store;
        this.jobs = jobs;
    }

    /**
     * Handles {@code GET /api/recommendation/catalog}: returns the curated list of algorithms grouped by family.
     * @param ctx the request context.
     */
    public void catalog(Context ctx)
    {
        ctx.json(RecommendationCatalog.toJson());
    }

    /**
     * Handles {@code POST /api/recommendation/run}: trains a recommender (or link predictor), overlays its links on
     * the session and returns them. Re-selecting an identical model (algorithm + parameters + mode + cutoff) reuses
     * the cached result instead of recomputing it.
     * @param ctx the request context, with body {@code {graphId, algorithm, params, mode, cutoff, reciprocal?}}.
     */
    @SuppressWarnings("unchecked")
    public void run(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        if (session.isMultigraph())
        {
            ctx.status(400).json(Map.of("error", "Recommendation is not available for multigraphs."));
            return;
        }

        String algorithmId = String.valueOf(body.get("algorithm"));
        RecommendationCatalog.AlgorithmDef def = RecommendationCatalog.algorithms().get(algorithmId);
        if (def == null)
        {
            ctx.status(400).json(Map.of("error", "Unknown recommendation algorithm: " + algorithmId));
            return;
        }

        String mode = "prediction".equals(body.get("mode")) ? "prediction" : "recommendation";
        int cutoff = intValue(body.get("cutoff"), 10);
        if (cutoff < 1) cutoff = 1;
        boolean reciprocal = Boolean.TRUE.equals(body.get("reciprocal"));
        Map<?, ?> params = MetricController.paramsOf(body);

        String label = def.label + Grids.suffix(def.params, params);
        String key = label + " · " + mode + " · n=" + cutoff + (reciprocal ? " · recip" : "");

        RecommendationResult result = session.getRecommendations().get(key);
        if (result == null)
        {
            final int cutoffF = cutoff;
            try
            {
                FastGraph<String> graph = (FastGraph<String>) session.getGraph();
                FastPreferenceData<String, String> prefData = GraphSimpleFastPreferenceData.load(graph);

                AlgorithmGridSelector<String> selector = new AlgorithmGridSelector<>(Parsers.sp);
                Grid grid = buildGrid(def, algorithmId, params);
                Map<String, RecommendationAlgorithmFunction<String>> funcs = selector.getRecommenders(algorithmId, grid);
                if (funcs == null || funcs.isEmpty())
                {
                    ctx.status(400).json(Map.of("error", "Algorithm " + algorithmId + " could not be configured."));
                    return;
                }

                RecommendationAlgorithmFunction<String> fn = funcs.values().iterator().next();

                // Train the model and score the candidate links on a worker thread so the Stop button can cancel it
                // (the per-node loop in recommend() honours interruption). The result is only cached on the session
                // after it completes, so a cancelled run leaves no trace.
                result = jobs.run(MetricController.jobKey(body, "recommendation"), () ->
                {
                    Recommender<String, String> rec = fn.apply(graph, prefData);
                    List<RecommendationResult.RecEdge> edges = mode.equals("prediction")
                            ? predict(graph, rec, cutoffF, reciprocal)
                            : recommend(graph, rec, cutoffF, reciprocal);
                    return new RecommendationResult(label, mode, cutoffF, edges);
                });
                session.getRecommendations().put(key, result);
            }
            catch (JobCancelledException e)
            {
                ctx.json(Map.of("cancelled", true));
                return;
            }
            catch (ClassCastException cce)
            {
                ctx.status(400).json(Map.of("error", "This network type does not support recommendation."));
                return;
            }
            catch (Exception | NoClassDefFoundError | UnsatisfiedLinkError e)
            {
                ctx.status(500).json(Map.of("error", "Recommendation failed (" + algorithmId + "): " + e));
                return;
            }
        }

        session.setActiveRecommendation(key);
        ctx.json(response(key, result));
    }

    /* ------------------------------ evaluation ------------------------------ */

    /** Handles {@code GET /api/recommendation/eval-catalog}: the available evaluation metrics. */
    public void evalCatalog(Context ctx)
    {
        ctx.json(RecommendationEvalCatalog.toJson());
    }

    /**
     * Handles {@code POST /api/graph/{id}/recommendation/test}: a multipart upload of the held-out test edge list.
     * The test set is read with the session's directedness (unweighted, no self-loops, as in RELISON's
     * {@code Evaluation} example) and stored on the session for later evaluation runs.
     * @param ctx the request context (multipart field {@code file}).
     */
    public void uploadTest(Context ctx)
    {
        GraphSession session = store.get(ctx.pathParam("id"));
        if (session == null) { ctx.status(404).json(Map.of("error", "Unknown graph id.")); return; }
        UploadedFile file = ctx.uploadedFile("file");
        if (file == null)
        {
            ctx.status(400).json(Map.of("error", "No file was uploaded (expected multipart field 'file')."));
            return;
        }

        Graph<String> test;
        try (InputStream in = file.content())
        {
            GraphReader<String> reader = session.isMultigraph()
                    ? new TextMultiGraphReader<>(session.isDirected(), false, false, "\t", Parsers.sp)
                    : new TextGraphReader<>(session.isDirected(), false, false, "\t", Parsers.sp);
            test = reader.read(in, false, false);
        }
        catch (Exception e)
        {
            ctx.status(400).json(Map.of("error", "Could not read the test network: " + e.getMessage()));
            return;
        }
        if (test == null)
        {
            ctx.status(400).json(Map.of("error", "Could not read the test network (expected a tab-separated edge list)."));
            return;
        }

        session.setTestGraph(test);
        ctx.json(testStats(session, test));
    }

    /**
     * Handles {@code POST /api/recommendation/evaluate}: evaluates every recommendation computed so far in this
     * session against the uploaded test set, returning one row per algorithm and one column per metric.
     *
     * <p>Follows the same protocol as RELISON's {@code Evaluation} example: the test network is restricted to users
     * present in the training network and filtered with the same candidate filter the recommenders used (no training
     * links, no self-links, optionally no reciprocal links), the metrics are built by {@code RecommMetricGridSelector}
     * and fed the stored ranking of each algorithm.</p>
     *
     * @param ctx the request context, with body
     *            {@code {graphId, metrics:[{id,params}], reciprocal?, feature?}}.
     */
    @SuppressWarnings("unchecked")
    public void evaluate(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        if (session.getTestGraph() == null)
        {
            ctx.status(400).json(Map.of("error", "Upload a test set before evaluating."));
            return;
        }
        if (session.getRecommendations().isEmpty())
        {
            ctx.status(400).json(Map.of("error", "Run at least one recommendation algorithm before evaluating."));
            return;
        }

        // Resolve the requested metrics into RELISON metric factories, keyed by their "<metric>@<cutoff>" name.
        Map<String, RecommendationMetricFunction<String, String>> metricFns = new LinkedHashMap<>();
        List<String> unknown = new ArrayList<>();
        Object rawMetrics = body.get("metrics");
        if (rawMetrics instanceof List)
        {
            for (Object o : (List<?>) rawMetrics)
            {
                if (!(o instanceof Map)) continue;
                Map<?, ?> spec = (Map<?, ?>) o;
                String id = String.valueOf(spec.get("id"));
                RecommendationEvalCatalog.MetricDef def = RecommendationEvalCatalog.metrics().get(id);
                if (def == null) { unknown.add(id); continue; }
                Object params = spec.get("params");
                Grid grid = Grids.build(def.params, params instanceof Map ? (Map<?, ?>) params : null);
                Map<String, RecommendationMetricFunction<String, String>> built =
                        new RecommMetricGridSelector<String, String>().getMetrics(id, grid);
                if (built == null || built.isEmpty()) { unknown.add(id); continue; }
                metricFns.putAll(built);
            }
        }
        if (metricFns.isEmpty())
        {
            ctx.status(400).json(Map.of("error", "Select at least one valid evaluation metric."
                    + (unknown.isEmpty() ? "" : " Unknown: " + String.join(", ", unknown))));
            return;
        }

        boolean reciprocal = Boolean.TRUE.equals(body.get("reciprocal"));
        String featureAttr = body.get("feature") == null ? null : String.valueOf(body.get("feature"));
        // "test" (default) evaluates only users with test links; "all" feeds every ranked user.
        boolean allUsers = "all".equals(String.valueOf(body.get("population")));

        try
        {
            Map<String, Object> response = jobs.run(MetricController.jobKey(body, "evaluation"),
                    () -> runEvaluation(session, metricFns, reciprocal, featureAttr, allUsers));
            if (!unknown.isEmpty()) response.put("skipped", unknown);
            ctx.json(response);
        }
        catch (JobCancelledException e)
        {
            ctx.json(Map.of("cancelled", true));
        }
        catch (ClassCastException cce)
        {
            ctx.status(400).json(Map.of("error", "This network type does not support recommendation evaluation."));
        }
        catch (Exception | NoClassDefFoundError | UnsatisfiedLinkError e)
        {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "Evaluation failed: " + e));
        }
    }

    /**
     * Runs the evaluation itself (on a worker thread, so it can be stopped): prepares the train/test preference data
     * and the metric inputs once, then feeds every algorithm's stored ranking through a fresh set of metric objects.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> runEvaluation(GraphSession session,
                                              Map<String, RecommendationMetricFunction<String, String>> metricFns,
                                              boolean reciprocal, String featureAttr, boolean allUsers)
    {
        FastGraph<String> graph = (FastGraph<String>) session.getGraph();
        FastPreferenceData<String, String> trainData = GraphSimpleFastPreferenceData.load(graph);
        GraphIndex<String> index = new FastGraphIndex<>(graph);

        // The same candidate filter the recommenders applied, so the test set only keeps links that could have been
        // recommended (otherwise recall-style metrics are penalised by unreachable ground truth).
        Function<String, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(trainData), FastFilters.notSelf(index));
        if (!reciprocal) filter = FastFilters.and(filter, SocialFastFilters.notReciprocal(graph, index));

        // onlyTrainUsers must run BEFORE filteredGraph, and not only to drop unknown users: it seeds the new graph
        // with every training node, in training-index order, so the resulting graph's vertex index is identical to
        // the training one. filteredGraph resolves candidates through *that* index, while the filters above resolve
        // them through the training GraphIndex — feeding filteredGraph the raw uploaded test graph would index it by
        // whatever nodes appear in the test file instead, so notReciprocal/notInTrain would silently test the wrong
        // user and drop arbitrary links without erroring. Do not reorder these two lines.
        Graph<String> onlyTrain = Adapters.onlyTrainUsers(session.getTestGraph(), graph);
        FastGraph<String> testGraph = (FastGraph<String>) Adapters.filteredGraph(onlyTrain, filter);
        FastPreferenceData<String, String> testData = GraphSimpleFastPreferenceData.load(testGraph);

        FeatureData<String, String, Double> featureData = buildFeatureData(graph, featureAttr);
        Communities<String> comms = singleCommunity(graph);

        // Which users are fed to the metrics.
        //
        // "test" (default): only users with at least one test link. Recall divides by the number of relevant items and
        // MAP by min(cutoff, |relevant|), both unguarded, so a user with no test links yields 0/0 = NaN — and
        // AverageRecommendationMetric is built with ignoreNaN = false, so a single such user would turn those whole
        // columns into NaN. Restricting the population keeps them well-defined.
        //
        // "all": every user the recommender ranked. Precision and nDCG are identical either way (a user with nothing
        // relevant scores 0, and the accuracy denominator is fixed at testData.numUsersWithPreferences()), but the
        // novelty/diversity metrics — which self-average over whatever they are fed — then cover the full recommended
        // population. Recall and MAP are genuinely undefined here and are reported as such rather than as NaN.
        Set<String> targets = allUsers ? null : testData.getUsersWithPreferences().collect(Collectors.toSet());

        List<String> metricNames = new ArrayList<>(metricFns.keySet());
        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> undefined = new LinkedHashSet<>();   // metrics that came out undefined for at least one algorithm
        for (Map.Entry<String, RecommendationResult> entry : session.getRecommendations().entrySet())
        {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();

            // A metric object accumulates state, so each algorithm needs its own freshly-built set.
            Map<String, SystemMetric<String, String>> metrics = new LinkedHashMap<>();
            metricFns.forEach((name, fn) -> metrics.put(name, fn.apply(graph, testGraph, trainData, testData, featureData, comms)));

            List<Recommendation<String, String>> ranking = toRecommendations(entry.getValue(), targets);
            for (Recommendation<String, String> r : ranking)
            {
                for (SystemMetric<String, String> metric : metrics.values()) metric.add(r);
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("algorithm", entry.getKey());
            row.put("label", entry.getValue().label);
            row.put("mode", entry.getValue().mode);
            row.put("cutoff", entry.getValue().cutoff);
            row.put("users", ranking.size());
            Map<String, Object> values = new LinkedHashMap<>();
            metrics.forEach((name, metric) ->
            {
                double value = metric.evaluate();
                // A metric that divided by an empty relevant set (Recall / MAP over users with no test links) is
                // undefined, not zero. Report it as null so the table shows "—" instead of a misleading NaN.
                if (Double.isNaN(value)) { values.put(name, null); undefined.add(name); }
                else values.put(name, value);
            });
            row.put("values", values);
            rows.add(row);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", metricNames);
        out.put("rows", rows);
        out.put("test", testStats(session, session.getTestGraph()));
        out.put("evaluatedLinks", testGraph.getEdgeCount());
        out.put("evaluatedUsers", testData.numUsersWithPreferences());
        out.put("population", allUsers ? "all" : "test");
        out.put("undefined", new ArrayList<>(undefined));
        return out;
    }

    /**
     * Rebuilds the per-user rankings a recommender produced from the flat edge list stored on the session: the edges
     * are grouped by source user and sorted by descending score, which is the form the RankSys metrics consume.
     *
     * @param result  the stored recommendation.
     * @param targets the users to evaluate, or {@code null} to evaluate every ranked user.
     */
    private List<Recommendation<String, String>> toRecommendations(RecommendationResult result, Set<String> targets)
    {
        Map<String, List<Tuple2od<String>>> byUser = new LinkedHashMap<>();
        for (RecommendationResult.RecEdge e : result.edges)
        {
            if (targets != null && !targets.contains(e.source)) continue;
            byUser.computeIfAbsent(e.source, u -> new ArrayList<>()).add(new Tuple2od<>(e.target, e.score));
        }
        List<Recommendation<String, String>> out = new ArrayList<>();
        byUser.forEach((user, items) ->
        {
            items.sort((a, b) -> Double.compare(b.v2, a.v2));
            out.add(new Recommendation<>(user, items));
        });
        return out;
    }

    /**
     * A partition holding every user in a single community. None of the catalogued metrics is community-based, but
     * RELISON's metric factories take a partition regardless, so this supplies the neutral one its example uses as a
     * fallback.
     */
    private Communities<String> singleCommunity(Graph<String> graph)
    {
        Communities<String> all = new Communities<>();
        all.addCommunity();
        graph.getAllNodes().forEach(node -> all.add(node, 0));
        return all;
    }

    /**
     * Feature data for the feature-based metrics (ILD, Unexpectedness), derived from a node attribute: each user gets
     * its attribute value as a single feature. With no attribute chosen the data is empty, which is what RELISON's
     * example does when no feature file is supplied.
     */
    private FeatureData<String, String, Double> buildFeatureData(Graph<String> graph, String attribute)
    {
        if (attribute == null || attribute.isEmpty()) return SimpleFeatureData.load(Stream.empty());
        List<Tuple3<String, String, Double>> tuples = new ArrayList<>();
        graph.getAllNodes().forEach(node ->
        {
            Object value = graph.getNodeAttribute(node, attribute);
            if (value != null) tuples.add(new Tuple3<>(node, String.valueOf(value), 1.0));
        });
        return SimpleFeatureData.load(tuples.stream());
    }

    /** A short description of an uploaded test network. */
    private Map<String, Object> testStats(GraphSession session, Graph<String> test)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nodes", test.getVertexCount());
        out.put("edges", test.getEdgeCount());
        // How much of the test set is usable: links whose endpoints both exist in the training network.
        Graph<String> train = session.getGraph();
        long shared = test.getAllNodes().filter(train::containsVertex).count();
        out.put("sharedUsers", shared);
        return out;
    }

    /**
     * Handles {@code POST /api/recommendation/clear}: removes the overlaid recommendation from the session.
     * @param ctx the request context, with body {@code {graphId}}.
     */
    public void clear(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;
        session.setActiveRecommendation(null);
        ctx.json(Map.of("ok", true));
    }

    /**
     * Builds the parameter grid for an algorithm. Most algorithms use a flat grid; the k-NN algorithms need a nested
     * similarity grid (a Cosine recommender parameterised by uSel/vSel), which is assembled here.
     */
    private Grid buildGrid(RecommendationCatalog.AlgorithmDef def, String algorithmId, Map<?, ?> params)
    {
        Grid grid = Grids.build(def.params, params);
        if (algorithmId.equals(AlgorithmIdentifiers.UB) || algorithmId.equals(AlgorithmIdentifiers.IB))
        {
            // The k-NN grid reads k/q/weighted at the top level and the similarity from a nested "sim" grid; move the
            // uSel/vSel/weighted orientations into a Cosine sub-grid that defines that similarity.
            Grid simGrid = new Grid();
            simGrid.getOrientationValues().put("uSel", grid.getOrientationValues().getOrDefault("uSel", List.of()));
            simGrid.getOrientationValues().put("vSel", grid.getOrientationValues().getOrDefault("vSel", List.of()));
            simGrid.getBooleanValues().put("weighted", grid.getBooleanValues().getOrDefault("weighted", List.of(false)));
            grid.getGridValues().put("sim", Map.of(AlgorithmIdentifiers.COSINE, simGrid));
        }
        return grid;
    }

    /**
     * Computes a per-node recommendation: for every node, ranks the valid candidate links and keeps the top
     * {@code cutoff} of them.
     */
    private List<RecommendationResult.RecEdge> recommend(FastGraph<String> graph, Recommender<String, String> rec, int cutoff, boolean reciprocal)
    {
        List<String> nodes = new ArrayList<>();
        graph.getAllNodes().forEach(nodes::add);
        List<RecommendationResult.RecEdge> edges = new ArrayList<>();
        for (String u : nodes)
        {
            // Cooperative cancellation: stop scoring when the Stop button interrupted this worker thread.
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            Recommendation<String, String> r = rec.getRecommendation(u,
                    nodes.stream().filter(v -> !v.equals(u)
                            && !graph.containsEdge(u, v)
                            && (reciprocal || !graph.containsEdge(v, u))));
            r.getItems().stream().limit(cutoff).forEach(item ->
                    edges.add(new RecommendationResult.RecEdge(u, item.v1, item.v2)));
        }
        return edges;
    }

    /**
     * Computes a link prediction: ranks every valid candidate link across the whole network and keeps the top
     * {@code cutoff} of them.
     */
    private List<RecommendationResult.RecEdge> predict(FastGraph<String> graph, Recommender<String, String> rec, int cutoff, boolean reciprocal)
    {
        Predicate<Pair<String>> filter = LinkPredFastFilters.and(
                LinkPredFastFilters.onlyNewLinks(graph), LinkPredFastFilters.notSelf());
        if (!reciprocal)
        {
            filter = LinkPredFastFilters.and(filter, LinkPredFastFilters.notReciprocal(graph));
        }

        RecommendationLinkPredictor<String> predictor = new RecommendationLinkPredictor<>(graph, rec);
        Prediction<String> prediction = predictor.getPrediction(cutoff, filter);

        List<RecommendationResult.RecEdge> edges = new ArrayList<>();
        for (Tuple2od<Pair<String>> link : prediction.getPrediction())
        {
            edges.add(new RecommendationResult.RecEdge(link.v1.v1(), link.v1.v2(), link.v2));
        }
        return edges;
    }

    /** Builds the JSON payload describing an (overlaid) recommendation. */
    private Map<String, Object> response(String key, RecommendationResult result)
    {
        List<Map<String, Object>> edges = new ArrayList<>();
        for (RecommendationResult.RecEdge e : result.edges)
        {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("source", e.source);
            entry.put("target", e.target);
            entry.put("score", e.score);
            edges.add(entry);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("key", key);
        out.put("label", result.label);
        out.put("mode", result.mode);
        out.put("cutoff", result.cutoff);
        out.put("count", edges.size());
        out.put("edges", edges);
        return out;
    }

    private static int intValue(Object value, int def)
    {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value == null) return def;
        try { return Integer.parseInt(value.toString().trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private GraphSession requireSession(Context ctx, Map<?, ?> body)
    {
        Object graphId = body.get("graphId");
        GraphSession session = graphId == null ? null : store.get(String.valueOf(graphId));
        if (session == null)
        {
            ctx.status(404).json(Map.of("error", "Unknown graph id."));
            return null;
        }
        return session;
    }
}
