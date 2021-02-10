/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;

import java.io.Serializable;

/**
 * Abstract class for representing global simulation metrics.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public abstract class AbstractGlobalSimulationMetric<U extends Serializable,I extends Serializable,P> implements GlobalSimulationMetric<U,I,P> 
{
    /**
     * The name of the metric.
     */
    private final String name;
    /**
     * Indicates if the metric has been initialized or not.
     */
    protected boolean initialized;
    /**
     * The data.
     */
    protected Data<U,I,P> data;
    
    /**
     * Constructor.
     * @param name the name of the metric.
     */
    public AbstractGlobalSimulationMetric(String name)
    {
        this.name = name;
        this.initialized = false;
        this.data = null;
    }

    @Override
    public String getName() 
    {
        return name;
    }
    
    @Override
    public boolean isInitialized()
    {
        return this.initialized;
    }
    
    @Override
    public void initialize(Data<U,I,P> data)
    {
        this.data = data;
        this.initialize();
    }
    
    /**
     * Initializes all the variables needed for computing and updating the 
     * values of the metric.
     */
    protected abstract void initialize();
}
