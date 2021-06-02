/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.metrics.features.global;

import es.uam.eps.ir.sonalire.diffusion.metrics.features.AbstractFeatureGlobalSimulationMetric;
import es.uam.eps.ir.sonalire.diffusion.simulation.Iteration;
import es.uam.eps.ir.sonalire.utils.indexes.Entropy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Metric that computes the entropy over the number of times each feature has been received.
 * If we use information pieces features (i.e. hashtags) the (user, feature) value counts the number of times
 * that the user has received information pieces using that feature. In case we use user features, it is just
 * how many times the user has received information from users with that feature.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information pieces features.
 */
public class FeatureGlobalEntropy<U extends Serializable,I extends Serializable, F> extends AbstractFeatureGlobalSimulationMetric<U,I, F>
{

    /**
     * Name fixed value
     */
    private final static String ENTROPY = "feat-gl-entropy";

    /**
     * Times each parameter has been received.
     */
    private final Map<F,Double> values;
    
    /**
     * The total number of features that have reached the different users.
     */
    private double sum;
    
    /**
     * Indicates if a piece of information is considered once (or each time it appears if false).
     */
    private final boolean unique;
    
    /**
     * Constructor.
     * @param userFeat  true if we are using a user feature, false if we are using an information piece feature.
     * @param feature   the name of the feature.
     * @param unique    true if a piece of information is considered once, false if it is considered each time it appears.
     */
    public FeatureGlobalEntropy(String feature,boolean userFeat, boolean unique)
    {
        super(ENTROPY + "-" + (userFeat ? "user" : "info") + "-" + feature + "-" + (unique ? "unique" : "repetitions"), userFeat, feature);
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
    protected void updateUserFeature(Iteration<U,I,F> iteration)
    {
        if(iteration == null) return;
     
        iteration.getReceivingUsers().forEach(u -> 
        {
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getCreators(i.v1()).forEach(creator ->
                    data.getUserFeatures(creator, this.getFeature()).forEach(p ->
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
                        data.getUserFeatures(creator, this.getFeature()).forEach(p ->
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
    protected void updateInfoFeature(Iteration<U,I,F> iteration)
    {
        if(iteration == null) return;
     
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getInfoPiecesFeatures(i.v1(), this.getFeature()).forEach(p ->
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
                    data.getInfoPiecesFeatures(i.v1(), this.getFeature()).forEach(p ->
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
            data.getAllFeatureValues(this.getFeature()).forEach(p -> this.values.put(p, 0.0));
            this.sum = 0.0;
            this.initialized = true;
        }
    }

    
    
}
