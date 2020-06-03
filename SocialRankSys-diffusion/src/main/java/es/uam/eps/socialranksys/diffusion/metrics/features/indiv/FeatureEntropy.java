/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.metrics.features.indiv;

import es.uam.eps.ir.socialranksys.utils.indexes.Entropy;
import es.uam.eps.socialranksys.diffusion.metrics.features.AbstractFeatureIndividualSimulationMetric;
import es.uam.eps.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * It computes the Gini complement of the distribution of times that the different values
 * of a user or information piece feature has reached the different users in the network
 * during a simulation.
 * 
 * We differ two cases:
 * 
 * a) User parameters: (Ex.: Communities) In this case, we take the values of the parameter
 * for the creators of the received information pieces. 
 * 
 * b) Information parameters: (Ex: hashtags) In this case, we take the values of the parameters
 * for the different information pieces which are received and observed by each individual user.
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the user
 * @param <I> type of the information
 * @param <P> type of the parameters
 */
public class FeatureEntropy<U extends Serializable,I extends Serializable,P> extends AbstractFeatureIndividualSimulationMetric<U,I,P>
{

    /**
     * Name fixed value
     */
    private final static String ENTROPY = "feat-entropy";
       
    /**
     * For each user, this map register the total number of times each user it has received 
     * a parameter value. Ex: f there are four possible parameter values, A, B, C and D, and,
     * for user u, value A has been received thrice (1 in iteration 1, 2 in iteration 3) , value
     * B has been received once (in iteration 2), value C has not been received, and value
     * D has been received five times (3 in iteration 2 and 2 in iteration 4), the value of this
     * map for user u will be equal to 3+1+0+5=9.
     */
    private final Map<U, Double> sum;
    
    /**
     * For each user, registers the number of times each parameter value has been received. Ex: 
     * If there are four possible parameter values, A, B, C and D, and,
     * for user u, value A has been received thrice (1 in iteration 1, 2 in iteration 3) , value
     * B has been received once (in iteration 2), value C has not been received, and value
     * D has been received five times (3 in iteration 2 and 2 in iteration 4), the map for user u
     * will contain pairs (A,3),(B,1),(C,0),(D,5).
     */
    private final Map<U, Map<P, Double>> indivParamCounter;
    
    /**
     * The number of different values for the feature.
     */
    private int count;
    
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
    public FeatureEntropy(String parameter,boolean userparam, boolean unique) 
    {
        super(ENTROPY + "-" + (userparam ? "user" : "info") + "-" + parameter + "-" + (unique ? "unique" : "repetitions"), userparam, parameter);
        this.sum = new HashMap<>();
        this.indivParamCounter = new HashMap<>();
        this.unique = unique;
    }

 
    @Override
    public double calculate() 
    {
        OptionalDouble opt = this.calculateIndividuals().values().stream().mapToDouble(aDouble -> aDouble).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }

    @Override
    protected void updateUserParam(Iteration<U, I, P> iteration)
    {
        iteration.getReceivingUsers().forEach(u -> 
        {
            Map<P,Double> aux = new HashMap<>();
            
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getCreators(i.v1()).forEach(v ->
                    data.getUserFeatures(v, this.getParameter()).forEach(param ->
                        aux.put(param.v1, param.v2*val + aux.getOrDefault(param.v1, 0.0))
                    )
                );
            });
            
            this.updateMap(u, aux);
        });
        
        if(!unique)
        {
            iteration.getReReceivingUsers().forEach(u -> 
            {
                Map<P,Double> aux = new HashMap<>();

                iteration.getSeenInformation(u).forEach(i -> 
                {
                    double val = i.v2().size();
                    data.getCreators(i.v1()).forEach(v ->
                        data.getUserFeatures(v, this.getParameter()).forEach(param ->
                            aux.put(param.v1, param.v2*val + aux.getOrDefault(param.v1, 0.0))
                        )
                    );
                });
                this.updateMap(u, aux);
            });
        }
    }
    
    @Override
    protected void updateInfoParam(Iteration<U, I, P> iteration) 
    {
        iteration.getReceivingUsers().forEach(u -> 
        {
            Map<P,Double> aux = new HashMap<>();
            
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(param ->
                    aux.put(param.v1, val*param.v2 + aux.getOrDefault(param.v1, 0.0))
                );
            });
            
            if(!unique)
            {
                iteration.getReReceivedInformation(u).forEach(i -> 
                {
                    double val = i.v2().size();
                    data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(param ->
                        aux.put(param.v1, val*param.v2 + aux.getOrDefault(param.v1, 0.0))
                    );
                });
            }
            
            this.updateMap(u, aux);
        });   
    }
    
    /**
     * Internal function for updating the individual and counter maps.
     * @param u the user
     * @param aux an auxiliary map containing the new increments of several parameters for user u.
     */
    private void updateMap(U u, Map<P, Double> aux)
    {
        Map<P,Double> indiv = this.indivParamCounter.get(u);
        aux.keySet().forEach(p -> 
        {
            double oldValue = indiv.getOrDefault(p, 0.0);
            double newValue = oldValue + aux.get(p);

            this.sum.put(u, this.sum.get(u) + newValue - oldValue);
            indiv.put(p, newValue);
        });
    }

    @Override
    public double calculate(U user) 
    {
        if(this.isInitialized() && this.data.containsUser(user))
        {
            Entropy entropy = new Entropy();
            return entropy.compute(this.indivParamCounter.get(user).values().stream(), this.sum.get(user));
        }
        
        return Double.NaN;
    }

    @Override
    public void clear() 
    {
        this.indivParamCounter.clear();
        this.sum.clear();
        this.count = 0;
        this.initialized = false;
    }
    
    @Override
    protected void initialize() 
    {
        if(!this.isInitialized() && data.doesFeatureExist(this.getParameter()))
        {
            this.count = data.numFeatureValues(this.getParameter());
            data.getAllUsers().forEach(u -> 
            {
                // Initialize the individual map:
                Map<P, Double> indiv = new HashMap<>();
                data.getAllFeatureValues(this.getParameter()).forEach(p -> indiv.put(p, 0.0));
                this.indivParamCounter.put(u, indiv);
                
                this.sum.put(u, 0.0);
            });
            this.initialized = true;
        }
    }
 
}
