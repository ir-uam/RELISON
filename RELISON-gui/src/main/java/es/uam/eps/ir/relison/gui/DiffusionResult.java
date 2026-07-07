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
import es.uam.eps.ir.relison.diffusion.simulation.Simulation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The outcome of a diffusion simulation kept on the session: the {@link Simulation} and its {@link Data} (for the
 * per-node, per-iteration state queries), compact per-iteration node summaries (for the time-scrubbing canvas) and
 * the per-iteration value series of the requested metrics (for the metrics-plots tab).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class DiffusionResult
{
    /** Per-iteration node summary: who propagated and who was newly informed this iteration. */
    public static final class IterationSummary
    {
        public final List<String> propagating;
        public final List<String> newlyInformed;

        public IterationSummary(List<String> propagating, List<String> newlyInformed)
        {
            this.propagating = propagating;
            this.newlyInformed = newlyInformed;
        }
    }

    /** The diffusion data the simulation ran over. */
    public final Data<String, String, String> data;
    /** The simulation itself (used by the per-node state endpoint). */
    public final Simulation<String, String, String> simulation;
    /** Number of iterations. */
    public final int numIterations;
    /** One summary per iteration. */
    public final List<IterationSummary> iterations;
    /** Per-iteration value series per computed metric (id -> values). */
    public final Map<String, List<Double>> series;
    /** Display labels for the metric series (id -> label). */
    public final Map<String, String> seriesLabels;

    public DiffusionResult(Data<String, String, String> data, Simulation<String, String, String> simulation,
                           List<IterationSummary> iterations, Map<String, List<Double>> series, Map<String, String> seriesLabels)
    {
        this.data = data;
        this.simulation = simulation;
        this.numIterations = simulation.getNumIterations();
        this.iterations = iterations;
        this.series = series;
        this.seriesLabels = seriesLabels;
    }

    /** Builds the JSON payload returned to the client (without the per-node detail, which is fetched lazily). */
    public Map<String, Object> toJson()
    {
        List<Map<String, Object>> iters = new java.util.ArrayList<>();
        for (IterationSummary s : iterations)
        {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("propagating", s.propagating);
            m.put("newlyInformed", s.newlyInformed);
            iters.add(m);
        }

        List<Map<String, Object>> metrics = new java.util.ArrayList<>();
        for (String id : series.keySet())
        {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("label", seriesLabels.getOrDefault(id, id));
            m.put("values", series.get(id));
            metrics.add(m);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("numIterations", numIterations);
        out.put("iterations", iters);
        out.put("metrics", metrics);
        return out;
    }
}
