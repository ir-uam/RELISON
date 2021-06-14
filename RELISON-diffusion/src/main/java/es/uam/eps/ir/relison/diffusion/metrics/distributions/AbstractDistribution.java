/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.metrics.distributions;

import es.uam.eps.ir.relison.diffusion.data.Data;

import java.io.Serializable;

/**
 * Abstract class for defining a distribution of elements.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <F> Type of the features.
 */
public abstract class AbstractDistribution<U extends Serializable, I extends Serializable, F> implements Distribution<U,I, F>
{
    /**
     * The complete data.
     */
    protected Data<U,I, F> data;
    /**
     * Indicates if it has been initialized.
     */
    protected boolean initialized;
    
    /**
     * The distribution name
     */
    private final String name;
    
    /**
     * Constructor
     * @param name the distribution name. 
     */
    public AbstractDistribution(String name)
    {
        this.name = name;
    }
    
    @Override
    public void initialize(Data<U, I, F> data)
    {
        this.data = data;
        this.initialize();
    }
    
    /**
     * Initializes the necessary variables.
     */
    protected abstract void initialize();
    
    @Override
    public String getName()
    {
        return this.name;
    }
    
    @Override
    public boolean isInitialized()
    {
        return this.initialized;
    }
}
