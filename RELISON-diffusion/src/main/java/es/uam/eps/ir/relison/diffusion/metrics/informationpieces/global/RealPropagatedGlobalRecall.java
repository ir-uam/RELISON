/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.metrics.informationpieces.global;

import es.uam.eps.ir.relison.diffusion.metrics.AbstractGlobalSimulationMetric;
import es.uam.eps.ir.relison.diffusion.simulation.Iteration;

import java.io.Serializable;

/**
 * Computes the fraction of pieces which were repropagated in the real setting which have been received by the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the user.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class RealPropagatedGlobalRecall<U extends Serializable,I extends Serializable,P> extends AbstractGlobalSimulationMetric<U,I,P>
{

    /**
     * Name fixed value.
     */
    private final static String PROP = "global-infoprop-recall";

    /**
     * Speed value.
     */
    private double current;
    
    /**
     * Number of currently propagated.
     */
    private double maximum;
    
    /**
     * Constructor.
     */
    public RealPropagatedGlobalRecall()
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
