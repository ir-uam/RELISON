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
import it.unimi.dsi.fastutil.doubles.Double2IntMap;
import it.unimi.dsi.fastutil.doubles.Double2IntOpenHashMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeSet;

/**
 * Computes the proportion of different parameters which have been received by each
 * user in the network. We find how many different values of a parameter have been received by each user,
 * and divide this number by the number of different values for the corresponding
 * parameter.
 * 
 * We differ two cases:
 * 
 * a) User parameters: (Ex.: Communities) In this case, we take the values of the parameter
 * for the creators of the received information pieces. 
 * 
 * b) Information parameters: (Ex: hashtags) In this case, we take the values of the parameters
 * for the different information pieces which are received and observed by each individual user.
 *
 * For each user, this method does not take into account those parameters that the user already knows
 * (i.e communities in the case of user parameters, hashtags in the case of information parameters)
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> type of the user
 * @param <I> type of the information
 * @param <P> type of the parameters
 */
public class ExternalFeatureGini<U extends Serializable,I extends Serializable,P> extends AbstractExternalFeatureIndividualSimulationMetric<U,I,P>
{

    /**
     * Name fixed value
     */
    private final static String GINI = "ext-feat-gini";

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
     * For each user, registers the number of parameters with a certain frequency of appearance. Ex:
     * if value A appears 3.0 times, value B appears 3.0 times, and value C appears twice, the map
     * contains pairs (3.0,2) and (2.0,1).
     */
    private final Map<U, Double2IntMap> paramCounter;

    /**
     * For each user, registers the number of times each parameter value has been received. Ex:
     * If there are four possible parameter values, A, B, C and D, and,
     * for user u, value A has been received thrice (1 in iteration 1, 2 in iteration 3) , value
     * B has been received once (in iteration 2), value C has not been received, and value
     * D has been received five times (3 in iteration 2 and 2 in iteration 4), the map for user u
     * will contain pairs (A,3),(B,1),(C,0),(D,5).
     */
    private final Map<P, Map<U, Double>> indivParamCounter;

    /**
     * The number of different values for the feature.
     */
    private final Map<U, Integer> count;

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
    public ExternalFeatureGini(String parameter, boolean userparam, boolean unique)
    {
        super(GINI + "-" + (userparam ? "user" : "info") + "-" + parameter + "-" + (unique ? "unique" : "repetitions"), parameter, userparam);
        this.sum = new HashMap<>();
        this.paramCounter = new HashMap<>();
        this.indivParamCounter = new HashMap<>();
        this.count = new HashMap<>();
        this.unique = unique;
    }

 
    @Override
    public double calculate() 
    {
        OptionalDouble opt = this.calculateIndividuals().values().stream().mapToDouble(v -> v).average();
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
                    {
                        if(!this.getOwnParams(u).contains(param.v1()))
                        {
                            aux.put(param.v1, param.v2*val + aux.getOrDefault(param.v1, 0.0));
                        }
                    })
                );
            });
            
            this.updateMap(u, aux);
        });
        
        if(!unique)
        {
            iteration.getReReceivingUsers().forEach(u -> 
            {
                Map<P,Double> aux = new HashMap<>();
                
                iteration.getReReceivedInformation(u).forEach(i -> 
                {
                    double val = i.v2().size();
                    data.getCreators(i.v1()).forEach(v ->
                        data.getUserFeatures(v, this.getParameter()).forEach(param ->
                        {
                            if(!this.getOwnParams(u).contains(param.v1()))
                            {
                                aux.put(param.v1, param.v2*val + aux.getOrDefault(param.v1, 0.0));
                            }
                        })
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
                {
                    if(!this.getOwnParams(u).contains(param.v1()))
                    {
                        aux.put(param.v1, param.v2*val + aux.getOrDefault(param.v1, 0.0));
                    }
                });
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
                    data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(param -> 
                    {
                        if(!this.getOwnParams(u).contains(param.v1()))
                        {
                            aux.put(param.v1, param.v2*val + aux.getOrDefault(param.v1, 0.0));
                        }
                    });
                });
                
                this.updateMap(u, aux);
            });
        }
    }
    
    /**
     * Internal function for updating the individual and counter maps.
     * @param u the user
     * @param aux an auxiliary map containing the new increments of several parameters for user u.
     */
    private void updateMap(U u, Map<P, Double> aux)
    {
        Double2IntMap pcount = this.paramCounter.get(u);
        
        aux.keySet().forEach(p -> 
        {
            double oldValue = this.indivParamCounter.get(p).getOrDefault(u, 0.0);
            double newValue = oldValue + aux.get(p);

            this.sum.put(u, this.sum.get(u) + newValue - oldValue);

            pcount.put(oldValue, pcount.get(oldValue) - 1);
            if (pcount.get(oldValue) <= 0)
            {
                pcount.remove(oldValue);
            }

            pcount.put(newValue, pcount.getOrDefault(newValue, 0) + 1);
            this.indivParamCounter.get(p).put(u, newValue);

        });
    }

    @Override
    public double calculate(U user) 
    {
        if(!this.isInitialized())
        {
            return Double.NaN;
        }
        
        Double2IntMap counter = this.paramCounter.get(user);
        double gini = 0.0;
        double auxsum = this.sum.get(user);
        
        if(auxsum == 0.0)
        {
            return 0.0;
        }
        
        TreeSet<Double> list = new TreeSet<>(counter.keySet());
        double min;
        double max = 0.0;
        
        for(double num : list)
        {
            min = max + 1;
            max = min + counter.get(num) - 1;
            gini += num*(max - min + 1.0)*(max + min - this.count.get(user) - 1);
        }
        
        return 1.0 - gini/((this.count.get(user) - 1)*auxsum);
    
    }

    @Override
    public void clear() 
    {
        this.indivParamCounter.clear();
        this.clearOwnParams();
        this.paramCounter.clear();
        this.sum.clear();
        this.count.clear();
        this.initialized = false;
    }
    
    @Override
    protected void initialize() 
    {
        if(!this.isInitialized() && this.data != null && this.data.doesFeatureExist(this.getParameter())) {
            this.clearOwnParams();
            this.sum.clear();
            this.count.clear();
            this.paramCounter.clear();
            this.indivParamCounter.clear();

            this.computeOwnParams();

            int totalCount = data.numFeatureValues(this.getParameter());

            data.getAllFeatureValues(this.getParameter()).forEach(p ->
            {
                Map<U, Double> indiv = new HashMap<>();
                data.getAllUsers().filter(u -> !this.getOwnParams(u).contains(p)).forEach(u -> indiv.put(u, 0.0));
                this.indivParamCounter.put(p, indiv);

            });

            data.getAllUsers().forEach(u ->
            {
                this.sum.put(u, 0.0);

                this.count.put(u, totalCount - this.getOwnParams(u).size());
                // Initialize the counter map
                Double2IntMap pcount = new Double2IntOpenHashMap();
                pcount.defaultReturnValue(0);
                pcount.put(0.0, this.count.get(u).intValue());
                this.paramCounter.put(u, pcount);
            });

            this.initialized = true;
        }
    }
 
}
