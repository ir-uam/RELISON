/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.metrics;

import es.uam.eps.socialranksys.diffusion.simulation.Simulation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interface for the different individual metrics (can be applied over individual
 * users) to apply over the simulation.
 * @author Javier Sanz-Cruzado Puig
 * 
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public interface IndividualSimulationMetric<U extends Serializable,I extends Serializable,P> extends SimulationMetric<U,I,P> 
{
    /**
     * Calculates the metric for each individual in the network.
     * @return a map containing the values of the metric for each user.
     */
    Map<U, Double> calculateIndividuals();
    
    /**
     * Calculates the metric for each individual user on each iteration of a simulation
     * @param simulation the whole simulation.
     * @return the value of the metric for each iteration.
     */
    default List<Map<U,Double>> calculateIndividuals(Simulation<U, I, P> simulation)
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
     * Calculates the metric value for a single user
     * @param user the single user
     * @return the value of the metric, NaN if something failed.
     */
    double calculate(U user);

    /**
     * Calculates the metric for a single user on each iteration of a simulation
     * @param user the user.
     * @param simulation the whole simulation
     * @return the value of the metric for that user in each iteration.
     */
    default List<Double> calculate(U user, Simulation<U, I, P> simulation)
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
