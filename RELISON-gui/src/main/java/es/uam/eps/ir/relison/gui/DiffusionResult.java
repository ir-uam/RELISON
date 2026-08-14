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
import es.uam.eps.ir.relison.diffusion.io.backup.BinarySimulationReader;
import es.uam.eps.ir.relison.diffusion.simulation.Iteration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The outcome of a diffusion simulation kept on the session. To keep memory bounded, the simulation itself is
 * <em>streamed to a binary file</em> as it runs (see {@link StreamingSimulationConsumer}) rather than retained: only
 * its {@link Data}, compact per-iteration node summaries (for the time-scrubbing canvas) and the per-iteration metric
 * series (for the metrics-plots tab) are held in memory. The per-node / per-piece / distribution endpoints read the
 * iterations back from the file on demand via {@link #eachIteration}.
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

    /** Visits one iteration read back from the streamed file, together with its (0-based) index. */
    public interface IterationConsumer
    {
        void accept(int index, Iteration<String, String, String> iteration);
    }

    /** The diffusion data the simulation ran over. */
    public final Data<String, String, String> data;
    /** Path to the binary file the iterations were streamed to (read back on demand). */
    public final String simFile;
    /** Number of iterations. */
    public final int numIterations;
    /** One summary per iteration. */
    public final List<IterationSummary> iterations;
    /** Per-iteration value series per computed metric (id -> values). */
    public final Map<String, List<Double>> series;
    /** Display labels for the metric series (id -> label). */
    public final Map<String, String> seriesLabels;

    public DiffusionResult(Data<String, String, String> data, String simFile, int numIterations,
                           List<IterationSummary> iterations, Map<String, List<Double>> series, Map<String, String> seriesLabels)
    {
        this.data = data;
        this.simFile = simFile;
        this.numIterations = numIterations;
        this.iterations = iterations;
        this.series = series;
        this.seriesLabels = seriesLabels;
    }

    /**
     * Streams the iterations back from the backing file in order, invoking {@code consumer} for each. At most
     * {@code maxExclusive} iterations are read (a negative value reads all of them). The reader is always closed.
     * @param maxExclusive the number of iterations to read (from the first), or a negative value for all.
     * @param consumer     the per-iteration callback.
     */
    public void eachIteration(int maxExclusive, IterationConsumer consumer)
    {
        BinarySimulationReader<String, String, String> reader = new BinarySimulationReader<>();
        if (!reader.initialize(simFile)) return;
        try
        {
            int limit = (maxExclusive < 0 || maxExclusive > numIterations) ? numIterations : maxExclusive;
            for (int i = 0; i < limit; ++i)
            {
                Iteration<String, String, String> it = reader.readIteration(data);
                if (it == null) break;
                consumer.accept(i, it);
            }
        }
        finally
        {
            reader.close();
        }
    }

    /** Deletes the backing file; called when this result is discarded (replaced, cleared or the graph edited). */
    public void deleteBackingFile()
    {
        if (simFile != null)
        {
            try { new File(simFile).delete(); }
            catch (Exception ignored) { /* best-effort cleanup */ }
        }
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
