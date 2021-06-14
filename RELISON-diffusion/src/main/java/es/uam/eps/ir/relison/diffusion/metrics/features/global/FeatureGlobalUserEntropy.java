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
import es.uam.eps.ir.relison.index.Index;
import es.uam.eps.ir.relison.index.Relation;
import es.uam.eps.ir.relison.index.fast.FastWeightedPairwiseRelation;
import es.uam.eps.ir.relison.utils.indexes.Entropy;

import java.io.Serializable;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Computes the entropy over the different features. For each feature value, it counts the
 * number of (different) users who have received the feature.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the user.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class FeatureGlobalUserEntropy<U extends Serializable,I extends Serializable,P> extends AbstractFeatureGlobalSimulationMetric<U,I,P>
{

    /**
     * Name fixed value.
     */
    private final static String GLOBALGINI = "global-feat-user-entropy";

    /**
     * Times each parameter has been received.
     */
    private Relation<Integer> relation;

    /**
     * The number of different (user, feature) pairs.
     */
    private double sum;
      
    /**
     * Constructor.
     * @param userFeat  true if we are using a user feature, false if we are using an information piece feature.
     * @param feature   the name of the feature.
     */
    public FeatureGlobalUserEntropy(String feature, boolean userFeat)
    {
        super(GLOBALGINI + "-" + (userFeat ? "user" : "info") + "-" + feature, userFeat, feature);
    }

    @Override
    public double calculate() 
    {
        if(!this.isInitialized())
            return Double.NaN;
        
        Stream<Double> values = this.relation.getAllSecond().mapToDouble(p -> this.relation.numFirst(p) + 0.0).boxed();
        Entropy entropy = new Entropy();
        return entropy.compute(values, this.sum);
    }
    
    @Override
    protected void updateUserFeature(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
     
        Index<P> pIndex = data.getFeatureIndex(this.getFeature());
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i -> 
            {
                int uidx = data.getUserIndex().object2idx(u);
                data.getCreators(i.v1()).forEach(creator -> 
                    data.getUserFeatures(creator, this.getFeature()).forEach(p ->
                    {
                        int pidx = pIndex.object2idx(p.v1);
                        if(!this.relation.containsPair(uidx, pidx))
                        {
                            this.relation.addRelation(uidx, pidx, 1);
                            this.sum++;
                        }
                    })
                );
            })
        );
    }
    
    @Override
    protected void updateInfoFeature(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
     
        Index<P> pIndex = data.getFeatureIndex(this.getFeature());
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i -> 
            {
                int uidx = data.getUserIndex().object2idx(u);

                data.getInfoPiecesFeatures(i.v1(), this.getFeature()).forEach(p ->
                {
                    int pidx = pIndex.object2idx(p.v1);
                    if(!this.relation.containsPair(uidx, pidx))
                    {
                        this.relation.addRelation(uidx, pidx, 1);
                        this.sum++;
                    }
                });
            })
        );
    }
    
    @Override
    public void clear() 
    {
        this.sum = 0.0;
        this.relation = null;
        this.initialized = false;
        
    }
    
    
    @Override
    protected void initialize() 
    {
        if(!this.isInitialized())
        {
            this.sum = 0.0;
            this.relation = new FastWeightedPairwiseRelation<>();
            IntStream.range(0, data.numUsers()).forEach(uidx -> relation.addFirstItem(uidx));
            IntStream.range(0, data.numFeatureValues(this.getFeature())).forEach(pidx -> relation.addSecondItem(pidx));
            this.initialized = true;
        }
    }

    
    
}
