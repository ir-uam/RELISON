/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.relison.diffusion.metrics.MetricsSimulationConsumer;
import es.uam.eps.ir.relison.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.relison.diffusion.simulation.CollectingSimulationConsumer;
import es.uam.eps.ir.relison.diffusion.simulation.CompositeSimulationConsumer;
import es.uam.eps.ir.relison.diffusion.simulation.Iteration;
import es.uam.eps.ir.relison.diffusion.simulation.Simulation;
import es.uam.eps.ir.relison.diffusion.simulation.Simulator;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.grid.Parameters;
import es.uam.eps.ir.relison.grid.diffusion.SimulationParameterReader;
import es.uam.eps.ir.relison.grid.diffusion.SimulatorSelector;
import es.uam.eps.ir.relison.grid.diffusion.metrics.MetricParameterReader;
import es.uam.eps.ir.relison.grid.diffusion.metrics.MetricSelector;
import es.uam.eps.ir.relison.utils.datatypes.Tuple2oo;
import io.javalin.http.Context;
import org.jooq.lambda.tuple.Tuple2;
import org.ranksys.formats.parsing.Parsers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST handlers for running an information-diffusion simulation over the loaded network and querying its evolution.
 *
 * <p>The frontend configuration is assembled into the YAML-shaped map that {@code SimulationParameterReader} consumes,
 * so the whole RELISON configurator/selector stack ({@code SimulatorSelector}, the protocol/mechanism/stop/filter
 * selectors, {@code MetricSelector}) is reused unchanged. Information pieces are currently seeded synthetically from
 * the graph (each node owns a configurable number of pieces).</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class DiffusionController
{
    private final GraphStore store;

    public DiffusionController(GraphStore store)
    {
        this.store = store;
    }

    /** Handles {@code GET /api/diffusion/catalog}: the configurable elements and their parameters. */
    public void catalog(Context ctx)
    {
        ctx.json(DiffusionCatalog.toJson());
    }

    /**
     * Handles {@code POST /api/diffusion/run}: builds the data, runs the simulation, and stores the result.
     * @param ctx body {@code {graphId, seedCount, protocol, filters, stop, metrics}}.
     */
    @SuppressWarnings("unchecked")
    public void run(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        try
        {
            Graph<String> graph = session.getGraph();
            // Use the information pieces sent in the request if any, otherwise the ones persisted on the session;
            // if there are none, seed synthetically.
            Object piecesObj = body.get("pieces");
            List<?> pieces = (piecesObj instanceof List) ? (List<?>) piecesObj : session.getDiffusionPieces();
            Data<String, String, String> data;
            if (pieces != null && !pieces.isEmpty())
            {
                data = DiffusionData.fromPieces(graph, pieces);
                if (data.getInformationPiecesIndex().numObjects() == 0)   // all pieces invalid → fall back
                    data = DiffusionData.synthetic(graph, intValue(body.get("seedCount"), 1));
            }
            else
            {
                data = DiffusionData.synthetic(graph, intValue(body.get("seedCount"), 1));
            }

            Map<String, Object> simMap = assembleSimulations(body);
            SimulationParameterReader reader = new SimulationParameterReader();
            reader.read(simMap);

            SimulatorSelector<String, String, String> selector = new SimulatorSelector<>(Parsers.sp);
            Tuple2oo<Simulator<String, String, String>, DataFilter<String, String, String>> pair =
                    selector.select(reader, 0, "", null);
            if (pair == null || pair.v1() == null)
            {
                ctx.status(400).json(Map.of("error", "The diffusion configuration is invalid."));
                return;
            }

            Data<String, String, String> filtered = pair.v2().filter(data);
            Simulator<String, String, String> simulator = pair.v1();
            simulator.initialize(filtered);

            // Build the requested metric objects, then run the simulation once: a single streaming pass both collects
            // the iterations (for the canvas + per-node state endpoint) and folds them into the metric accumulators,
            // instead of re-traversing the simulation once per metric.
            Map<String, SimulationMetric<String, String, String>> metricObjs = new LinkedHashMap<>();
            Map<String, String> labels = new LinkedHashMap<>();
            buildMetricObjects(body.get("metrics"), metricObjs, labels);

            CollectingSimulationConsumer<String, String, String> collector =
                    new CollectingSimulationConsumer<>(filtered, 0, null);
            MetricsSimulationConsumer<String, String, String> metricConsumer =
                    new MetricsSimulationConsumer<>(filtered, metricObjs);
            simulator.simulate(new CompositeSimulationConsumer<>(collector, metricConsumer));

            Simulation<String, String, String> simulation = collector.getSimulation();
            Map<String, List<Double>> series = metricConsumer.getResults();

            List<DiffusionResult.IterationSummary> iterations = new ArrayList<>();
            for (int i = 0; i < simulation.getNumIterations(); ++i)
            {
                Iteration<String, String, String> it = simulation.getIteration(i);
                List<String> propagating = it.getPropagatingUsers().collect(Collectors.toList());
                List<String> newlyInformed = it.getReceivingUsers().collect(Collectors.toList());
                iterations.add(new DiffusionResult.IterationSummary(propagating, newlyInformed));
            }

            DiffusionResult result = new DiffusionResult(filtered, simulation, iterations, series, labels);
            session.setDiffusion(result);
            ctx.json(result.toJson());
        }
        catch (Exception | NoClassDefFoundError | UnsatisfiedLinkError e)
        {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "Diffusion simulation failed: " + e));
        }
    }

    /**
     * Handles {@code POST /api/diffusion/state}: the per-node, per-iteration information for the right panel.
     * @param ctx body {@code {graphId, iteration, node}}.
     */
    public void state(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        DiffusionResult result = session.getDiffusion();
        if (result == null)
        {
            ctx.status(400).json(Map.of("error", "No diffusion result. Run a simulation first."));
            return;
        }

        String node = String.valueOf(body.get("node"));
        int iter = intValue(body.get("iteration"), 0);
        if (iter < 0) iter = 0;
        if (iter >= result.numIterations) iter = result.numIterations - 1;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("node", node);
        out.put("iteration", iter);

        if (node != null && result.data.getUserIndex().object2idx(node) != -1 && iter >= 0)
        {
            // Own information is fixed (the pieces created by the node), so it is the same per-iteration and overall.
            List<String> own = result.data.getPieces(node).collect(Collectors.toList());

            Iteration<String, String, String> it = result.simulation.getIteration(iter);
            List<String> propIter = it.getPropagatedInformation(node).collect(Collectors.toList());
            List<String> readIter = it.getSeenInformation(node).map(Tuple2oo::v1).collect(Collectors.toList());
            List<String> recvIter = receivedThisIteration(it, node);
            List<String> discIter = it.getDiscardedInformation(node).collect(Collectors.toList());

            // Cumulative sets up to and including the current iteration.
            Set<String> propAll = new LinkedHashSet<>(), readAll = new LinkedHashSet<>();
            Set<String> recvAll = new LinkedHashSet<>(), discAll = new LinkedHashSet<>();
            for (int j = 0; j <= iter; ++j)
            {
                Iteration<String, String, String> itj = result.simulation.getIteration(j);
                itj.getPropagatedInformation(node).forEach(propAll::add);
                itj.getSeenInformation(node).forEach(t -> { readAll.add(t.v1()); recvAll.add(t.v1()); });
                itj.getReReceivedInformation(node).forEach(t -> recvAll.add(t.v1()));
                itj.getDiscardedInformation(node).forEach(discAll::add);
            }

            out.put("own", pair(own, own));
            out.put("propagated", pair(propIter, new ArrayList<>(propAll)));
            out.put("read", pair(readIter, new ArrayList<>(readAll)));
            out.put("received", pair(recvIter, new ArrayList<>(recvAll)));
            out.put("discarded", pair(discIter, new ArrayList<>(discAll)));
        }
        else
        {
            List<String> empty = List.of();
            for (String key : new String[]{"own", "propagated", "read", "received", "discarded"})
            {
                out.put(key, pair(empty, empty));
            }
        }
        ctx.json(out);
    }

    /** Pieces a user received (newly seen and re-received) during a single iteration, de-duplicated. */
    private static List<String> receivedThisIteration(Iteration<String, String, String> it, String node)
    {
        Set<String> set = new LinkedHashSet<>();
        it.getSeenInformation(node).forEach(t -> set.add(t.v1()));
        it.getReReceivedInformation(node).forEach(t -> set.add(t.v1()));
        return new ArrayList<>(set);
    }

    /** Wraps the this-iteration and overall lists for one information category. */
    private static Map<String, Object> pair(List<String> iter, List<String> overall)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("iter", iter);
        m.put("overall", overall);
        return m;
    }

    /** Handles {@code POST /api/diffusion/clear}: drops the stored simulation. */
    public void clear(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;
        session.setDiffusion(null);
        ctx.json(Map.of("ok", true));
    }

    /** Handles {@code POST /api/diffusion/pieces}: persists the information pieces on the session. */
    public void savePieces(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;
        Object piecesObj = body.get("pieces");
        List<?> pieces = (piecesObj instanceof List) ? (List<?>) piecesObj : List.of();
        session.setDiffusionPieces(pieces);
        ctx.json(Map.of("count", pieces.size()));
    }

    /** Handles {@code POST /api/diffusion/pieces/get}: returns the information pieces persisted on the session. */
    public void getPieces(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;
        ctx.json(Map.of("pieces", session.getDiffusionPieces()));
    }

    /* --------------------------- configuration assembly --------------------------- */

    @SuppressWarnings("unchecked")
    private Map<String, Object> assembleSimulations(Map<?, ?> body)
    {
        Map<String, Object> sim = new LinkedHashMap<>();
        sim.put("protocol", buildProtocol((Map<?, ?>) body.get("protocol")));
        sim.put("filters", buildFilters(body.get("filters")));
        sim.put("stop", buildStop((Map<?, ?>) body.get("stop")));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("simulations", List.of(sim));
        return root;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildProtocol(Map<?, ?> p)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        if (p != null && "custom".equals(p.get("type")))
        {
            out.put("name", "custom");
            out.put("type", "CUSTOM");
            out.put("selection", buildMechanism("selection", (Map<?, ?>) p.get("selection")));
            out.put("expiration", buildMechanism("expiration", (Map<?, ?>) p.get("expiration")));
            out.put("update", buildMechanism("update", (Map<?, ?>) p.get("update")));
            out.put("propagation", buildMechanism("propagation", (Map<?, ?>) p.get("propagation")));
            out.put("sight", buildMechanism("sight", (Map<?, ?>) p.get("sight")));
        }
        else
        {
            String id = p == null ? null : String.valueOf(p.get("id"));
            out.put("name", id);
            out.put("type", "PRECONFIGURED");
            out.put("params", encodeParams(DiffusionCatalog.find("protocol", id), paramsOf(p)));
        }
        return out;
    }

    private Map<String, Object> buildMechanism(String family, Map<?, ?> m)
    {
        String id = m == null ? null : String.valueOf(m.get("id"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", id);
        out.put("params", encodeParams(DiffusionCatalog.find(family, id), paramsOf(m)));
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildFilters(Object filtersObj)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        if (filtersObj instanceof List)
        {
            List<?> list = (List<?>) filtersObj;
            for (Object o : list)
            {
                if (!(o instanceof Map)) continue;
                Map<?, ?> f = (Map<?, ?>) o;
                String id = String.valueOf(f.get("id"));
                Map<String, Object> enc = encodeParams(DiffusionCatalog.find("filter", id), paramsOf(f));
                out.put(id, enc.isEmpty() ? "" : enc);
            }
        }
        if (out.isEmpty()) out.put("Basic", "");   // always keep at least the no-op filter
        return out;
    }

    /** The stop condition map always carries a {@code params} key (a null/empty params would NPE in the reader). */
    private Map<String, Object> buildStop(Map<?, ?> s)
    {
        String id = s == null ? "Num. iter" : String.valueOf(s.get("id"));
        DiffusionCatalog.Element def = DiffusionCatalog.find("stop", id);
        Map<String, Object> enc = encodeParams(def, paramsOf(s));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", id);
        out.put("params", enc.isEmpty() ? "" : enc);
        return out;
    }

    /** Encodes a provided/defaulted parameter set into the {@code {name:{type,value}}} map the readers expect. */
    private Map<String, Object> encodeParams(DiffusionCatalog.Element def, Map<?, ?> provided)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        if (def == null) return out;
        for (DiffusionCatalog.DiffParam p : def.params)
        {
            Object val = provided == null ? null : provided.get(p.name);
            if (val == null) val = p.def;
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("type", p.type);
            pm.put("value", String.valueOf(val));
            out.put(p.name, pm);
        }
        return out;
    }

    /**
     * Builds the requested metric objects (keyed by id) and their display labels. The metrics are computed in a
     * single streaming pass during the simulation (see {@link MetricsSimulationConsumer}), not here.
     */
    private void buildMetricObjects(Object metricsObj, Map<String, SimulationMetric<String, String, String>> metricObjs,
                                    Map<String, String> labels)
    {
        if (!(metricsObj instanceof List)) return;
        List<?> list = (List<?>) metricsObj;
        MetricSelector<String, String, String> selector = new MetricSelector<>();
        for (Object o : list)
        {
            String id;
            Map<?, ?> params;
            if (o instanceof Map) { Map<?, ?> m = (Map<?, ?>) o; id = String.valueOf(m.get("id")); params = paramsOf(m); }
            else { id = String.valueOf(o); params = null; }

            DiffusionCatalog.Element def = DiffusionCatalog.find("metric", id);
            if (def == null || def.needsFeatures) continue;   // feature/real-prop metrics need uploaded data
            try
            {
                Parameters mp = metricParams(def, params);
                Tuple2<String, SimulationMetric<String, String, String>> metric = selector.select(id, mp);
                if (metric == null || metric.v2() == null) continue;
                metricObjs.put(id, metric.v2());
                labels.put(id, def.label);
            }
            catch (Exception e)
            {
                // skip a metric that cannot be built (e.g. missing data) rather than failing the whole run
            }
        }
    }

    private Parameters metricParams(DiffusionCatalog.Element def, Map<?, ?> provided)
    {
        Map<String, Object> enc = encodeParams(def, provided);
        Object value = enc.isEmpty() ? "" : enc;
        MetricParameterReader mpr = new MetricParameterReader();
        mpr.readMetric(Map.entry(def.id, value));
        return mpr.getParams();
    }

    private static Map<?, ?> paramsOf(Map<?, ?> map)
    {
        Object params = map == null ? null : map.get("params");
        return params instanceof Map ? (Map<?, ?>) params : null;
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
