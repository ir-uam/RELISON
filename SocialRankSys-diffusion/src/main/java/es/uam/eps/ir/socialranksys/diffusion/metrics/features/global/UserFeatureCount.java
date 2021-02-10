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
 * Counts the number of different pairs (user, feature) that have appeared during the simulation.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the user.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class UserFeatureCount<U extends Serializable,I extends Serializable,P> extends AbstractFeatureGlobalSimulationMetric<U,I,P>
{

    /**
     * Name fixed value.
     */
    private final static String RECALL = "global-count";

    /**
     * Stores (if it is necessary), a relation between users and parameters.
     */
    private final Map<U, Set<P>> recParams;

    /**
     * The total number of external parameters that have reached the different users.
     */
    private double total;    
    
    /**
     * Constructor.
     * @param userparam true if we are using a user parameter, false if we are using an information piece parameter.
     * @param parameter the name of the parameter.
     */
    public UserFeatureCount(String parameter, boolean userparam) 
    {
        super(RECALL + "-" + (userparam ? "user" : "info") + "-" + parameter, userparam, parameter);
        this.recParams = new HashMap<>();
    }

    @Override
    public void clear() 
    {
        this.total = 0.0;
        this.recParams.clear();
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        return this.total;
    }

    /**
     * Updates the necessary values for computing the metric (when using user parameters).
     * @param iteration the iteration data.
     */
    @Override
    protected void updateUserParam(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
        
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i ->
                data.getCreators(i.v1()).forEach(creator ->
                    data.getUserFeatures(creator, this.getParameter()).forEach(p -> 
                    {
                        if(!this.recParams.get(u).contains(p.v1()))
                        {
                            this.recParams.get(u).add(p.v1());
                            ++total;
                        }
                    })
                )
            )
        );
    }
    
    /**
     * Updates the necessary values for computing the metric (when using user parameters).
     * @param iteration the iteration data.
     */
    @Override
    protected void updateInfoParam(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
        
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i ->
                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p ->
                {
                    if(!this.recParams.get(u).contains(p.v1()))
                    {
                        this.recParams.get(u).add(p.v1());
                        ++total;
                    }
                })
            )
        );
    }

    @Override
    protected void initialize() 
    {
        if(!this.isInitialized() && this.data != null && data.doesFeatureExist(this.getParameter()))
        {
            this.recParams.clear();
            data.getAllUsers().forEach(u -> this.recParams.put(u, new HashSet<>()));
            this.total = 0.0;
            this.initialized = true;
        }
        
    }
}
