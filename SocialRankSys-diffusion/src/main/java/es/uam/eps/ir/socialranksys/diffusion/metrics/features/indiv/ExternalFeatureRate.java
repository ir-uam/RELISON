/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.features.indiv;

import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Computes the proportion of features that reach a user and are unknown to him/her. 
 * In terms of knowledge, we consider that a user does not know about a feature if:
 * 
 * <ul>
 *  <li><b>User feature:</b> The user does not have that feature. For example, if the user
 *  belongs to community 1, the rest of communities will be considered as unknown for him</li>
 *  <li><b>Information piece feature:</b> there is no piece of information published by the user
 *  which contains that feature. For example, if user u has three tweets, containing respectively
 *  hashtag sets {sports, football}, {ir} and {ff,followfriday}, hashtag basketball will be unknown
 *  to the user, and hashtag football will be known to him</li>
 * </ul>
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users of the network.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class ExternalFeatureRate<U extends Serializable,I extends Serializable,P> extends AbstractExternalFeatureIndividualSimulationMetric<U,I,P>
{
    /**
     * Name fixed value
     */
    private final static String EXTPARAMRATE = "ext-featrate";

    /**
     * Stores the number of external parameters received
     */
    private final Map<U,Double> externalParams;
    
    /**
     * Stores the total number of external parameters received
     */
    private final Map<U,Double> totalParams;
    
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
    public ExternalFeatureRate(String parameter, boolean userparam, boolean unique) 
    {
        super(EXTPARAMRATE + "-" + (userparam ? "user" : "info") + "-" + parameter + "-" + (unique ? "unique" : "repetitions"), parameter, userparam);
        this.externalParams = new HashMap<>();
        this.totalParams = new HashMap<>();
        this.unique = unique;
    }

    @Override
    public void clear() 
    {
        this.totalParams.clear();
        this.externalParams.clear();
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
        {
            // Get all his received pieces
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());

                // and its creators.
                data.getCreators(i.v1()).forEach(creator -> 
                {
                    // Identify the parameters of the creators.
                    data.getUserFeatures(creator, this.getParameter()).forEach(p -> 
                    {
                        if(!this.getOwnParams(u).contains(p.v1))
                        {
                            this.externalParams.put(u, this.externalParams.get(u) + p.v2*val);
                        }
                        this.totalParams.put(u, this.totalParams.get(u) + p.v2*val);
                    });
                });
            });
        });
        
        if(!unique)
        {
            iteration.getReReceivingUsers().forEach(u ->
                iteration.getReReceivedInformation(u).forEach(i -> 
                {
                    double val = i.v2().size();
                    data.getCreators(i.v1()).forEach(creator ->
                        data.getUserFeatures(creator, this.getParameter()).forEach(p -> 
                        {
                            if(!this.getOwnParams(u).contains(p.v1))
                            {
                                this.externalParams.put(u, this.externalParams.get(u) + p.v2*val);
                            }
                            this.totalParams.put(u, this.totalParams.get(u) + p.v2*val);
                        })
                    );
                })
            );
        }
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
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());

                // Identify its parameters.
                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p -> 
                {
                    if(!this.getOwnParams(u).contains(p.v1))
                    {
                        this.externalParams.put(u, this.externalParams.get(u) + p.v2*val);
                    }
                    this.totalParams.put(u, this.totalParams.get(u) + p.v2*val);
                });
            });
            
            if(!unique)
            {
                iteration.getReReceivedInformation(u).forEach(i -> 
                {
                    double val = i.v2().size();

                    // Identify its parameters.
                    data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p -> 
                    {
                        if(!this.getOwnParams(u).contains(p.v1))
                        {
                            this.externalParams.put(u, this.externalParams.get(u) + p.v2*val);
                        }
                        this.totalParams.put(u, this.totalParams.get(u) + p.v2*val);
                    });
                });
            }
        });
    }

    @Override
    protected void initialize()
    {
        if (!this.isInitialized() && this.data != null && this.data.doesFeatureExist(this.getParameter()))
        {
            this.clearOwnParams();
            this.totalParams.clear();
            this.externalParams.clear();

            data.getAllUsers().forEach(u ->
            {
                Set<P> ownParamSet = this.computeOwnParams(u);

                this.setOwnParams(u, ownParamSet);
                this.totalParams.put(u, 0.0);
                this.externalParams.put(u, 0.0);
            });
            this.initialized = true;
        }
    }

    @Override
    public double calculate(U user) 
    {
        if(!this.isInitialized() || !data.containsUser(user)) return Double.NaN;
        
        double total = this.totalParams.get(user);
        double extern = this.externalParams.get(user);
        if(total > 0.0)
            return extern / total;
        else
            return 0.0;
    }
}
