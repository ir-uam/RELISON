/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.metrics.informationpieces.global;

import es.uam.eps.socialranksys.diffusion.metrics.AbstractGlobalSimulationMetric;
import es.uam.eps.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;

/**
 * Computes the number of pieces of information propagated and seen in all the iterations.
 * 
 * @author Javier Sanz-Cruzado Puig
 * 
 * @param <U> type of the user
 * @param <I> type of the information
 * @param <P> type of the parameters
 */
public class GlobalRealPropagatedRecall<U extends Serializable,I extends Serializable,P> extends AbstractGlobalSimulationMetric<U,I,P>
{

    /**
     * Name fixed value
     */
    private final static String PROP = "global-infoprop-recall";

    /**
     * Speed value
     */
    private double current;
    
    /**
     * Number of currently propagated
     */
    private double maximum;
    
    /**
     * Constructor.
     */
    public GlobalRealPropagatedRecall() 
    {
        super(PROP);
        this.current = 0.0;
    }

    @Override
    public void clear() 
    {
        this.current = 0.0;
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        if(!this.isInitialized())
            return Double.NaN;
        return this.current / this.maximum;
        
    }

    @Override
    public void update(Iteration<U, I, P> iteration)
    {
        if(this.isInitialized())
        {
            double sum = iteration.getReceivingUsers().mapToDouble(u -> 
                iteration.getSeenInformation(u)
                    .mapToDouble(i -> this.data.isRealRepropagatedPiece(u, i.v1()) ? 1.0 : 0.0)
                        .sum())
                    .sum();
            this.current += sum;
        }
    }

    @Override
    protected void initialize() 
    {
        // The maximum is the total number of pairs (user, infopiece)
        this.maximum = this.data.getAllUsers().mapToDouble(u -> this.data.getRealPropagatedPieces(u).count() + 0.0).sum();
        this.current = 0.0;
        
        this.initialized = true;
    }  
}
