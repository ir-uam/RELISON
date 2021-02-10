/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.informationpieces.individual;

import es.uam.eps.ir.socialranksys.diffusion.metrics.AbstractIndividualSimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

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
public class RealPropagatedRecall<U extends Serializable,I extends Serializable,P> extends AbstractIndividualSimulationMetric<U,I,P>
{

    /**
     * Name fixed value.
     */
    private final static String PROP = "global-infoprop-recall";

    /**
     * Speed value.
     */
    private final Map<U, Double> current;
    
    /**
     * Number of currently propagated.
     */
    private final Map<U, Double> maximum;
    
    /**
     * Constructor.
     */
    public RealPropagatedRecall() 
    {
        super(PROP);
        this.current = new HashMap<>();
        this.maximum = new HashMap<>();
    }

    @Override
    public void clear() 
    {
        this.current.clear();
        this.maximum.clear();
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        OptionalDouble opt = this.calculateIndividuals().values().stream().mapToDouble(aDouble -> aDouble).average();
        return opt.isPresent() ? opt.getAsDouble() : 0.0;
    }

    @Override
    public void update(Iteration<U, I, P> iteration)
    {
        if(this.isInitialized())
        {
            iteration.getReceivingUsers().forEach(u -> 
            {
                double sum = iteration.getSeenInformation(u).mapToDouble(i -> 
                {
                   if(this.data.isRealRepropagatedPiece(u, i.v1()))
                   {
                       return 1.0;
                   }
                   else
                   {
                       return 0.0;
                   }
                }).sum();
                this.current.put(u, this.current.get(u) + sum);
            });
        }
    }

    @Override
    protected void initialize() 
    {
        // The maximum is the total number of pairs (user, infopiece)
        this.data.getAllUsers().forEach(u -> 
        {
            this.maximum.put(u, this.data.getRealPropagatedPieces(u).count() + 0.0);
            this.current.put(u, 0.0);
        });
        
        this.initialized = true;
    }  

    @Override
    public double calculate(U user) 
    {
        if(!this.maximum.containsKey(user)) return Double.NaN;
        if(this.maximum.get(user) == 0) return 0.0;
        return this.current.get(user)/this.maximum.get(user);
    }
}
