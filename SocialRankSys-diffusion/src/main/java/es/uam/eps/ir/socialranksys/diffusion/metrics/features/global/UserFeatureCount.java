/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.features.global;

import es.uam.eps.ir.socialranksys.diffusion.metrics.features.AbstractFeatureGlobalSimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Metric that computes the number of different (user, feature) pairs which have appeared during the simulation.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information pieces features.
 */
public class UserFeatureCount<U extends Serializable,I extends Serializable, F> extends AbstractFeatureGlobalSimulationMetric<U,I, F>
{
    /**
     * Name fixed value.
     */
    private final static String RECALL = "global-count";

    /**
     * Stores (if it is necessary), a relation between users and features.
     */
    private final Map<U, Set<F>> receivedFeatures;

    /**
     * The total number of external parameters that have reached the different users.
     */
    private double total;    
    
    /**
     * Constructor.
     * @param userFeats true if we are using a user feature, false if we are using an information piece feature.
     * @param feature   the name of the feature.
     */
    public UserFeatureCount(String feature, boolean userFeats)
    {
        super(RECALL + "-" + (userFeats ? "user" : "info") + "-" + feature, userFeats, feature);
        this.receivedFeatures = new HashMap<>();
    }

    @Override
    public void clear() 
    {
        this.total = 0.0;
        this.receivedFeatures.clear();
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        return this.total;
    }

    /**
     * Updates the necessary values for computing the metric (when using user features).
     * @param iteration the iteration data.
     */
    @Override
    protected void updateUserFeature(Iteration<U,I, F> iteration)
    {
        if(iteration == null) return;
        
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i ->
                data.getCreators(i.v1()).forEach(creator ->
                    data.getUserFeatures(creator, this.getFeature()).forEach(p ->
                    {
                        if(!this.receivedFeatures.get(u).contains(p.v1()))
                        {
                            this.receivedFeatures.get(u).add(p.v1());
                            ++total;
                        }
                    })
                )
            )
        );
    }
    
    /**
     * Updates the necessary values for computing the metric (when using information pieces features).
     * @param iteration the iteration data.
     */
    @Override
    protected void updateInfoFeature(Iteration<U,I, F> iteration)
    {
        if(iteration == null) return;
        
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i ->
                data.getInfoPiecesFeatures(i.v1(), this.getFeature()).forEach(p ->
                {
                    if(!this.receivedFeatures.get(u).contains(p.v1()))
                    {
                        this.receivedFeatures.get(u).add(p.v1());
                        ++total;
                    }
                })
            )
        );
    }

    @Override
    protected void initialize() 
    {
        if(!this.isInitialized() && this.data != null && data.doesFeatureExist(this.getFeature()))
        {
            this.receivedFeatures.clear();
            data.getAllUsers().forEach(u -> this.receivedFeatures.put(u, new HashSet<>()));
            this.total = 0.0;
            this.initialized = true;
        }
        
    }
}
