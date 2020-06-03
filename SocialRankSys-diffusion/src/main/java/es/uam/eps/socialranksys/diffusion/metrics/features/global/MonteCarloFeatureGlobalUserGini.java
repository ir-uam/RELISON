/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.metrics.features.global;

import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.index.Relation;
import es.uam.eps.ir.socialranksys.index.fast.FastWeightedPairwiseRelation;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;
import es.uam.eps.ir.socialranksys.utils.indexes.MonteCarloGiniCollection;
import es.uam.eps.socialranksys.diffusion.metrics.features.AbstractFeatureGlobalSimulationMetric;
import es.uam.eps.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Computes the number of pieces of information propagated and seen in all the iterations.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the user
 * @param <I> type of the information
 * @param <P> type of the parameters
 */
public class MonteCarloFeatureGlobalUserGini<U extends Serializable,I extends Serializable,P> extends AbstractFeatureGlobalSimulationMetric<U,I,P>
{

    /**
     * Number of MonteCarlo samplings
     */
    private final int numMC;
    
    /**
     * Name fixed value
     */
    private final static String GLOBALGINI = "mc-feat-gl-user-ginicompl";

    /**
     * List of MonteCarlo Gini
     */
    private MonteCarloGiniCollection mcList;
    
    
    /**
     * Number of features
     */
    private int numFeats;
    /**
     * Sum of weights.
     */
    private int sum = 0;
    
    /**
     * Times each parameter has been received.
     */
    private Relation<Integer> relation;
      
    /**
     * Constructor.
     * @param userparam true if we are using a user parameter, false if we are using an information piece parameter.
     * @param parameter the name of the parameter.
     * @param numMC number of MonteCarlo trials each iteration.
     */
    public MonteCarloFeatureGlobalUserGini(String parameter, boolean userparam, int numMC) 
    {
        super(GLOBALGINI + "-" + (userparam ? "user" : "info") + "-" + parameter, userparam, parameter);
        this.numMC = numMC;
    }

    @Override
    public double calculate() 
    {
        if(!this.isInitialized())
            return Double.NaN;
        
        Stream<Double> values = this.relation.getAllSecond().mapToDouble(p -> this.relation.numFirst(p) + 0.0).boxed();
        GiniIndex giniIndex = new GiniIndex();
        double ginivalue = giniIndex.compute(values, true);
        ginivalue = (ginivalue - this.mcList.averageValue())/(this.mcList.maxValue() - this.mcList.averageValue());
        
        return (1.0 - ginivalue)/2;
    }
    
    @Override
    protected void updateUserParam(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
        int auxSum = sum;

        
        Index<P> pIndex = data.getFeatureIndex(this.getParameter());
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i -> 
            {
                int uidx = data.getUserIndex().object2idx(u);
                data.getCreators(i.v1()).forEach(creator ->
                    data.getUserFeatures(creator, this.getParameter()).forEach(p ->
                    {
                        int pidx = pIndex.object2idx(p.v1);
                        if(!this.relation.containsPair(uidx, pidx)) this.sum++;
                        this.relation.updatePair(uidx, pidx, 1, true);
                        
                    })
                );
            })
        );
        
        this.mcList.update(this.sum - auxSum);

    }
    
    @Override
    protected void updateInfoParam(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
     
        int auxSum = sum;
        Index<P> pIndex = data.getFeatureIndex(this.getParameter());
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i -> 
            {
                int uidx = data.getUserIndex().object2idx(u);

                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p ->
                {
                    int pidx = pIndex.object2idx(p.v1);
                    if(!this.relation.containsPair(uidx, pidx)) this.sum++;
                    this.relation.updatePair(uidx, pidx, 1, true);
                });
            })
        );
        
        
        this.mcList.update(this.sum - auxSum);
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
            
            this.sum = 0;
            this.numFeats = data.numFeatureValues(this.getParameter());
            this.relation = new FastWeightedPairwiseRelation<>();
            IntStream.range(0, data.numUsers()).forEach(uidx -> relation.addFirstItem(uidx));
            IntStream.range(0, data.numFeatureValues(this.getParameter())).forEach(pidx -> relation.addSecondItem(pidx));
            this.initialized = true;
            this.mcList = new MonteCarloGiniCollection(this.numFeats, this.data.numUsers(), this.numMC);
        }
    }

    
    
}
