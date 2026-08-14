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
import es.uam.eps.ir.relison.diffusion.metrics.distributions.Distribution;
import es.uam.eps.ir.relison.diffusion.metrics.distributions.InformationFeatureDistribution;
import es.uam.eps.ir.relison.diffusion.metrics.distributions.MixedFeatureDistribution;
import es.uam.eps.ir.relison.diffusion.metrics.distributions.UserFeatureDistribution;
import es.uam.eps.ir.relison.diffusion.seed.FixedNumberPieceSeeder;
import es.uam.eps.ir.relison.diffusion.seed.PieceSeeder;
import es.uam.eps.ir.relison.diffusion.seed.PoissonPieceSeeder;
import es.uam.eps.ir.relison.diffusion.seed.UniformRandomPieceSeeder;
import es.uam.eps.ir.relison.diffusion.seed.selector.AllUsersSelector;
import es.uam.eps.ir.relison.diffusion.seed.selector.CommunityUserSelector;
import es.uam.eps.ir.relison.diffusion.seed.selector.RandomUserSelector;
import es.uam.eps.ir.relison.diffusion.seed.selector.UserSelector;
import es.uam.eps.ir.relison.diffusion.seed.selector.ValuesUserSelector;
import es.uam.eps.ir.relison.diffusion.seed.timestamp.FixedTimestampGenerator;
import es.uam.eps.ir.relison.diffusion.seed.timestamp.TimestampGenerator;
import es.uam.eps.ir.relison.diffusion.seed.timestamp.UniformTimestampGenerator;
import es.uam.eps.ir.relison.diffusion.simulation.CompositeSimulationConsumer;
import es.uam.eps.ir.relison.diffusion.simulation.Iteration;
import es.uam.eps.ir.relison.diffusion.simulation.Simulator;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.utils.generator.Generator;
import es.uam.eps.ir.relison.grid.Parameters;
import es.uam.eps.ir.relison.grid.diffusion.SimulationParameterReader;
import es.uam.eps.ir.relison.grid.diffusion.SimulatorSelector;
import es.uam.eps.ir.relison.grid.diffusion.metrics.MetricParameterReader;
import es.uam.eps.ir.relison.grid.diffusion.metrics.MetricSelector;
import es.uam.eps.ir.relison.utils.datatypes.Tuple2oo;
import io.javalin.http.Context;
import org.jooq.lambda.tuple.Tuple2;
import org.ranksys.formats.parsing.Parsers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private final JobManager jobs;

    public DiffusionController(GraphStore store, JobManager jobs)
    {
        this.store = store;
        this.jobs = jobs;
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
            // Run over the base graph, or — when the request asks for it and a recommendation is overlaid — over the
            // graph augmented with the active recommendation's edges, so information can spread along recommended links.
            boolean withRec = MetricController.useRecommendation(body, session);
            Graph<String> graph = withRec ? session.getAugmentedGraph() : session.getGraph();
            // Use the information pieces sent in the request if any, otherwise the ones persisted on the session;
            // if there are none, seed synthetically.
            Object piecesObj = body.get("pieces");
            List<?> pieces = (piecesObj instanceof List) ? (List<?>) piecesObj : session.getDiffusionPieces();
            // "Real propagated" records (which pieces each user repropagated in the real scenario) — from the request
            // if present, otherwise the ones persisted on the session. Only meaningful with an explicit piece set.
            Object realPropObj = body.get("realPropagated");
            List<?> realProp = (realPropObj instanceof List) ? (List<?>) realPropObj : session.getRealPropagated();
            Data<String, String, String> data;
            if (pieces != null && !pieces.isEmpty())
            {
                data = DiffusionData.fromPieces(graph, pieces, session.getCommunities(), realProp);
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

            // Build the requested metric objects, then run the simulation once: a single streaming pass streams each
            // iteration to a binary file (keeping only compact per-iteration summaries in memory, for the canvas) and
            // folds the iterations into the metric accumulators. The full simulation is never retained — the per-node /
            // per-piece / distribution endpoints read the iterations back from the file on demand — so peak memory is
            // proportional to a single iteration rather than the whole run.
            Map<String, SimulationMetric<String, String, String>> metricObjs = new LinkedHashMap<>();
            Map<String, String> labels = new LinkedHashMap<>();
            buildMetricObjects(body.get("metrics"), metricObjs, labels);

            // A unique path in the temp dir, but not pre-created: BinarySimulationWriter would otherwise delete and
            // recreate it, and a freshly-created temp file can still be momentarily locked on Windows (indexer / AV),
            // making that delete+reopen fail and silently leaving an empty file.
            File simFile = new File(System.getProperty("java.io.tmpdir"), "relison-sim-" + java.util.UUID.randomUUID() + ".bin");
            simFile.deleteOnExit();   // safety net: normally removed via setDiffusion, but clean up on a crash too
            StreamingSimulationConsumer streamer = new StreamingSimulationConsumer(filtered, simFile.getAbsolutePath());
            MetricsSimulationConsumer<String, String, String> metricConsumer =
                    new MetricsSimulationConsumer<>(filtered, metricObjs);
            // Run the simulation on a worker thread so the Stop button can cancel it. The streaming consumer checks
            // for interruption between iterations, so cancellation halts the run promptly; the partial backing file is
            // discarded and no DiffusionResult is stored, so a cancelled run leaves the session untouched.
            Simulator<String, String, String> sim = simulator;
            try
            {
                jobs.run(MetricController.jobKey(body, "diffusion"), () ->
                {
                    sim.simulate(new CompositeSimulationConsumer<>(streamer, metricConsumer));
                    return null;
                });
            }
            catch (JobCancelledException ce)
            {
                new File(simFile.getAbsolutePath()).delete();
                ctx.json(Map.of("cancelled", true));
                return;
            }

            Map<String, List<Double>> series = metricConsumer.getResults();
            DiffusionResult result = new DiffusionResult(filtered, simFile.getAbsolutePath(),
                    streamer.getNumIterations(), streamer.getSummaries(), series, labels);
            session.setDiffusion(result);

            Map<String, Object> response = result.toJson();
            // Report any metrics that were dropped (typically ran out of memory) so the client can warn the user.
            List<String> skipped = new ArrayList<>();
            for (String id : metricConsumer.getFailed()) skipped.add(labels.getOrDefault(id, id));
            if (!skipped.isEmpty()) response.put("skippedMetrics", skipped);
            ctx.json(response);
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

            // Single streaming pass over iterations 0..iter: accumulate the cumulative sets and, at the requested
            // iteration, capture the this-iteration lists.
            final int target = iter;
            List<String> propIter = new ArrayList<>(), readIter = new ArrayList<>();
            List<String> recvIter = new ArrayList<>(), discIter = new ArrayList<>();
            Set<String> propAll = new LinkedHashSet<>(), readAll = new LinkedHashSet<>();
            Set<String> recvAll = new LinkedHashSet<>(), discAll = new LinkedHashSet<>();
            result.eachIteration(target + 1, (i, it) ->
            {
                it.getPropagatedInformation(node).forEach(propAll::add);
                it.getSeenInformation(node).forEach(t -> { readAll.add(t.v1()); recvAll.add(t.v1()); });
                it.getReReceivedInformation(node).forEach(t -> recvAll.add(t.v1()));
                it.getDiscardedInformation(node).forEach(discAll::add);
                if (i == target)
                {
                    it.getPropagatedInformation(node).forEach(propIter::add);
                    it.getSeenInformation(node).forEach(t -> readIter.add(t.v1()));
                    recvIter.addAll(receivedThisIteration(it, node));
                    it.getDiscardedInformation(node).forEach(discIter::add);
                }
            });

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

    /**
     * Handles {@code POST /api/diffusion/trajectory}: the per-iteration breakdown, by a chosen feature, of the pieces
     * in one category for a single node — the data behind the "Node" timeline subtab. Body:
     * {@code {graphId, node, feature, userFeature, category, mode, aggregation}} where {@code category} is one of
     * {@code received|read|propagated|discarded}, {@code mode} is {@code cumulative|delta} and {@code aggregation}
     * is {@code count|weight}. When {@code userFeature} is true the breakdown is by the piece <em>creators'</em> user
     * feature; otherwise by the info-piece feature.
     */
    public void trajectory(Context ctx)
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

        String node = body.get("node") == null ? null : body.get("node").toString();
        String feature = body.get("feature") == null ? "" : body.get("feature").toString().trim();
        boolean userFeature = Boolean.TRUE.equals(body.get("userFeature"));
        String category = body.get("category") == null ? "received" : body.get("category").toString();
        boolean cumulative = !"delta".equals(body.get("mode"));
        boolean weighted = "weight".equals(body.get("aggregation"));
        // Base "information pieces" mode: instead of breaking one category down by a feature, plot the number of pieces
        // in each category (received / read / propagated / discarded) per iteration for the node.
        boolean basePieces = Boolean.TRUE.equals(body.get("pieces"));

        Data<String, String, String> data = result.data;
        int n = result.numIterations;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("node", node);
        out.put("feature", feature);
        out.put("userFeature", userFeature);
        out.put("category", category);
        out.put("mode", cumulative ? "cumulative" : "delta");
        out.put("aggregation", weighted ? "weight" : "count");
        out.put("base", basePieces);
        out.put("iterations", n);

        boolean nodeOk = node != null && data.getUserIndex().object2idx(node) != -1;
        if (!nodeOk || (!basePieces && feature.isEmpty()))
        {
            out.put("values", new ArrayList<>());
            out.put("series", new LinkedHashMap<>());
            ctx.json(out);
            return;
        }

        if (basePieces)
        {
            List<String> cats = List.of("received", "read", "propagated", "discarded");
            Map<String, Set<String>> cumulativeSets = new LinkedHashMap<>();
            Map<String, List<Double>> series = new LinkedHashMap<>();
            for (String cat : cats) { cumulativeSets.put(cat, new LinkedHashSet<>()); series.put(cat, new ArrayList<>()); }
            result.eachIteration(-1, (i, it) ->
            {
                for (String cat : cats)
                {
                    Set<String> pieces = categoryPieces(it, node, cat);
                    double count;
                    if (cumulative) { cumulativeSets.get(cat).addAll(pieces); count = cumulativeSets.get(cat).size(); }
                    else count = pieces.size();
                    series.get(cat).add(count);
                }
            });
            out.put("values", cats);
            out.put("series", series);
            ctx.json(out);
            return;
        }

        // Walk every iteration once, aggregating the chosen feature over the category's pieces. In cumulative mode the
        // scope is the distinct-piece set accumulated so far (so a re-received piece is not double-counted); in delta
        // mode it is only this iteration's pieces. "peak" tracks each value's largest per-iteration aggregate, used to
        // pick the top-K values to show as bands (the rest are folded into "other").
        List<Map<String, Double>> perIter = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Map<String, Double> peak = new LinkedHashMap<>();
        result.eachIteration(-1, (i, it) ->
        {
            Set<String> pieces = categoryPieces(it, node, category);
            Iterable<String> scope;
            if (cumulative) { seen.addAll(pieces); scope = seen; }
            else scope = pieces;

            Map<String, Double> agg = new LinkedHashMap<>();
            for (String pid : scope)
            {
                for (Map.Entry<String, Double> vw : featureValues(data, pid, feature, userFeature).entrySet())
                {
                    agg.merge(vw.getKey(), weighted ? vw.getValue() : 1.0, Double::sum);
                }
            }
            agg.forEach((key, v) -> peak.merge(key, v, Double::max));
            perIter.add(agg);
        });

        List<String> ranked = new ArrayList<>(peak.keySet());
        ranked.sort((a, b) -> Double.compare(peak.get(b), peak.get(a)));
        int k = 8;
        boolean hasOther = ranked.size() > k;
        List<String> top = new ArrayList<>(ranked.subList(0, Math.min(k, ranked.size())));
        Set<String> topSet = new LinkedHashSet<>(top);

        List<String> values = new ArrayList<>(top);
        if (hasOther) values.add("other");
        Map<String, List<Double>> series = new LinkedHashMap<>();
        for (String v : values) series.put(v, new ArrayList<>());
        for (Map<String, Double> agg : perIter)
        {
            double other = 0.0;
            for (Map.Entry<String, Double> e : agg.entrySet())
            {
                if (topSet.contains(e.getKey())) continue;
                other += e.getValue();
            }
            for (String v : top) series.get(v).add(agg.getOrDefault(v, 0.0));
            if (hasOther) series.get("other").add(other);
        }

        out.put("values", values);
        out.put("series", series);
        ctx.json(out);
    }

    /** The de-duplicated piece ids of one category for a node in a single iteration. */
    private static Set<String> categoryPieces(Iteration<String, String, String> it, String node, String category)
    {
        Set<String> set = new LinkedHashSet<>();
        switch (category)
        {
            case "read":
                it.getSeenInformation(node).forEach(t -> set.add(t.v1()));
                break;
            case "propagated":
                it.getPropagatedInformation(node).forEach(set::add);
                break;
            case "discarded":
                it.getDiscardedInformation(node).forEach(set::add);
                break;
            case "received":
            default:
                it.getSeenInformation(node).forEach(t -> set.add(t.v1()));
                it.getReReceivedInformation(node).forEach(t -> set.add(t.v1()));
                break;
        }
        return set;
    }

    /** The (value -> summed weight) map of a feature for a piece: the info-piece feature, or the creators' user feature. */
    private static Map<String, Double> featureValues(Data<String, String, String> data, String piece,
                                                     String feature, boolean userFeature)
    {
        Map<String, Double> m = new LinkedHashMap<>();
        if (userFeature)
        {
            data.getCreators(piece).forEach(creator ->
                data.getUserFeatures(creator, feature).forEach(p -> m.merge(p.v1, p.v2, Double::sum)));
        }
        else
        {
            data.getInfoPiecesFeatures(piece, feature).forEach(p -> m.merge(p.v1, p.v2, Double::sum));
        }
        return m;
    }

    /**
     * Handles {@code POST /api/diffusion/piece-trajectory}: the dual of {@link #trajectory} keyed on a single
     * information piece instead of a user. For a chosen piece it plots, per iteration, either the number of users in
     * each category (received / read / propagated / discarded — the base view, {@code feature} empty) or, for one
     * category, the breakdown of those users by a user feature (node attribute or community). Body:
     * {@code {graphId, piece, feature, category, mode}}.
     */
    public void pieceTrajectory(Context ctx)
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

        String piece = body.get("piece") == null ? null : body.get("piece").toString();
        String feature = body.get("feature") == null ? "" : body.get("feature").toString().trim();
        String category = body.get("category") == null ? "received" : body.get("category").toString();
        boolean cumulative = !"delta".equals(body.get("mode"));
        boolean base = feature.isEmpty();

        Data<String, String, String> data = result.data;
        int n = result.numIterations;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("piece", piece);
        out.put("feature", feature);
        out.put("category", category);
        out.put("mode", cumulative ? "cumulative" : "delta");
        out.put("base", base);
        out.put("iterations", n);

        boolean pieceOk = piece != null && data.getInformationPiecesIndex().object2idx(piece) != -1;
        if (!pieceOk)
        {
            out.put("values", new ArrayList<>());
            out.put("series", new LinkedHashMap<>());
            ctx.json(out);
            return;
        }

        if (base)
        {
            List<String> cats = List.of("received", "read", "propagated", "discarded");
            Map<String, Set<String>> cumulativeSets = new LinkedHashMap<>();
            Map<String, List<Double>> series = new LinkedHashMap<>();
            for (String cat : cats) { cumulativeSets.put(cat, new LinkedHashSet<>()); series.put(cat, new ArrayList<>()); }
            result.eachIteration(-1, (i, it) ->
            {
                for (String cat : cats)
                {
                    Set<String> users = categoryUsersForPiece(it, piece, cat);
                    double count;
                    if (cumulative) { cumulativeSets.get(cat).addAll(users); count = cumulativeSets.get(cat).size(); }
                    else count = users.size();
                    series.get(cat).add(count);
                }
            });
            out.put("values", cats);
            out.put("series", series);
            ctx.json(out);
            return;
        }

        // Break the chosen category's users down by their user feature (count of users per value).
        List<Map<String, Double>> perIter = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Map<String, Double> peak = new LinkedHashMap<>();
        result.eachIteration(-1, (i, it) ->
        {
            Set<String> users = categoryUsersForPiece(it, piece, category);
            Iterable<String> scope;
            if (cumulative) { seen.addAll(users); scope = seen; }
            else scope = users;

            Map<String, Double> agg = new LinkedHashMap<>();
            for (String u : scope)
            {
                data.getUserFeatures(u, feature).forEach(p -> agg.merge(p.v1, 1.0, Double::sum));
            }
            agg.forEach((key, v) -> peak.merge(key, v, Double::max));
            perIter.add(agg);
        });

        List<String> ranked = new ArrayList<>(peak.keySet());
        ranked.sort((a, b) -> Double.compare(peak.get(b), peak.get(a)));
        int k = 8;
        boolean hasOther = ranked.size() > k;
        List<String> top = new ArrayList<>(ranked.subList(0, Math.min(k, ranked.size())));
        Set<String> topSet = new LinkedHashSet<>(top);

        List<String> values = new ArrayList<>(top);
        if (hasOther) values.add("other");
        Map<String, List<Double>> series = new LinkedHashMap<>();
        for (String v : values) series.put(v, new ArrayList<>());
        for (Map<String, Double> agg : perIter)
        {
            double other = 0.0;
            for (Map.Entry<String, Double> e : agg.entrySet())
            {
                if (topSet.contains(e.getKey())) continue;
                other += e.getValue();
            }
            for (String v : top) series.get(v).add(agg.getOrDefault(v, 0.0));
            if (hasOther) series.get("other").add(other);
        }

        out.put("values", values);
        out.put("series", series);
        ctx.json(out);
    }

    /** The de-duplicated users that received / read / propagated / discarded a specific piece in a single iteration. */
    private static Set<String> categoryUsersForPiece(Iteration<String, String, String> it, String piece, String category)
    {
        Set<String> users = new LinkedHashSet<>();
        switch (category)
        {
            case "propagated":
                it.getPropagatingUsers().forEach(u -> { if (it.getPropagatedInformation(u).anyMatch(p -> p.equals(piece))) users.add(u); });
                break;
            case "discarded":
                it.getDiscardingUsers().forEach(u -> { if (it.getDiscardedInformation(u).anyMatch(p -> p.equals(piece))) users.add(u); });
                break;
            case "read":
                it.getReceivingUsers().forEach(u -> { if (it.getSeenInformation(u).anyMatch(t -> t.v1().equals(piece))) users.add(u); });
                break;
            case "received":
            default:
                it.getReceivingUsers().forEach(u -> { if (it.getSeenInformation(u).anyMatch(t -> t.v1().equals(piece))) users.add(u); });
                it.getReReceivingUsers().forEach(u -> { if (it.getReReceivedInformation(u).anyMatch(t -> t.v1().equals(piece))) users.add(u); });
                break;
        }
        return users;
    }

    /**
     * Handles {@code POST /api/diffusion/feature-trajectory}: the network-wide spread of a feature's values over the
     * iterations. For a chosen feature it plots, per iteration, the count (or summed weight) of pieces carrying each
     * value across every user in the chosen category — the global counterpart of the per-node feature breakdown. Body:
     * {@code {graphId, feature, userFeature, category, mode, aggregation}}.
     */
    public void featureTrajectory(Context ctx)
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

        String feature = body.get("feature") == null ? "" : body.get("feature").toString().trim();
        boolean userFeature = Boolean.TRUE.equals(body.get("userFeature"));
        String category = body.get("category") == null ? "received" : body.get("category").toString();
        boolean cumulative = !"delta".equals(body.get("mode"));
        boolean byUsers = "users".equals(body.get("entity"));   // count distinct users vs distinct information pieces
        // A specific feature value drills down to that value only, plotting the four categories over time; empty (or
        // the "__all__" sentinel) plots every value of the feature for the chosen category.
        String value = body.get("value") == null ? "" : body.get("value").toString();
        boolean specificValue = !value.isEmpty() && !"__all__".equals(value);

        Data<String, String, String> data = result.data;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("feature", feature);
        out.put("userFeature", userFeature);
        out.put("category", category);
        out.put("mode", cumulative ? "cumulative" : "delta");
        out.put("entity", byUsers ? "users" : "pieces");
        out.put("value", specificValue ? value : "");
        out.put("iterations", result.numIterations);

        if (feature.isEmpty())
        {
            out.put("values", new ArrayList<>());
            out.put("series", new LinkedHashMap<>());
            ctx.json(out);
            return;
        }

        if (specificValue)
        {
            // Filter to the chosen value and count, per iteration, the distinct entities (pieces or users) carrying it
            // in each of the four categories.
            List<String> cats = List.of("received", "read", "propagated", "discarded");
            Map<String, Set<String>> cumulativeSets = new LinkedHashMap<>();
            Map<String, List<Double>> series = new LinkedHashMap<>();
            for (String cat : cats) { cumulativeSets.put(cat, new LinkedHashSet<>()); series.put(cat, new ArrayList<>()); }
            result.eachIteration(-1, (i, it) ->
            {
                for (String cat : cats)
                {
                    Set<String> ents = new LinkedHashSet<>();
                    if (byUsers)
                    {
                        for (String u : categoryUsersGlobal(it, cat))
                        {
                            if (userFeature)
                            {
                                if (userHasFeatureValue(data, u, feature, value)) ents.add(u);
                            }
                            else
                            {
                                for (String piece : categoryPieces(it, u, cat))
                                {
                                    if (featureValues(data, piece, feature, false).containsKey(value)) { ents.add(u); break; }
                                }
                            }
                        }
                    }
                    else
                    {
                        for (String pid : categoryPiecesGlobal(it, cat))
                        {
                            if (featureValues(data, pid, feature, userFeature).containsKey(value)) ents.add(pid);
                        }
                    }
                    double count;
                    if (cumulative) { cumulativeSets.get(cat).addAll(ents); count = cumulativeSets.get(cat).size(); }
                    else count = ents.size();
                    series.get(cat).add(count);
                }
            });
            out.put("values", cats);
            out.put("series", series);
            ctx.json(out);
            return;
        }

        // For each iteration collect, per feature value, the set of contributing entities — distinct information
        // pieces or distinct users, per the request. In cumulative mode the sets persist across iterations (so a
        // value's count is the distinct entities associated with it so far); in delta mode they are per-iteration.
        List<Map<String, Double>> perIter = new ArrayList<>();
        Map<String, Set<String>> cumulativeSets = new LinkedHashMap<>();
        Map<String, Double> peak = new LinkedHashMap<>();
        result.eachIteration(-1, (i, it) ->
        {
            Map<String, Set<String>> iterSets = new LinkedHashMap<>();
            if (byUsers)
            {
                // A user counts towards a value if they carry it (user feature) or received a piece with it (info feature).
                for (String u : categoryUsersGlobal(it, category))
                {
                    if (userFeature)
                    {
                        data.getUserFeatures(u, feature).forEach(p ->
                                iterSets.computeIfAbsent(p.v1, k -> new LinkedHashSet<>()).add(u));
                    }
                    else
                    {
                        for (String piece : categoryPieces(it, u, category))
                        {
                            data.getInfoPiecesFeatures(piece, feature).forEach(p ->
                                    iterSets.computeIfAbsent(p.v1, k -> new LinkedHashSet<>()).add(u));
                        }
                    }
                }
            }
            else
            {
                // A piece counts towards each of its feature values (its own, or its creators' for a user feature).
                for (String pid : categoryPiecesGlobal(it, category))
                {
                    for (String fv : featureValues(data, pid, feature, userFeature).keySet())
                    {
                        iterSets.computeIfAbsent(fv, k -> new LinkedHashSet<>()).add(pid);
                    }
                }
            }

            Map<String, Double> agg = new LinkedHashMap<>();
            if (cumulative)
            {
                iterSets.forEach((v, ents) -> cumulativeSets.computeIfAbsent(v, k -> new LinkedHashSet<>()).addAll(ents));
                cumulativeSets.forEach((v, ents) -> agg.put(v, (double) ents.size()));
            }
            else
            {
                iterSets.forEach((v, ents) -> agg.put(v, (double) ents.size()));
            }
            agg.forEach((key, v) -> peak.merge(key, v, Double::max));
            perIter.add(agg);
        });

        List<String> ranked = new ArrayList<>(peak.keySet());
        ranked.sort((a, b) -> Double.compare(peak.get(b), peak.get(a)));
        int k = 8;
        boolean hasOther = ranked.size() > k;
        List<String> top = new ArrayList<>(ranked.subList(0, Math.min(k, ranked.size())));
        Set<String> topSet = new LinkedHashSet<>(top);

        List<String> values = new ArrayList<>(top);
        if (hasOther) values.add("other");
        Map<String, List<Double>> series = new LinkedHashMap<>();
        for (String v : values) series.put(v, new ArrayList<>());
        for (Map<String, Double> agg : perIter)
        {
            double other = 0.0;
            for (Map.Entry<String, Double> e : agg.entrySet())
            {
                if (topSet.contains(e.getKey())) continue;
                other += e.getValue();
            }
            for (String v : top) series.get(v).add(agg.getOrDefault(v, 0.0));
            if (hasOther) series.get("other").add(other);
        }

        out.put("values", values);
        out.put("series", series);
        ctx.json(out);
    }

    /** The de-duplicated piece ids of one category across every user in a single iteration. */
    private static Set<String> categoryPiecesGlobal(Iteration<String, String, String> it, String category)
    {
        Set<String> set = new LinkedHashSet<>();
        switch (category)
        {
            case "read":
                it.getReceivingUsers().forEach(u -> it.getSeenInformation(u).forEach(t -> set.add(t.v1())));
                break;
            case "propagated":
                it.getPropagatingUsers().forEach(u -> it.getPropagatedInformation(u).forEach(set::add));
                break;
            case "discarded":
                it.getDiscardingUsers().forEach(u -> it.getDiscardedInformation(u).forEach(set::add));
                break;
            case "received":
            default:
                it.getReceivingUsers().forEach(u -> it.getSeenInformation(u).forEach(t -> set.add(t.v1())));
                it.getReReceivingUsers().forEach(u -> it.getReReceivedInformation(u).forEach(t -> set.add(t.v1())));
                break;
        }
        return set;
    }

    /** The de-duplicated users active in one category (received / read / propagated / discarded) in a single iteration. */
    private static Set<String> categoryUsersGlobal(Iteration<String, String, String> it, String category)
    {
        Set<String> set = new LinkedHashSet<>();
        switch (category)
        {
            case "read":
                it.getReceivingUsers().forEach(set::add);
                break;
            case "propagated":
                it.getPropagatingUsers().forEach(set::add);
                break;
            case "discarded":
                it.getDiscardingUsers().forEach(set::add);
                break;
            case "received":
            default:
                it.getReceivingUsers().forEach(set::add);
                it.getReReceivingUsers().forEach(set::add);
                break;
        }
        return set;
    }

    /** Whether a user has a given value of a user feature. */
    private static boolean userHasFeatureValue(Data<String, String, String> data, String user, String feature, String value)
    {
        return data.getUserFeatures(user, feature).anyMatch(p -> value.equals(p.v1));
    }

    /**
     * Handles {@code POST /api/diffusion/distribution}: computes one of RELISON's diffusion distributions over the
     * whole simulation and returns it for plotting. Body {@code {graphId, type, feature | infoFeature, userFeature}}
     * where {@code type} is {@code info} (info-piece feature distribution, {@link InformationFeatureDistribution}),
     * {@code user} (user feature distribution, {@link UserFeatureDistribution}) or {@code mixed}
     * ({@link MixedFeatureDistribution}, info-feature × user-feature cross-tab).
     *
     * <p>The distribution objects accumulate over iterations and only expose their result through
     * {@code print(file)}, so we feed every iteration, print to a temporary file and read it back.</p>
     */
    public void distribution(Context ctx)
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

        String type = body.get("type") == null ? "info" : body.get("type").toString();
        String feature = body.get("feature") == null ? "" : body.get("feature").toString().trim();
        String infoFeature = body.get("infoFeature") == null ? "" : body.get("infoFeature").toString().trim();
        String userFeature = body.get("userFeature") == null ? "" : body.get("userFeature").toString().trim();

        Distribution<String, String, String> dist;
        switch (type)
        {
            case "user": dist = new UserFeatureDistribution<>(feature); break;
            case "mixed": dist = new MixedFeatureDistribution<>(infoFeature, userFeature); break;
            case "info":
            default: dist = new InformationFeatureDistribution<>(feature); break;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", type);

        dist.initialize(result.data);
        if (!dist.isInitialized())
        {
            // The distribution refused the feature (wrong kind — e.g. a user feature for an info distribution).
            out.put("error", "The selected feature is not of the kind this distribution requires.");
            out.put("values", new ArrayList<>());
            ctx.json(out);
            return;
        }

        result.eachIteration(-1, (i, it) -> dist.update(it));

        try
        {
            File tmp = File.createTempFile("relison-dist", ".tsv");
            dist.print(tmp.getAbsolutePath());
            List<String> lines = Files.readAllLines(tmp.toPath());
            tmp.delete();

            if ("mixed".equals(type)) buildMixedDistribution(out, lines, infoFeature, userFeature);
            else buildSingleDistribution(out, lines, feature);
            ctx.json(out);
        }
        catch (IOException e)
        {
            ctx.status(500).json(Map.of("error", "Could not compute the distribution: " + e.getMessage()));
        }
    }

    /** Parses a {@code value \t count} distribution file into a list of {@code {value, count}}, sorted by count. */
    private static void buildSingleDistribution(Map<String, Object> out, List<String> lines, String feature)
    {
        out.put("feature", feature);
        List<Map<String, Object>> values = new ArrayList<>();
        for (String line : lines)
        {
            String[] p = line.split("\t");
            if (p.length < 2) continue;
            double count = parseDoubleOr0(p[1]);
            if (count <= 0.0) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", p[0]);
            m.put("count", count);
            values.add(m);
        }
        values.sort((a, b) -> Double.compare((double) b.get("count"), (double) a.get("count")));
        out.put("values", values);
    }

    /** Parses a {@code v1 \t v2 \t count} file into a (top-12 × top-12) info-value × user-value count matrix. */
    private static void buildMixedDistribution(Map<String, Object> out, List<String> lines, String infoFeature, String userFeature)
    {
        Map<String, Map<String, Double>> cells = new LinkedHashMap<>();
        Map<String, Double> infoMarginal = new LinkedHashMap<>();
        Map<String, Double> userMarginal = new LinkedHashMap<>();
        for (String line : lines)
        {
            String[] p = line.split("\t");
            if (p.length < 3) continue;
            double count = parseDoubleOr0(p[2]);
            if (count <= 0.0) continue;
            String iv = p[0], uv = p[1];
            cells.computeIfAbsent(iv, k -> new LinkedHashMap<>()).merge(uv, count, Double::sum);
            infoMarginal.merge(iv, count, Double::sum);
            userMarginal.merge(uv, count, Double::sum);
        }
        List<String> infoValues = topKeys(infoMarginal, 12);
        List<String> userValues = topKeys(userMarginal, 12);
        List<List<Double>> matrix = new ArrayList<>();
        for (String iv : infoValues)
        {
            List<Double> row = new ArrayList<>();
            Map<String, Double> r = cells.getOrDefault(iv, Map.of());
            for (String uv : userValues) row.add(r.getOrDefault(uv, 0.0));
            matrix.add(row);
        }
        out.put("infoFeature", infoFeature);
        out.put("userFeature", userFeature);
        out.put("infoValues", infoValues);
        out.put("userValues", userValues);
        out.put("matrix", matrix);
    }

    /** The keys of a marginal map with the largest values, at most {@code k}, largest first. */
    private static List<String> topKeys(Map<String, Double> marginal, int k)
    {
        List<String> keys = new ArrayList<>(marginal.keySet());
        keys.sort((a, b) -> Double.compare(marginal.get(b), marginal.get(a)));
        return new ArrayList<>(keys.subList(0, Math.min(k, keys.size())));
    }

    private static double parseDoubleOr0(String s)
    {
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return 0.0; }
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

    /** Handles {@code POST /api/diffusion/real-propagated}: persists the real-propagation records on the session. */
    public void saveRealPropagated(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;
        Object obj = body.get("realPropagated");
        List<?> records = (obj instanceof List) ? (List<?>) obj : List.of();
        session.setRealPropagated(records);
        ctx.json(Map.of("count", records.size()));
    }

    /** Handles {@code POST /api/diffusion/real-propagated/get}: returns the real-propagation records on the session. */
    public void getRealPropagated(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;
        ctx.json(Map.of("realPropagated", session.getRealPropagated()));
    }

    /**
     * Handles {@code POST /api/diffusion/seed}: generates synthetic information pieces with the RELISON piece seeders
     * and returns them as the {@code {id, creator, timestamp}} list the pieces table consumes (the frontend then
     * displays/persists them). Body: {@code {graphId, count:{type,…}, users:{type,…}, timestamp:{type,…}, seed?}}.
     */
    public void seed(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        Graph<String> graph = session.getGraph();
        Long seed = longOrNull(body.get("seed"));
        Random rng = (seed == null) ? new Random() : new Random(seed);

        TimestampGenerator timestamps = buildTimestampGenerator(asMap(body.get("timestamp")), seed);
        UserSelector<String> selector = buildUserSelector(asMap(body.get("users")), session, seed);
        Generator<String> ids = newPieceIdGenerator();

        Map<?, ?> count = asMap(body.get("count"));
        String countType = (count == null) ? "fixed" : String.valueOf(count.get("type"));
        PieceSeeder<String, String, String> seeder;
        switch (countType)
        {
            case "uniform":
                seeder = new UniformRandomPieceSeeder<>(intValue(count.get("min"), 1), intValue(count.get("max"), 1),
                        ids, timestamps, rng, selector);
                break;
            case "poisson":
                seeder = new PoissonPieceSeeder<>(doubleValue(count.get("lambda"), 1.0), ids, timestamps, rng, selector);
                break;
            case "fixed":
            default:
                seeder = new FixedNumberPieceSeeder<>(intValue(count == null ? null : count.get("n"), 1), ids, timestamps, selector);
                break;
        }

        Data<String, String, String> data = seeder.seed(graph);

        List<Map<String, Object>> pieces = new ArrayList<>();
        data.getAllInformationPieces().forEach(piece ->
        {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", piece);
            p.put("creator", data.getCreators(piece).findFirst().orElse(""));
            p.put("timestamp", data.getTimestamp(piece));
            p.put("features", List.of());
            pieces.add(p);
        });

        ctx.json(Map.of("pieces", pieces, "count", pieces.size()));
    }

    /** Builds the timestamp generator from the request's {@code timestamp} config. */
    private TimestampGenerator buildTimestampGenerator(Map<?, ?> ts, Long seed)
    {
        if (ts != null && "uniform".equals(String.valueOf(ts.get("type"))))
        {
            long min = longValue(ts.get("min"), 0L);
            long max = longValue(ts.get("max"), 0L);
            return (seed == null) ? new UniformTimestampGenerator(min, max) : new UniformTimestampGenerator(min, max, seed);
        }
        return new FixedTimestampGenerator(ts == null ? 0L : longValue(ts.get("value"), 0L));
    }

    /** Builds the user selector from the request's {@code users} config (all users when unspecified/unknown). */
    private UserSelector<String> buildUserSelector(Map<?, ?> u, GraphSession session, Long seed)
    {
        if (u == null) return new AllUsersSelector<>();
        switch (String.valueOf(u.get("type")))
        {
            case "random":
            {
                double fraction = doubleValue(u.get("fraction"), 1.0);
                return (seed == null) ? new RandomUserSelector<>(fraction) : new RandomUserSelector<>(fraction, seed);
            }
            case "community":
            {
                Communities<String> comms = session.getCommunities().get(String.valueOf(u.get("partition")));
                if (comms == null) return new AllUsersSelector<>();
                return new CommunityUserSelector<>(comms, intValue(u.get("community"), 0));
            }
            case "values":
            {
                Map<String, Double> values = new HashMap<>();
                if (u.get("values") instanceof Map)
                {
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) u.get("values")).entrySet())
                    {
                        Double d = toDouble(e.getValue());
                        if (d != null) values.put(String.valueOf(e.getKey()), d);
                    }
                }
                ValuesUserSelector.Mode mode = "bottom".equalsIgnoreCase(String.valueOf(u.get("mode")))
                        ? ValuesUserSelector.Mode.BOTTOM : ValuesUserSelector.Mode.TOP;
                return new ValuesUserSelector<>(values, intValue(u.get("k"), 0), mode);
            }
            default:
                return new AllUsersSelector<>();
        }
    }

    /** A fresh generator of piece identifiers ("p0", "p1", …); per-request so concurrent seeds do not interfere. */
    private static Generator<String> newPieceIdGenerator()
    {
        return new Generator<>()
        {
            private int prev = -1;

            @Override
            public String generate()
            {
                return "p" + (++prev);
            }

            @Override
            public void reset()
            {
                prev = -1;
            }

            @Override
            public void reset(String v)
            {
                try { prev = Integer.parseInt(v.startsWith("p") ? v.substring(1) : v); }
                catch (NumberFormatException e) { prev = -1; }
            }
        };
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
            if (def == null || def.needsRealProp) continue;   // real-propagation metrics need ground-truth data we don't have
            try
            {
                Parameters mp = metricParams(def, params);
                Tuple2<String, SimulationMetric<String, String, String>> metric = selector.select(id, mp);
                if (metric == null || metric.v2() == null) continue;
                // A feature metric can be requested once per feature parameter, so key it (and label it) by that
                // parameter to avoid collisions between instances of the same metric id.
                String key = id, label = def.label;
                Object feature = params == null ? null : params.get("feature");
                if (feature != null && !String.valueOf(feature).isEmpty())
                {
                    boolean userFeat = Boolean.parseBoolean(String.valueOf(params.get("userFeature")));
                    key = id + "|" + (userFeat ? "u" : "i") + "|" + feature;
                    label = def.label + " — " + feature + (userFeat ? " (user)" : "");
                }
                metricObjs.put(key, metric.v2());
                labels.put(key, label);
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

    private static double doubleValue(Object value, double def)
    {
        Double d = toDouble(value);
        return d == null ? def : d;
    }

    private static Double toDouble(Object value)
    {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value == null) return null;
        try { return Double.parseDouble(value.toString().trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private static long longValue(Object value, long def)
    {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value == null) return def;
        try { return Long.parseLong(value.toString().trim()); }
        catch (NumberFormatException e)
        {
            Double d = toDouble(value);
            return d == null ? def : d.longValue();
        }
    }

    private static Long longOrNull(Object value)
    {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value == null) return null;
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return null; }
    }

    private static Map<?, ?> asMap(Object value)
    {
        return (value instanceof Map) ? (Map<?, ?>) value : null;
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
