/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.features.global;

import es.uam.eps.ir.socialranksys.diffusion.metrics.features.AbstractFeatureGlobalSimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;
import es.uam.eps.ir.socialranksys.utils.indexes.Entropy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This global metric computes the entropy of 
 * Computes the Gini index over the global distribution of parameters.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class FeatureGlobalEntropy<U extends Serializable,I extends Serializable,P> extends AbstractFeatureGlobalSimulationMetric<U,I,P>
{

    /**
     * Name fixed value
     */
    private final static String ENTROPY = "feat-gl-entropy";

    /**
     * Times each parameter has been received.
     */
    private final Map<P,Double> values;
    
    /**
     * The total number of external parameters that have reached the different users.
     */
    private double sum;
    
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
    public FeatureGlobalEntropy(String parameter,boolean userparam, boolean unique) 
    {
        super(ENTROPY + "-" + (userparam ? "user" : "info") + "-" + parameter + "-" + (unique ? "unique" : "repetitions"), userparam, parameter);
        this.values = new HashMap<>();
        this.unique = unique;
    }

    @Override
    public double calculate() 
    {
        if(!this.isInitialized())
            return Double.NaN;
        
        Entropy entropy = new Entropy();
        return entropy.compute(this.values.values().stream(), sum);
    }
    
    @Override
    protected void updateUserParam(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
     
        iteration.getReceivingUsers().forEach(u -> 
        {
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getCreators(i.v1()).forEach(creator ->
                    data.getUserFeatures(creator, this.getParameter()).forEach(p ->
                    {
                        this.values.put(p.v1, this.values.get(p.v1) + p.v2*val);
                        this.sum += p.v2*val;
                    })
                );
            });
            
            if(!unique)
            {
                iteration.getReReceivedInformation(u).forEach(i -> 
                {
                    double val = i.v2().size();
                    data.getCreators(i.v1()).forEach(creator ->
                        data.getUserFeatures(creator, this.getParameter()).forEach(p ->
                        {
                            this.values.put(p.v1, this.values.get(p.v1) + p.v2*val);
                            this.sum += p.v2*val;
                        })
                    );
                });
            }
        });
    }
    
    @Override
    protected void updateInfoParam(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
     
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p ->
                {
                    this.values.put(p.v1, this.values.get(p.v1) + p.v2*val);
                    this.sum += p.v2*val;
                });
            })
        );
        
        
        if(!unique)
        {
            iteration.getReReceivingUsers().forEach(u ->
                iteration.getReReceivedInformation(u).forEach(i -> 
                {
                    double val = i.v2().size();
                    data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p ->
                    {
                        this.values.put(p.v1, this.values.get(p.v1) + p.v2*val);
                        this.sum += p.v2*val;
                    });
                })
            );
        }
    }
    
    @Override
    public void clear() 
    {
        this.values.clear();
        this.sum = 0.0;
        this.initialized = false;
    }
    
    
    @Override
    protected void initialize() 
    {
        if(!this.isInitialized())
        {
            data.getAllFeatureValues(this.getParameter()).forEach(p -> this.values.put(p, 0.0));
            this.sum = 0.0;
            this.initialized = true;
        }
    }

    
    
}
