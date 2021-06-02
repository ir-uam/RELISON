/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.metrics;

import es.uam.eps.ir.sonalire.diffusion.simulation.Simulation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interface for the different individual metrics (can be applied over individual
 * users) to apply over the simulation.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the features.
 */
public interface IndividualSimulationMetric<U extends Serializable,I extends Serializable, F> extends SimulationMetric<U,I, F>
{
    /**
     * Calculates the metric for each individual in the network.
     * @return a map containing the values of the metric for each user.
     */
    Map<U, Double> calculateIndividuals();
    
    /**
     * Calculates the metric for each individual user on each iteration of a simulation.
     * @param simulation the whole simulation.
     * @return the value of the metric for each iteration.
     */
    default List<Map<U,Double>> calculateIndividuals(Simulation<U, I, F> simulation)
    {
        if(simulation == null || !this.isInitialized())
            return new ArrayList<>();

        this.clear();
        int numIter = simulation.getNumIterations();
        List<Map<U,Double>> values = new ArrayList<>();
        for(int i = 0; i < numIter; ++i)
        {
            this.update(simulation.getIteration(i));
            values.add(this.calculateIndividuals());
        }
        return values;
    }

    /**
     * Calculates the metric value for a single user.
     * @param user the single user.
     * @return the value of the metric, NaN if something failed.
     */
    double calculate(U user);

    /**
     * Calculates the metric for a single user on each iteration of a simulation.
     * @param user the user.
     * @param simulation the whole simulation
     * @return the value of the metric for that user in each iteration.
     */
    default List<Double> calculate(U user, Simulation<U, I, F> simulation)
    {
        if(simulation == null || user == null  || !this.isInitialized())
            return new ArrayList<>();
        
        this.clear();
        int numIter = simulation.getNumIterations();
        List<Double> values = new ArrayList<>();
        for(int i = 0; i < numIter; ++i)
        {
            this.update(simulation.getIteration(i));
            values.add(this.calculate(user));
        }
        return values;
    }
}
