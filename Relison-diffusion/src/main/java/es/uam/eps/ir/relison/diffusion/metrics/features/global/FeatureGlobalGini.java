/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.metrics.features.global;

import es.uam.eps.ir.relison.diffusion.metrics.features.AbstractFeatureGlobalSimulationMetric;
import es.uam.eps.ir.relison.diffusion.simulation.Iteration;
import es.uam.eps.ir.relison.utils.indexes.GiniIndex;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Metric that computes the complement of the Gini coefficient over the different features.
 * It provides a measure of the balance of the distribution of times each feature has been received.
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
public class FeatureGlobalGini<U extends Serializable,I extends Serializable, F> extends AbstractFeatureGlobalSimulationMetric<U,I, F>
{

    /**
     * Name fixed value
     */
    private final static String GLOBALGINI = "feat-gl-ginicompl";

    /**
     * Times each parameter has been received.
     */
    private final Map<F,Double> values;
    
    /**
     * The total number of features that have reached the different users.
     */
    private double sum;
    
    /**
     * The number of different parameters.
     */
    private int count;    
    
    /**
     * Indicates if a piece of information is considered once (or each time it appears if false).
     */
    private final boolean unique;
    
    /**
     * Constructor.
     * @param userFeats true if we are using a user feature, false if we are using an information piece feature.
     * @param feature   the name of the feature.
     * @param unique    true if a piece of information is considered once, false if it is considered each time it appears.
     */
    public FeatureGlobalGini(String feature,boolean userFeats, boolean unique)
    {
        super(GLOBALGINI + "-" + (userFeats ? "user" : "info") + "-" + feature + "-" + (unique ? "unique" : "repetitions"), userFeats, feature);
        
        this.values = new HashMap<>();
        this.unique = unique;
    }

    @Override
    public double calculate() 
    {
        if(!this.isInitialized())
            return Double.NaN;
        
        GiniIndex giniIndex = new GiniIndex();
        return 1.0 - giniIndex.compute(this.values.values().stream(), true, count, sum);
    }
    
    @Override
    protected void updateUserFeature(Iteration<U,I, F> iteration)
    {
        if(iteration == null) return;
     
        iteration.getReceivingUsers().forEach(u ->
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
            })
        );
        
        if(!unique)
        {
            iteration.getReReceivingUsers().forEach(u ->
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
                })
            );
        }
    }
    
    @Override
    protected void updateInfoFeature(Iteration<U,I, F> iteration)
    {
        if(iteration == null) return;
     
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getInfoPiecesFeatures(i.v1(), this.getFeature()).forEach(p ->
                {
                    this.values.put(p.v1, this.values.get(p.v1) + p.v2);
                    this.sum += p.v2*val;
                });
            })
        );
        
        iteration.getReReceivingUsers().forEach(u ->
            iteration.getReReceivedInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getInfoPiecesFeatures(i.v1(), this.getFeature()).forEach(p ->
                {
                    this.values.put(p.v1, this.values.get(p.v1) + p.v2);
                    this.sum += p.v2*val;
                });
            })
        );
    }
    
    @Override
    public void clear() 
    {
        this.values.clear();
        this.count = 0;
        this.sum = 0.0;
        this.initialized = false;
    }

    @Override
    protected void initialize() 
    {
        if(!this.isInitialized())
        {
            data.getAllFeatureValues(this.getFeature()).forEach(f -> this.values.put(f, 0.0));
            this.count = this.values.size();
            this.sum = 0.0;
            this.initialized = true;
        }
    }
}
