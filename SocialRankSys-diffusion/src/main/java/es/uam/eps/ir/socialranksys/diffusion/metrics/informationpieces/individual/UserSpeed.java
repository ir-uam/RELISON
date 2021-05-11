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
 * Computes the number of information pieces received by each user in the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the user.
 * @param <I> type of the information.
 * @param <F> type of the features.
 */
public class UserSpeed<U extends Serializable,I extends Serializable, F> extends AbstractIndividualSimulationMetric<U,I, F>
{
    /**
     * Name fixed value.
     */
    private final static String SPEED = "numinfo";

    /**
     * Speed value.
     */
    private final Map<U, Double> speed;

    /**
     * Constructor.
     */
    public UserSpeed() 
    {
        super(SPEED);
        this.speed = new HashMap<>();
    }   

    @Override
    public void clear() 
    {
        this.speed.clear();
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        OptionalDouble opt = data.getAllUsers().mapToDouble(this.speed::get).average();
        return opt.isPresent() ? opt.getAsDouble() : 0.0;
    }

    @Override
    public void update(Iteration<U, I, F> iteration)
    {
        iteration.getReceivingUsers().forEach(u -> this.speed.put(u, this.speed.get(u) + iteration.getNumUniqueSeen(u)));
    }

    @Override
    protected void initialize() 
    {
        if(!this.isInitialized() && this.data != null)
        {
            this.speed.clear();
            data.getAllUsers().forEach(u -> speed.put(u, 0.0));
            this.initialized = true;
        }
    }

    @Override
    public double calculate(U user) 
    {
        if(this.isInitialized() && data.containsUser(user))
            return this.speed.get(user);
        return Double.NaN;
    }

    
}
