/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.features.indiv;

import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.*;

/**
 * Computes the number of pieces of information propagated and seen in all the iterations.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> type of the user
 * @param <I> type of the information
 * @param <P> type of the parameters
 */
public class ExternalFeatureRecall<U extends Serializable,I extends Serializable,P> extends AbstractExternalFeatureIndividualSimulationMetric<U,I,P>
{

    /**
     * Name fixed value
     */
    private final static String RECALL = "ext-recall";

    /**
     * Stores (if it is necessary), a relation between users and parameters.
     */
    private final Map<U, Set<P>> recParams;

    /**
     * The total number of external parameters that have reached the different users
     */
    private final Map<U, Double> total;

    /**
     * Constructor.
     * @param userparam true if we are using a user parameter, false if we are using an information piece parameter.
     * @param parameter the name of the parameter.
     */
    public ExternalFeatureRecall(String parameter, boolean userparam)
    {
        super(RECALL + "-" + (userparam ? "user" : "info") + "-" + parameter, parameter, userparam);
        this.recParams = new HashMap<>();
        this.total = new HashMap<>();
    }

    @Override
    public void clear() 
    {
        this.total.clear();
        this.recParams.clear();
        this.clearOwnParams();
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        OptionalDouble opt = this.calculateIndividuals().values().stream().mapToDouble(v -> v).average();
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
            // Get all his received pieces
            iteration.getSeenInformation(u).forEach(i ->
                // and its creators.
                data.getCreators(i.v1()).forEach(creator ->
                    // Identify the parameters of the creators.
                    data.getUserFeatures(creator, this.getParameter()).forEach(p -> 
                    {
                        if(!this.getOwnParams(u).contains(p.v1))
                            this.recParams.get(u).add(p.v1);
                    })
                )
            )
        );
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
            // Get all his received pieces
            iteration.getSeenInformation(u).forEach(i ->
                // Identify its parameters.
                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p -> 
                {
                    if(!this.getOwnParams(u).contains(p.v1))
                        this.recParams.get(u).add(p.v1);
                })
            )
        );
    }

    @Override
    protected void initialize() 
    {
        if(!this.isInitialized() && data.doesFeatureExist(this.getParameter()))
        {
            double auxtotal = data.numFeatureValues(this.getParameter());
            this.recParams.clear();
            data.getAllUsers().forEach(u -> 
            {
                Set<P> params = this.computeOwnParams(u);
                this.setOwnParams(u, params);
                this.recParams.put(u, new HashSet<>());
                this.total.put(u, auxtotal - params.size() + 0.0);
            });

            this.initialized = true;
        }
        
    }    

    @Override
    public double calculate(U user) 
    {
        if(!this.isInitialized())
            return Double.NaN;
        
        if(!this.data.containsUser(user)) return Double.NaN;
        
        if(this.total.getOrDefault(user, 0.0) > 0.0)
            return this.recParams.get(user).size() / this.total.get(user);
        else
            return 0.0;
    }
}
