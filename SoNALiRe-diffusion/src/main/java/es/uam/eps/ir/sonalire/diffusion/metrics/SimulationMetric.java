/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.metrics;

import es.uam.eps.ir.sonalire.diffusion.data.Data;
import es.uam.eps.ir.sonalire.diffusion.simulation.Iteration;
import es.uam.eps.ir.sonalire.diffusion.simulation.Simulation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface for the different metrics to apply over the simulation.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <F> Type of the features.
 */
public interface SimulationMetric<U extends Serializable,I extends Serializable, F>
{    
    /**
     * Obtains the name of the metric.
     * @return the name of the metric.
     */
    String getName();

    /**
     * Indicates if the metric has been initialized or not.
     * @return true if it has been initialized, false if it has not.
     */
    boolean isInitialized();

    /**
     * Calculates the metric for the current state of the simulation.
     * @return the value of the metric for the current state of the simulation
     */
    double calculate();

    /**
     * Calculates the metric for each iteration of a simulation.
     * @param data the data.
     * @param simulation the whole simulation.
     * @return the value of the metric for each iteration.
     */
    default List<Double> calculate(Data<U, I, F> data, Simulation<U, I, F> simulation)
    {
        if(simulation == null || data == null)
            return new ArrayList<>();

        this.clear();
        this.initialize(data);
        int numIter = simulation.getNumIterations();
        List<Double> values = new ArrayList<>();
        for(int i = 0; i < numIter; ++i)
        {
            this.update(simulation.getIteration(i));
            values.add(this.calculate());
        }
        return values;
    }

    /**
     * Updates the different values which are necessary for computing a metric, given
     * the information received by users in an iteration of the simulation.
     * @param iteration the new iteration.
     */
    void update(Iteration<U, I, F> iteration);

    /**
     * Resets the metric.
     */
    void clear();

    /**
     * Initializes the metric.
     * @param data the data.
     */
    void initialize(Data<U, I, F> data);

    /**
     * Initializes and establishes a given state to the metric from a simulation backup.
     * @param data the data.
     * @param backupSim the simulation backup.
     */
    default void initialize(Data<U, I, F> data, Simulation<U, I, F> backupSim)
    {
        this.initialize(data);
        for(int i = 0; i < backupSim.getNumIterations(); ++i)
        {
            this.update(backupSim.getIteration(i));
        }
    }
    
}
