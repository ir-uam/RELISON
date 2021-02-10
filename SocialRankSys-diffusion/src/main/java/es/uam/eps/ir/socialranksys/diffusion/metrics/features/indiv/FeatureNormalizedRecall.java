/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.features.indiv;

import es.uam.eps.ir.socialranksys.diffusion.metrics.features.AbstractFeatureIndividualSimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.*;

/**
 * Computes the number of pieces of information propagated and seen in all the iterations.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the user.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class FeatureNormalizedRecall<U extends Serializable,I extends Serializable,P> extends AbstractFeatureIndividualSimulationMetric<U,I,P>
{

    /**
     * Name fixed value.
     */
    private final static String RECALL = "norm-recall";

    /**
     * Stores (if it is necessary), a relation between users and parameters.
     */
    private final Map<U, Set<P>> recParams;

    /**
     * The total number of external parameters that have reached the different users.
     */
    private double total;    
    
    /**
     * The total number of information pieces received by each user.
     */
    private final Map<U,Double> numPieces;
    
    /**
     * Constructor.
     * @param userparam true if we are using a user parameter, false if we are using an information piece parameter.
     * @param parameter the name of the parameter.
     */
    public FeatureNormalizedRecall(String parameter, boolean userparam) 
    {
        super(RECALL + "-" + (userparam ? "user" : "info") + "-" + parameter, userparam, parameter);
        this.recParams = new HashMap<>();
        this.numPieces = new HashMap<>();
    }

    @Override
    public void clear() 
    {
        this.total = 0.0;
        this.recParams.clear();
        this.numPieces.clear();
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        OptionalDouble opt = this.calculateIndividuals().values().stream().mapToDouble(aDouble -> aDouble).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }

    /**
     * Updates the necessary values for computing the metric (when using user parameters).
     * @param iteration the iteration data.
     */
    @Override
    protected void updateUserParam(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
        
        // For each user that has received at least a new piece of information
        iteration.getReceivingUsers().forEach(u -> 
        {
            // Get all his received pieces
            double newPiecesU = iteration.getSeenInformation(u).mapToDouble(i -> 
            {
                // and its creators.
                data.getCreators(i.v1()).forEach(creator ->
                    // Identify the parameters of the creators.
                    data.getUserFeatures(creator, this.getParameter()).forEach(p ->
                        this.recParams.get(u).add(p.v1)
                    )
                );
                
                return 1.0;
            }).sum();
            
            this.numPieces.put(u, this.numPieces.get(u) + newPiecesU);
        });
    }
    
    /**
     * Updates the necessary values for computing the metric (when using user parameters).
     * @param iteration the iteration data.
     */
    @Override
    protected void updateInfoParam(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
        
        // For each user that has received at least a new piece of information
        iteration.getReceivingUsers().forEach(u -> 
        {
            // Get all his received pieces
            double newPiecesU = iteration.getSeenInformation(u).mapToDouble(i -> 
            {
                // Identify its parameters.
                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p ->
                    this.recParams.get(u).add(p.v1)
                );
                return 1.0;
            }).sum();
            
            this.numPieces.put(u, this.numPieces.get(u) + newPiecesU);

        });
    }

    @Override
    protected void initialize() 
    {
        if(!this.isInitialized())
        {
            this.recParams.clear();
            this.numPieces.clear();
            data.getAllUsers().forEach(u -> 
            {
                this.recParams.put(u, new HashSet<>());
                this.numPieces.put(u, 0.0);
            });
            this.total = data.numFeatureValues(this.getParameter());
            this.initialized = data.doesFeatureExist(this.getParameter());
        }
        
    }    

    @Override
    public double calculate(U user) 
    {
        if(!this.isInitialized())
            return Double.NaN;
        
        if(!this.data.containsUser(user)) return Double.NaN;
        
        double numPiecesU = this.numPieces.get(user);
        if(this.total > 0.0 && numPiecesU > 0.0)
            return this.recParams.get(user).size() / (total*numPiecesU);
        else
            return 0.0;
    }
}
