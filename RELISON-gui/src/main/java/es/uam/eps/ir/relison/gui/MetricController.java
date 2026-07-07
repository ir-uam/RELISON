/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.sna.graph.GraphMetricSelector;
import es.uam.eps.ir.relison.grid.sna.pair.PairMetricIdentifiers;
import es.uam.eps.ir.relison.grid.sna.pair.PairMetricSelector;
import es.uam.eps.ir.relison.grid.sna.vertex.VertexMetricSelector;
import es.uam.eps.ir.relison.sna.metrics.GraphMetric;
import es.uam.eps.ir.relison.sna.metrics.PairMetric;
import es.uam.eps.ir.relison.sna.metrics.VertexMetric;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * REST handlers for computing vertex and graph-global metrics, plus the metric catalog.
 *
 * <p>Each handler looks up the requested metric in {@link MetricCatalog}, builds its default parameter grid, asks
 * the matching RELISON selector for a configured metric, and computes it over the session's graph — the same path
 * {@code GraphAnalyzer} follows.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class MetricController
{
    private final GraphStore store;

    public MetricController(GraphStore store)
    {
        this.store = store;
    }

    /**
     * Handles {@code GET /api/metrics}: returns the catalog of available metrics grouped by family.
     * @param ctx the request context.
     */
    public void catalog(Context ctx)
    {
        ctx.json(MetricCatalog.toJson());
    }

    /**
     * Handles {@code POST /api/metrics/vertex}: computes a per-node vertex metric.
     * @param ctx the request context, with body {@code {graphId, metric}}.
     */
    public void vertex(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        String metricId = String.valueOf(body.get("metric"));
        MetricCatalog.MetricDef def = MetricCatalog.vertexMetrics().get(metricId);
        if (def == null)
        {
            ctx.status(400).json(Map.of("error", "Unknown vertex metric: " + metricId));
            return;
        }

        boolean withRec = useRecommendation(body, session);
        Graph<String> graph = withRec ? session.getAugmentedGraph() : session.getGraph();
        DistanceCalculator<String> dc = withRec ? session.getAugmentedDistanceCalculator() : session.getDistanceCalculator();

        Grid grid = Grids.build(def.params, paramsOf(body));
        VertexMetricSelector<String> selector = new VertexMetricSelector<>();
        Map<String, Supplier<VertexMetric<String>>> metrics =
                selector.getMetrics(metricId, grid, dc);
        if (metrics == null || metrics.isEmpty())
        {
            ctx.status(400).json(Map.of("error", "Metric " + metricId + " could not be configured."));
            return;
        }

        VertexMetric<String> metric = metrics.values().iterator().next().get();
        Map<String, Double> values = metric.compute(graph);

        Map<String, Double> out = new LinkedHashMap<>();
        values.forEach(out::put);
        double average = values.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("metric", metricId);
        response.put("label", def.label + Grids.suffix(def.params, paramsOf(body)));
        response.put("values", out);
        response.put("average", average);
        response.put("recommendation", withRec ? session.getActiveRecommendationKey() : null);
        ctx.json(response);
    }

    /**
     * Handles {@code POST /api/metrics/graph}: computes a single graph-global metric.
     * @param ctx the request context, with body {@code {graphId, metric}}.
     */
    public void graph(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        String metricId = String.valueOf(body.get("metric"));
        MetricCatalog.MetricDef def = MetricCatalog.graphMetrics().get(metricId);
        if (def == null)
        {
            ctx.status(400).json(Map.of("error", "Unknown graph metric: " + metricId));
            return;
        }

        boolean withRec = useRecommendation(body, session);
        Graph<String> graph = withRec ? session.getAugmentedGraph() : session.getGraph();
        DistanceCalculator<String> dc = withRec ? session.getAugmentedDistanceCalculator() : session.getDistanceCalculator();

        Grid grid = Grids.build(def.params, paramsOf(body));
        GraphMetricSelector<String> selector = new GraphMetricSelector<>();
        Map<String, Supplier<GraphMetric<String>>> metrics =
                selector.getMetrics(metricId, grid, dc);
        if (metrics == null || metrics.isEmpty())
        {
            ctx.status(400).json(Map.of("error", "Metric " + metricId + " could not be configured."));
            return;
        }

        GraphMetric<String> metric = metrics.values().iterator().next().get();
        double value = metric.compute(graph);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("metric", metricId);
        response.put("label", def.label + Grids.suffix(def.params, paramsOf(body)));
        response.put("value", value);
        response.put("recommendation", withRec ? session.getActiveRecommendationKey() : null);
        ctx.json(response);
    }

    /**
     * Handles {@code POST /api/metrics/pair}: computes a pair/edge metric. By default it is computed only over the
     * existing links ({@code computeOnlyLinks}); pass {@code onlyLinks:false} to summarise the metric over all node
     * pairs. The all-pairs case never materialises the N×N product: it returns only an aggregate (average +
     * histogram), computed exactly for small graphs and from a random sample of pairs for large ones, so it scales
     * to networks with tens of thousands of nodes.
     * @param ctx the request context, with body {@code {graphId, metric, onlyLinks?}}.
     */
    public void pair(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        String metricId = String.valueOf(body.get("metric"));
        MetricCatalog.MetricDef def = PairMetricCatalog.pairMetrics().get(metricId);
        if (def == null)
        {
            ctx.status(400).json(Map.of("error", "Unknown pair metric: " + metricId));
            return;
        }

        boolean onlyLinks = !Boolean.FALSE.equals(body.get("onlyLinks"));
        boolean withRec = useRecommendation(body, session);
        Graph<String> graph = withRec ? session.getAugmentedGraph() : session.getGraph();
        DistanceCalculator<String> dc = withRec ? session.getAugmentedDistanceCalculator() : session.getDistanceCalculator();
        String recKey = withRec ? session.getActiveRecommendationKey() : null;

        Grid grid = Grids.build(def.params, paramsOf(body));
        PairMetricSelector<String> selector = new PairMetricSelector<>();
        Map<String, Supplier<PairMetric<String>>> metrics =
                selector.getMetrics(metricId, grid, dc);
        if (metrics == null || metrics.isEmpty())
        {
            ctx.status(400).json(Map.of("error", "Metric " + metricId + " could not be configured."));
            return;
        }

        PairMetric<String> metric = metrics.values().iterator().next().get();
        String label = def.label + Grids.suffix(def.params, paramsOf(body));

        if (onlyLinks)
        {
            pairLinks(ctx, session, graph, recKey, metricId, label, metric);
        }
        else
        {
            pairAggregate(ctx, session, graph, recKey, metricId, label, metric);
        }
    }

    /** Returns the full per-link values (bounded by the number of edges). */
    private void pairLinks(Context ctx, GraphSession session, Graph<String> graph, String recKey, String metricId, String label, PairMetric<String> metric)
    {
        Map<Pair<String>, Double> values = metric.computeOnlyLinks(graph);
        List<Map<String, Object>> out = new ArrayList<>();
        values.forEach((pair, value) ->
        {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("source", pair.v1());
            entry.put("target", pair.v2());
            entry.put("value", value);
            out.add(entry);
        });
        double average = values.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("metric", metricId);
        response.put("label", label);
        response.put("onlyLinks", true);
        response.put("count", out.size());
        response.put("values", out);
        response.put("average", average);
        response.put("recommendation", recKey);
        ctx.json(response);
    }

    /** Number of node pairs above which non-distance metrics estimate the aggregate from a random sample. */
    private static final long EXACT_LIMIT = 5_000_000L;
    /** Number of pairs sampled when a (non-distance) network is too large to enumerate exactly. */
    private static final int SAMPLE_SIZE = 2_000_000;
    /** Number of histogram buckets returned for the distribution. */
    private static final int HISTOGRAM_BINS = 40;

    /**
     * Pair metrics derived from all-pairs shortest paths. RELISON's {@code CompleteDistanceCalculator} already
     * stores every distance once computed, so enumerating all pairs is just cheap lookups — these are therefore
     * always computed exactly (never sampled), even for very large networks.
     */
    private static final Set<String> DISTANCE_BASED = Set.of(
            PairMetricIdentifiers.DISTANCE,
            PairMetricIdentifiers.GEODESICS,
            PairMetricIdentifiers.BETWEENNESS);

    /**
     * Summarises a pair metric over all node pairs without materialising them: streams values into an average,
     * min/max and a histogram. Distance-based metrics are always enumerated exactly (their values are already
     * cached); other metrics are enumerated exactly when small and sampled when large.
     */
    private void pairAggregate(Context ctx, GraphSession session, Graph<String> graph, String recKey, String metricId, String label, PairMetric<String> metric)
    {
        boolean directed = session.isDirected();
        List<String> nodes = new ArrayList<>();
        graph.getAllNodes().forEach(nodes::add);
        int n = nodes.size();
        long totalPairs = directed ? (long) n * (n - 1) : (long) n * (n - 1) / 2;
        boolean distanceBased = DISTANCE_BASED.contains(metricId);
        boolean estimated = !distanceBased && totalPairs > EXACT_LIMIT;

        // First pass: average, min/max, finite/infinite counts.
        double[] acc = new double[]{0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}; // sum, min, max
        long[] tally = new long[]{0, 0}; // finite, infinite
        java.util.function.DoubleConsumer collect = v ->
        {
            if (Double.isFinite(v)) { acc[0] += v; if (v < acc[1]) acc[1] = v; if (v > acc[2]) acc[2] = v; tally[0]++; }
            else tally[1]++;
        };

        long evaluated;
        if (estimated)
        {
            evaluated = samplePairs(graph, nodes, metric, collect);
        }
        else
        {
            forEachPair(graph, nodes, directed, metric, collect);
            evaluated = totalPairs;
        }

        long finite = tally[0], infinite = tally[1];
        double sum = acc[0], min = acc[1], max = acc[2];
        double average = finite > 0 ? sum / finite : 0.0;
        double range = (finite > 0 && max > min) ? max - min : 1.0;

        // Second pass: fill the histogram (O(1) memory; values were never stored).
        long[] counts = new long[HISTOGRAM_BINS];
        if (finite > 0)
        {
            double lo = min, span = range;
            java.util.function.DoubleConsumer bin = v ->
            {
                if (!Double.isFinite(v)) return;
                int idx = (int) ((v - lo) / span * HISTOGRAM_BINS);
                if (idx >= HISTOGRAM_BINS) idx = HISTOGRAM_BINS - 1;
                if (idx < 0) idx = 0;
                counts[idx]++;
            };
            if (estimated) samplePairs(graph, nodes, metric, bin);
            else forEachPair(graph, nodes, directed, metric, bin);
        }

        List<Map<String, Object>> histogram = new ArrayList<>();
        for (int b = 0; finite > 0 && b < HISTOGRAM_BINS; b++)
        {
            Map<String, Object> binObj = new LinkedHashMap<>();
            binObj.put("x0", min + (range * b) / HISTOGRAM_BINS);
            binObj.put("x1", min + (range * (b + 1)) / HISTOGRAM_BINS);
            binObj.put("count", counts[b]);
            histogram.add(binObj);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("metric", metricId);
        response.put("label", label);
        response.put("onlyLinks", false);
        response.put("estimated", estimated);
        response.put("totalPairs", totalPairs);
        response.put("evaluated", evaluated);
        response.put("finite", finite);
        response.put("infinite", infinite);
        response.put("average", average);
        response.put("min", finite > 0 ? min : 0.0);
        response.put("max", finite > 0 ? max : 0.0);
        response.put("histogram", histogram);
        response.put("recommendation", recKey);
        ctx.json(response);
    }

    /** Applies the consumer to the metric value of every ordered (directed) / unordered (undirected) node pair. */
    private void forEachPair(Graph<String> graph, List<String> nodes, boolean directed, PairMetric<String> metric, java.util.function.DoubleConsumer consumer)
    {
        int n = nodes.size();
        for (int i = 0; i < n; i++)
        {
            int jStart = directed ? 0 : i + 1;
            for (int j = jStart; j < n; j++)
            {
                if (directed && i == j) continue;
                consumer.accept(metric.compute(graph, nodes.get(i), nodes.get(j)));
            }
        }
    }

    /** Applies the consumer to a fixed-size random sample of distinct node pairs (same seed for both passes). */
    private long samplePairs(Graph<String> graph, List<String> nodes, PairMetric<String> metric, java.util.function.DoubleConsumer consumer)
    {
        int n = nodes.size();
        Random rnd = new Random(42);
        long taken = 0;
        for (int k = 0; k < SAMPLE_SIZE; k++)
        {
            int i = rnd.nextInt(n), j = rnd.nextInt(n);
            if (i == j) continue;
            consumer.accept(metric.compute(graph, nodes.get(i), nodes.get(j)));
            taken++;
        }
        return taken;
    }

    /**
     * Resolves the session referenced by a request body, writing a 404 response and returning {@code null} when
     * the graph id is missing or unknown.
     * @param ctx  the request context.
     * @param body the parsed request body.
     * @return the session, or {@code null} if it could not be resolved.
     */
    /**
     * Extracts the optional {@code params} object from a request body.
     * @param body the parsed request body.
     * @return the parameter map, or {@code null} if none was supplied.
     */
    static Map<?, ?> paramsOf(Map<?, ?> body)
    {
        Object params = body.get("params");
        return params instanceof Map ? (Map<?, ?>) params : null;
    }

    /**
     * Determines whether a metric should be computed over the graph augmented with the active recommendation:
     * only when the request asks for it ({@code withRecommendation:true}) and a recommendation is currently overlaid.
     * @param body    the parsed request body.
     * @param session the resolved session.
     * @return {@code true} to compute over the augmented graph.
     */
    static boolean useRecommendation(Map<?, ?> body, GraphSession session)
    {
        return Boolean.TRUE.equals(body.get("withRecommendation")) && session.getActiveRecommendation() != null;
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
