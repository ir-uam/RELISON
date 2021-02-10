/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.user.global;


import es.uam.eps.ir.socialranksys.diffusion.metrics.AbstractGlobalSimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Computes the number of pieces of information propagated and seen in all the iterations.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the user.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class UserGlobalGini<U extends Serializable,I extends Serializable,P> extends AbstractGlobalSimulationMetric<U,I,P>
{

    /**
     * Name fixed value.
     */
    private final static String GINI = "global-user-gini";

    /**
     * Speed value.
     */
    private final Map<U, Double> counter;
    
    /**
     * Indicates if a piece of information is considered once (or each time it appears if false).
     */
    private final boolean unique;
    
    /**
     * Constructor.
     * @param unique true if a piece of information is considered once, false if it is considered each time it appears.
     */
    public UserGlobalGini(boolean unique) 
    {
        super(GINI + "-" + (unique ? "unique" : "repetitions"));
        this.counter = new HashMap<>();
        this.unique = unique;
    }
    
    @Override
    public void clear() 
    {
        this.counter.clear();
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        if(!this.isInitialized())
            return Double.NaN;
        
        GiniIndex gi = new GiniIndex();
        return 1.0 - gi.compute(this.counter.values().stream(), true);
    }

    @Override
    public void update(Iteration<U, I, P> iteration)
    {
        if(this.isInitialized())
        {
            iteration.getReceivingUsers().forEach(u ->
                iteration.getSeenInformation(u).forEach(i ->
                    data.getCreators(i.v1()).forEach(v ->
                        this.counter.put(v, this.counter.get(v) + (unique ? 1.0 : i.v2().size() + 0.0))
                    )
                )
            );
            
            if(!unique)
            {
                iteration.getReReceivingUsers().forEach(u ->
                    iteration.getReReceivedInformation(u).forEach(i ->
                        data.getCreators(i.v1()).forEach(v ->
                            this.counter.put(v, this.counter.get(v) + i.v2().size())
                        )
                    )
                );
            }
        }
    }

    @Override
    protected void initialize() 
    {
        if(!this.isInitialized())
        {
            this.counter.clear();
            this.data.getAllUsers().forEach(u -> this.counter.put(u, 0.0));
            this.initialized = true;
        }
    }  
}
