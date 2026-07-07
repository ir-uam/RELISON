/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.metrics;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.simulation.Iteration;
import es.uam.eps.ir.relison.diffusion.simulation.SimulationConsumer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link SimulationConsumer} that folds each iteration into a set of {@link SimulationMetric metrics} as the
 * simulation runs, producing one value per iteration for each metric without ever materialising the whole
 * {@link es.uam.eps.ir.relison.diffusion.simulation.Simulation}. This is the streaming equivalent of calling
 * {@link SimulationMetric#calculate(Data, es.uam.eps.ir.relison.diffusion.simulation.Simulation)} on every metric,
 * but it traverses the iterations a single time and keeps only the running accumulators plus the resulting series.
 *
 * <p>A metric that throws while updating (e.g. because its data is missing) is dropped, along with its partial
 * series, so one faulty metric does not abort the whole computation.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the features.
 */
public class MetricsSimulationConsumer<U extends Serializable, I extends Serializable, F> implements SimulationConsumer<U, I, F>
{
    /** The data of the simulation (used to initialize the metrics). */
    private final Data<U, I, F> data;
    /** The metrics to compute, keyed by an identifier chosen by the caller. */
    private final Map<String, SimulationMetric<U, I, F>> metrics;
    /** The resulting per-iteration series, keyed by the same identifier. */
    private final Map<String, List<Double>> results;

    /**
     * Constructor.
     * @param data    the simulation data.
     * @param metrics the metrics to compute, keyed by an identifier chosen by the caller (iteration order preserved).
     */
    public MetricsSimulationConsumer(Data<U, I, F> data, Map<String, SimulationMetric<U, I, F>> metrics)
    {
        this.data = data;
        this.metrics = new LinkedHashMap<>(metrics);
        this.results = new LinkedHashMap<>();
    }

    @Override
    public void start(int initialIteration)
    {
        List<String> failed = new ArrayList<>();
        this.metrics.forEach((id, metric) ->
        {
            try
            {
                metric.clear();
                metric.initialize(this.data);
                this.results.put(id, new ArrayList<>());
            }
            catch (Exception e)
            {
                failed.add(id);
            }
        });
        failed.forEach(this.metrics::remove);
    }

    @Override
    public void accept(Iteration<U, I, F> iteration)
    {
        List<String> failed = new ArrayList<>();
        this.metrics.forEach((id, metric) ->
        {
            try
            {
                metric.update(iteration);
                this.results.get(id).add(metric.calculate());
            }
            catch (Exception e)
            {
                failed.add(id);
            }
        });
        // Drop any metric that failed this iteration, together with its partial series.
        failed.forEach(id ->
        {
            this.metrics.remove(id);
            this.results.remove(id);
        });
    }

    /**
     * Obtains the per-iteration series computed for each metric that ran successfully.
     * @return a map from metric identifier to the list of per-iteration values.
     */
    public Map<String, List<Double>> getResults()
    {
        return this.results;
    }
}
