/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.features.global;

import es.uam.eps.ir.socialranksys.diffusion.metrics.features.AbstractFeatureGlobalSimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;
import es.uam.eps.ir.socialranksys.utils.indexes.MonteCarloGiniCollection;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Computes the number of pieces of information propagated and seen in all the iterations.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the user
 * @param <I> type of the information
 * @param <P> type of the parameters
 */
public class MonteCarloFeatureGlobalGini<U extends Serializable,I extends Serializable,P> extends AbstractFeatureGlobalSimulationMetric<U,I,P>
{

    /**
     * Name fixed value
     */
    private final static String GLOBALGINI = "mc-feat-gl-ginicompl";

    /**
     * Times each parameter has been received.
     */
    private final Map<P,Double> values;
    
    /**
     * The total number of external parameters that have reached the different users.
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
        private MonteCarloGiniCollection mcList;
    private final int numMC;

    /**
     * Constructor.
     * @param userparam true if we are using a user parameter, false if we are using an information piece parameter.
     * @param parameter the name of the parameter.
     * @param unique true if a piece of information is considered once, false if it is considered each time it appears.
     * @param numMC number of MonteCarlo samples.
     */
    public MonteCarloFeatureGlobalGini(String parameter,boolean userparam, boolean unique, int numMC) 
    {
        super(GLOBALGINI + "-" + (userparam ? "user" : "info") + "-" + parameter + "-" + (unique ? "unique" : "repetitions"), userparam, parameter);
        
        this.values = new HashMap<>();
        this.unique = unique;
        this.numMC = numMC;
    }

    @Override
    public double calculate() 
    {
        if(!this.isInitialized())
            return Double.NaN;
        
        GiniIndex giniIndex = new GiniIndex();
        double gini = giniIndex.compute(this.values.values().stream(), true, count, sum);
        gini = (gini - mcList.averageValue())/(mcList.maxValue() - mcList.averageValue());
        
        return (1.0 - gini)/2.0;
    }
    
    @Override
    protected void updateUserParam(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
     
        double auxSum = this.sum;
        iteration.getReceivingUsers().forEach(u ->
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
            })
        );
        
        if(!unique)
        {
            iteration.getReReceivingUsers().forEach(u ->
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
                })
            );
        }
        
        this.mcList.update(Double.valueOf(this.sum - auxSum).intValue());
    }
    
    @Override
    protected void updateInfoParam(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
        double auxSum = this.sum;
 
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p ->
                {
                    this.values.put(p.v1, this.values.get(p.v1) + p.v2);
                    this.sum += p.v2;
                });
            })
        );
        
        iteration.getReReceivingUsers().forEach(u ->
            iteration.getReReceivedInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p ->
                {
                    this.values.put(p.v1, this.values.get(p.v1) + p.v2);
                    this.sum += p.v2;
                });
            })
        );
        
        this.mcList.update(Double.valueOf(this.sum - auxSum).intValue());

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
            data.getAllFeatureValues(this.getParameter()).forEach(p -> this.values.put(p, 0.0));
            this.count = this.values.size();
            this.sum = 0.0;
            this.mcList = new MonteCarloGiniCollection(this.count, 0, this.numMC);
            this.initialized = true;
        }
    }

    
    
}
