/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.metrics.user.global;

import es.uam.eps.ir.socialranksys.utils.indexes.Entropy;
import es.uam.eps.socialranksys.diffusion.metrics.AbstractGlobalSimulationMetric;
import es.uam.eps.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Computes the number of pieces of information propagated and seen in all the iterations.
 * 
 * @author Javier Sanz-Cruzado Puig
 * 
 * @param <U> type of the user
 * @param <I> type of the information
 * @param <P> type of the parameters
 */
public class UserGlobalEntropy<U extends Serializable,I extends Serializable,P> extends AbstractGlobalSimulationMetric<U,I,P>
{

    /**
     * Name fixed value
     */
    private final static String ENTROPY = "global-user-entropy";

    /**
     * Speed value
     */
    private final Map<U, Double> counter;
    
    /**
     * Number of creators of the received tweets
     */
    private double sum;
    
    /**
     * Indicates if a piece of information is considered once (or each time it appears if false).
     */
    private final boolean unique;
    
    /**
     * Constructor.
     * @param unique true if a piece of information is considered once, false if it is considered each time it appears.
     */
    public UserGlobalEntropy(boolean unique) 
    {
        super(ENTROPY + "-" + (unique ? "unique" : "repetitions"));
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
        
        Entropy entropy = new Entropy();
        return entropy.compute(this.counter.values().stream(), this.sum);
    }

    @Override
    public void update(Iteration<U, I, P> iteration)
    {
        if(this.isInitialized())
        {
            iteration.getReceivingUsers().forEach(u ->
                iteration.getSeenInformation(u).forEach(i ->
                    data.getCreators(i.v1()).forEach(v -> 
                    {
                        this.counter.put(v, this.counter.get(v) + (unique ? 1.0 : i.v2().size() + 0.0));
                        this.sum += (unique ? 1.0 : i.v2().size() + 0.0);
                    })
                )
            );
            
            if(!unique)
            {
                iteration.getReReceivingUsers().forEach(u ->
                    iteration.getReReceivedInformation(u).forEach(i ->
                        data.getCreators(i.v1()).forEach(v -> 
                        {
                            this.counter.put(v, this.counter.get(v) + i.v2().size());
                            this.sum += i.v2().size();                        
                        })
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
