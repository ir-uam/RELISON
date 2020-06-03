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
 * Computes the number of different pieces of information propagated and seen in all the iterations.
 * Each pair (user, piece) is computed only once.
 * 
 * @author Javier Sanz-Cruzado Puig
 * 
 * @param <U> type of the user
 * @param <I> type of the information
 * @param <P> type of the parameters
 */
public class Speed<U extends Serializable,I extends Serializable,P> extends AbstractGlobalSimulationMetric<U,I,P>
{

    /**
     * Name fixed value
     */
    private final static String SPEED = "speed";

    /**
     * Speed value
     */
    private double speed;
    
    /**
     * Constructor.
     */
    public Speed() 
    {
        super(SPEED);
        this.speed = 0.0;
    }

    @Override
    public void clear() 
    {
        this.speed = 0.0;
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        if(!this.isInitialized())
            return Double.NaN;
        return this.speed;
        
    }

    @Override
    public void update(Iteration<U, I, P> iteration)
    {
        if(this.isInitialized())
        {
            this.speed += iteration.getNumUniqueSeen()+ 0.0;
        }
    }

    @Override
    protected void initialize() 
    {
        this.speed = 0.0;
        this.initialized = true;
    }  
}
