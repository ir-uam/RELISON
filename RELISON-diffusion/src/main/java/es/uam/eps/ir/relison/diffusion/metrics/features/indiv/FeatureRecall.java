/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.metrics.features.indiv;

import es.uam.eps.ir.relison.diffusion.metrics.features.AbstractFeatureIndividualSimulationMetric;
import es.uam.eps.ir.relison.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.*;

/**
 * Computes the fraction of all the features that each user has received during the diffusion process.
 * The final result averages over the set of users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the user.
 * @param <I> type of the information.
 * @param <F> type of the user / information pieces features.
 */
public class FeatureRecall<U extends Serializable,I extends Serializable, F> extends AbstractFeatureIndividualSimulationMetric<U,I, F>
{
    /**
     * Name fixed value.
     */
    private final static String RECALL = "feat-recall";

    /**
     * Stores (if it is necessary), a relation between users and the received features.
     */
    private final Map<U, Set<F>> receivedFeats;

    /**
     * The different values of the feature.
     */
    private double total;    
    
    /**
     * Constructor.
     * @param userFeats true if we are using a user features, false if we are using an information piece features.
     * @param features  the name of the feature.
     */
    public FeatureRecall(String features, boolean userFeats)
    {
        super(RECALL + "-" + (userFeats ? "user" : "info") + "-" + features, userFeats, features);
        this.receivedFeats = new HashMap<>();
    }

    @Override
    public void clear() 
    {
        this.total = 0.0;
        this.receivedFeats.clear();
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        OptionalDouble opt = this.calculateIndividuals().values().stream().mapToDouble(aDouble -> aDouble).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }

    /**
     * Updates the necessary values for computing the metric (when using user parameters).
     * @param iteration the iteration data.
     */
    @Override
    protected void updateUserFeatures(Iteration<U,I, F> iteration)
    {
        if(iteration == null) return;
        
        // For each user that has received at least a new piece of information
        iteration.getReceivingUsers().forEach(u ->
            // Get all his received pieces
            iteration.getSeenInformation(u).forEach(i ->
                // and its creators.
                data.getCreators(i.v1()).forEach(creator ->
                    // Identify the parameters of the creators.
                    data.getUserFeatures(creator, this.getParameter()).forEach(p -> this.receivedFeats.get(u).add(p.v1))
                )
            )
        );
    }
    
    /**
     * Updates the necessary values for computing the metric (when using user parameters).
     * @param iteration the iteration data.
     */
    @Override
    protected void updateInfoFeatures(Iteration<U,I, F> iteration)
    {
        if(iteration == null) return;
        
        // For each user that has received at least a new piece of information
        iteration.getReceivingUsers().forEach(u ->
            // Get all his received pieces
            iteration.getSeenInformation(u).forEach(i ->
                // Identify its parameters.
                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p -> this.receivedFeats.get(u).add(p.v1))
            )
        );
    }

    @Override
    protected void initialize() 
    {
        if(!this.isInitialized())
        {
            this.receivedFeats.clear();
            data.getAllUsers().forEach(u -> this.receivedFeats.put(u, new HashSet<>()));
            this.total = data.numFeatureValues(this.getParameter());
            this.initialized = data.doesFeatureExist(this.getParameter());
        }
        
    }    

    @Override
    public double calculate(U user) 
    {
        if(!this.isInitialized())
            return Double.NaN;
        
        if(!this.data.containsUser(user)) return Double.NaN;
        
        if(this.total > 0.0)
            return this.receivedFeats.get(user).size() / (total);
        else
            return 0.0;
    }
}
