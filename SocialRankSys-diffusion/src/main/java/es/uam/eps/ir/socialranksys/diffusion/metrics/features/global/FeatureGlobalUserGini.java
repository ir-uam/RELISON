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
import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Computes the complement of the Gini coefficient over the different features. For each feature value, it counts the
 * number of (different) users who have received the feature.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the user.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class FeatureGlobalUserGini<U extends Serializable,I extends Serializable,P> extends AbstractFeatureGlobalSimulationMetric<U,I,P>
{

    /**
     * Name fixed value.
     */
    private final static String GLOBALGINI = "feat-gl-user-ginicompl";

    /**
     * Times each parameter has been received.
     */
    private Map<Integer, Set<Integer>> relation;
      
    /**
     * Constructor.
     * @param userFeat  true if we are using a user feature, false if we are using an information piece feature.
     * @param feature   the name of the feature.
     */
    public FeatureGlobalUserGini(String feature, boolean userFeat)
    {
        super(GLOBALGINI + "-" + (userFeat ? "user" : "info") + "-" + feature, userFeat, feature);
    }

    @Override
    public double calculate() 
    {
        if(!this.isInitialized())
            return Double.NaN;
        
        List<Double> values = this.relation.values().stream().mapToDouble(Set::size).boxed().collect(Collectors.toCollection(ArrayList::new));
        long size = values.size();
        double sum = values.stream().mapToDouble(x -> x).sum();

        System.out.println("Size: " + size + " sum: " + sum);

        GiniIndex giniIndex = new GiniIndex();
        return 1.0 - giniIndex.compute(values, true, size, sum);
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
                    data.getUserFeatures(creator, this.getFeature()).forEach(p -> {
                        int pidx = pIndex.object2idx(p.v1);
                        this.relation.get(pidx).add(uidx);
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
                    this.relation.get(pidx).add(uidx);
                });
            })
        );
    }
    
    @Override
    public void clear() 
    {
        this.relation = null;
        this.initialized = false;
    }
    
    
    @Override
    protected void initialize() 
    {
        if(!this.isInitialized())
        {
            this.relation = new HashMap<>();
            IntStream.range(0, data.numFeatureValues(this.getFeature())).forEach(pidx -> relation.put(pidx, new HashSet<>()));
            this.initialized = true;
        }
    }

    
    
}
