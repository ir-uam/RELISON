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
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmGridSelector;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmIdentifiers;
import es.uam.eps.ir.relison.links.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.relison.links.linkprediction.Prediction;
import es.uam.eps.ir.relison.links.linkprediction.RecommendationLinkPredictor;
import es.uam.eps.ir.relison.links.linkprediction.filter.LinkPredFastFilters;
import es.uam.eps.ir.relison.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import io.javalin.http.Context;
import org.ranksys.core.util.tuples.Tuple2od;
import org.ranksys.formats.parsing.Parsers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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

    public RecommendationController(GraphStore store)
    {
        this.store = store;
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
                Recommender<String, String> rec = fn.apply(graph, prefData);

                List<RecommendationResult.RecEdge> edges = mode.equals("prediction")
                        ? predict(graph, rec, cutoff, reciprocal)
                        : recommend(graph, rec, cutoff, reciprocal);

                result = new RecommendationResult(label, mode, cutoff, edges);
                session.getRecommendations().put(key, result);
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
