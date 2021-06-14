/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.metrics.features.global;

import es.uam.eps.ir.relison.diffusion.simulation.Iteration;

import java.io.Serializable;


/**
 * Metric that computes the rate of features received by the different users which were unknown by the receiver
 * (we understand as external features those information features which are not present in the information pieces
 * created by the users, or those user features different from the receiver's ones).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information pieces features.
 */
public class ExternalFeatureGlobalRate<U extends Serializable,I extends Serializable, F> extends AbstractExternalFeatureGlobalSimulationMetric<U,I, F>
{
    /**
     * Name fixed value.
     */
    private final static String EXTPARAMRATE = "global-ext-featrate";

    /**
     * The number of external parameters that have reached the different users.
     */
    private double externalParams;
    
    /**
     * The total number of external parameters that have reached the different users.
     */
    private double totalParams;    
       
    /**
     * Indicates if a piece of information is considered once (or each time it appears if false).
     */
    private final boolean unique;
    
    /**
     * Constructor.
     * @param userparam true if we are using a user parameter, false if we are using an information piece parameter.
     * @param parameter the name of the parameter.
     * @param unique true if a piece of information is considered once, false if it is considered each time it appears.
     */
    public ExternalFeatureGlobalRate(String parameter, boolean userparam, boolean unique)
    {
        super(EXTPARAMRATE + "-" + (userparam ? "user" : "info") + "-" + parameter + "-" + (unique ? "unique" : "repetitions"), parameter, userparam);
        this.unique = unique;
    }

    @Override
    public void clear() 
    {
        this.externalParams = 0.0;
        this.totalParams = 0.0;
        this.clearOwnFeatures();
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        if(!this.isInitialized())
            return Double.NaN;
        else
        {
            if(this.totalParams == 0.0)
            {
                return 0.0;
            }
            else
            {
                return this.externalParams / this.totalParams;
            }
        }        
    }

    /**
     * Updates the necessary values for computing the metric (when using user parameters).
     * @param iteration the iteration data.
     */
    @Override
    protected void updateUserFeature(Iteration<U,I, F> iteration)
    {
        if(iteration == null) return;
        
        // For each user that has received at least a new piece of information
        iteration.getReceivingUsers().forEach(u -> 
        {
            // Get all his received pieces
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                // and its creators.
                data.getCreators(i.v1()).forEach(creator -> 
                {
                    // Identify the parameters of the creators.
                    data.getUserFeatures(creator, this.getFeature()).forEach(p ->
                    {
                        // For each one of them, update the external and total params.
                        if(!this.getOwnFeatures(u).contains(p.v1))
                        {
                            this.externalParams += p.v2*val;
                        }
                        this.totalParams += p.v2*val;
                    });
                });
            });
        });
        
        // For each user that has received at least a new piece of information
        iteration.getReReceivingUsers().forEach(u -> 
        {
            // Get all his received pieces
            iteration.getReReceivedInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                // and its creators.
                data.getCreators(i.v1()).forEach(creator -> 
                {
                    // Identify the parameters of the creators.
                    data.getUserFeatures(creator, this.getFeature()).forEach(p ->
                    {
                        // For each one of them, update the external and total params.
                        if(!this.getOwnFeatures(u).contains(p.v1))
                        {
                            this.externalParams += p.v2*val;
                        }
                        this.totalParams += p.v2*val;
                    });
                });
            });
        });
    }
    
    /**
     * Updates the necessary values for computing the metric (when using user parameters).
     * @param iteration the iteration data.
     */
    @Override
    protected void updateInfoFeature(Iteration<U,I, F> iteration)
    {
        if(iteration == null) return;
        
        // For each user that has received at least a new piece of information
        iteration.getReceivingUsers().forEach(u -> 
        {
            // Get all his received pieces
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                // Identify its parameters.
                data.getInfoPiecesFeatures(i.v1(), this.getFeature()).forEach(p ->
                {
                    // For each one of them, update the external and total params.
                    if(!this.getOwnFeatures(u).contains(p.v1))
                    {
                        this.externalParams += p.v2;
                    }
                    this.totalParams += p.v2;
                });
            });
        });
        
        // For each user that has received at least a new piece of information
        iteration.getReReceivingUsers().forEach(u -> 
        {
            // Get all his received pieces
            iteration.getReReceivedInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                // Identify its parameters.
                data.getInfoPiecesFeatures(i.v1(), this.getFeature()).forEach(p ->
                {
                    // For each one of them, update the external and total params.
                    if(!this.getOwnFeatures(u).contains(p.v1))
                    {
                        this.externalParams += p.v2;
                    }
                    this.totalParams += p.v2;
                });
            });
        });
    }

    @Override
    protected void initialize() 
    {
        if(!this.isInitialized() && this.data != null && this.data.doesFeatureExist(this.getFeature())) {
            this.externalParams = 0.0;
            this.totalParams = 0.0;
            this.computeOwnFeatures();
            this.initialized = true;
        }
    }
}
