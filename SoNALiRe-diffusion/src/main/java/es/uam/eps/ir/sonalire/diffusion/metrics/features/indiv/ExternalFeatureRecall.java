/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.metrics.features.indiv;

import es.uam.eps.ir.sonalire.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.*;

/**
 * Estimates the fraction of the unknown features of a user have been discovered thanks to the diffusion.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the user.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class ExternalFeatureRecall<U extends Serializable,I extends Serializable,P> extends AbstractExternalFeatureIndividualSimulationMetric<U,I,P>
{

    /**
     * Name fixed value.
     */
    private final static String RECALL = "ext-recall";

    /**
     * Stores (if it is necessary), a relation between users and features.
     */
    private final Map<U, Set<P>> receivedFeats;

    /**
     * The total number of external parameters that have reached the different users
     */
    private final Map<U, Double> total;

    /**
     * Constructor.
     * @param userFeat  true if we are using a user feature, false if we are using an information piece feature.
     * @param feature   the name of the feature.
     */
    public ExternalFeatureRecall(String feature, boolean userFeat)
    {
        super(RECALL + "-" + (userFeat ? "user" : "info") + "-" + feature, feature, userFeat);
        this.receivedFeats = new HashMap<>();
        this.total = new HashMap<>();
    }

    @Override
    public void clear() 
    {
        this.total.clear();
        this.receivedFeats.clear();
        this.clearOwnFeatures();
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        OptionalDouble opt = this.calculateIndividuals().values().stream().mapToDouble(v -> v).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }

    /**
     * Updates the necessary values for computing the metric (when using user features).
     * @param iteration the iteration data.
     */
    @Override
    protected void updateUserFeatures(Iteration<U,I,P> iteration)
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
                        if(!this.getOwnFeats(u).contains(p.v1))
                            this.receivedFeats.get(u).add(p.v1);
                    })
                )
            )
        );
    }
    
    /**
     * Updates the necessary values for computing the metric (when using information piece features).
     * @param iteration the iteration data.
     */
    @Override
    protected void updateInfoFeatures(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
        
        // For each user that has received at least a new piece of information
        iteration.getReceivingUsers().forEach(u ->
            // Get all his received pieces
            iteration.getSeenInformation(u).forEach(i ->
                // Identify its parameters.
                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p -> 
                {
                    if(!this.getOwnFeats(u).contains(p.v1))
                        this.receivedFeats.get(u).add(p.v1);
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
            this.receivedFeats.clear();
            data.getAllUsers().forEach(u -> 
            {
                Set<P> params = this.computeOwnFeatures(u);
                this.setOwnFeatures(u, params);
                this.receivedFeats.put(u, new HashSet<>());
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
            return this.receivedFeats.get(user).size() / this.total.get(user);
        else
            return 0.0;
    }
}
