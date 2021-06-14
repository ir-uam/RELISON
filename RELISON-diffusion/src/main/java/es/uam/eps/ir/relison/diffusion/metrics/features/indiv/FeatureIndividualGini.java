/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.metrics.features.indiv;

import es.uam.eps.ir.relison.diffusion.metrics.features.AbstractFeatureIndividualSimulationMetric;
import es.uam.eps.ir.relison.diffusion.simulation.Iteration;
import it.unimi.dsi.fastutil.doubles.Double2IntMap;
import it.unimi.dsi.fastutil.doubles.Double2IntOpenHashMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeSet;

/**
 * It computes the Gini complement of the distribution of times that the different values
 * of a user or information piece feature has reached the different users in the network
 * during a simulation.
 *
 * We differ two cases:
 *
 * a) User features: (Ex.: Communities) In this case, we take the values of the parameter
 * for the creators of the received information pieces.
 *
 * b) Information features: (Ex: hashtags) In this case, we take the values of the parameters
 * for the different information pieces which are received and observed by each individual user.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the user.
 * @param <I> type of the information.
 * @param <F> type of the features.
 */
public class FeatureIndividualGini<U extends Serializable,I extends Serializable, F> extends AbstractFeatureIndividualSimulationMetric<U,I, F>
{

    /**
     * Name fixed value.
     */
    private final static String GINI = "indiv-feat-gini";
       
    /**
     * For each user, this map register the total number of times each user it has received 
     * a feature value. Ex: f there are four possible feature values, A, B, C and D, and,
     * for user u, value A has been received thrice (1 in iteration 1, 2 in iteration 3) , value
     * B has been received once (in iteration 2), value C has not been received, and value
     * D has been received five times (3 in iteration 2 and 2 in iteration 4), the value of this
     * map for user u will be equal to 3+1+0+5=9.
     */
    private final Map<U, Double> sum;
    
    /**
     * For each user, registers the number of features with a certain frequency of appearance. Ex:
     * if value A appears 3.0 times, value B appears 3.0 times, and value C appears twice, the map
     * contains pairs (3.0,2) and (2.0,1).
     */
    private final Map<U, Double2IntMap> featCounter;
    
    /**
     * For each user, registers the number of times each feature value has been received. Ex:
     * If there are four possible feature values, A, B, C and D, and,
     * for user u, value A has been received thrice (1 in iteration 1, 2 in iteration 3) , value
     * B has been received once (in iteration 2), value C has not been received, and value
     * D has been received five times (3 in iteration 2 and 2 in iteration 4), the map for user u
     * will contain pairs (A,3),(B,1),(C,0),(D,5).
     */
    private final Map<F, Map<U, Double>> indivFeatCounter;
    
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
     * @param userFeat true if we are using a user feature, false if we are using an information piece feature.
     * @param feature   the name of the feature.
     * @param unique    true if a piece of information is considered once, false if it is considered each time it appears.
     */
    public FeatureIndividualGini(String feature, boolean userFeat, boolean unique)
    {
        super(GINI + "-" + (userFeat ? "user" : "info") + "-" + feature + "-" + (unique ? "unique" : "repetitions"), userFeat, feature);
        
        this.sum = new HashMap<>();
        this.featCounter = new HashMap<>();
        this.indivFeatCounter = new HashMap<>();
        this.unique = unique;
    }

 
    @Override
    public double calculate() 
    {
        OptionalDouble opt = this.calculateIndividuals().values().stream().mapToDouble(v -> v).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }

    @Override
    protected void updateUserFeatures(Iteration<U, I, F> iteration)
    {
        iteration.getReceivingUsers().forEach(u -> 
        {
            Map<F,Double> aux = new HashMap<>();
            
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getCreators(i.v1()).forEach(v ->
                    data.getUserFeatures(v, this.getParameter()).forEach(param ->
                        aux.put(param.v1, val*param.v2 + aux.getOrDefault(param.v1, 0.0))
                    )
                );
            });
            this.updateMap(u, aux);
        });
        
        if(!unique)
        {
            iteration.getReReceivingUsers().forEach(u ->
            {
                Map<F,Double> aux = new HashMap<>();

                iteration.getReReceivedInformation(u).forEach(i -> 
                {
                    double val = i.v2().size();
                    data.getCreators(i.v1()).forEach(v ->
                        data.getUserFeatures(v, this.getParameter()).forEach(param ->
                            aux.put(param.v1, val*param.v2 + aux.getOrDefault(param.v1, 0.0))
                        )
                    );
                });
                this.updateMap(u, aux);
            });
        }
    }
    
    @Override
    protected void updateInfoFeatures(Iteration<U, I, F> iteration)
    {
        iteration.getReceivingUsers().forEach(u -> 
        {
            Map<F,Double> aux = new HashMap<>();
            
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(param ->
                    aux.put(param.v1, param.v2*val + aux.getOrDefault(param.v1, 0.0))
                );
            });
            this.updateMap(u, aux);
        });   
        
        if(!unique)
        {
            iteration.getReReceivingUsers().forEach(u -> 
            {
                Map<F,Double> aux = new HashMap<>();

                iteration.getReReceivedInformation(u).forEach(i -> 
                {
                    double val = i.v2().size();
                    data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(param ->
                        aux.put(param.v1, param.v2*val + aux.getOrDefault(param.v1, 0.0))
                    );
                });
                this.updateMap(u, aux);
            });
        }
    }
    
    /**
     * Internal function for updating the individual and counter maps.
     * @param u the user
     * @param aux an auxiliary map containing the new increments of several features for user u.
     */
    private void updateMap(U u, Map<F, Double> aux)
    {
        Double2IntMap pcount = this.featCounter.get(u);
        
        aux.keySet().forEach(p -> 
        {
            double oldValue = this.indivFeatCounter.get(p).getOrDefault(u, 0.0);
            double newValue = oldValue + aux.get(p);
                
            this.sum.put(u, this.sum.get(u) + newValue - oldValue);
                
            pcount.put(oldValue, pcount.get(oldValue) - 1);
            if(pcount.get(oldValue) <= 0) 
            {
                pcount.remove(oldValue);
            }
                
            pcount.put(newValue, pcount.getOrDefault(newValue, 0) + 1);
            this.indivFeatCounter.get(p).put(u, newValue);
        });
    }

    @Override
    public double calculate(U user) 
    {
        if(!this.isInitialized())
        {
            return Double.NaN;
        }
        
        Double2IntMap counter = this.featCounter.get(user);
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
            gini += num*(max - min + 1.0)*(max + min - this.count - 1);
        }
        
        return 1.0 - gini/((this.count - 1)*auxsum);
    
    }

    @Override
    public void clear() 
    {
        this.indivFeatCounter.clear();
        this.featCounter.clear();
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
            data.getAllFeatureValues(this.getParameter()).forEach(p -> 
            {
                // Initialize the individual map:
                Map<U, Double> indiv = new HashMap<>();
                data.getAllUsers().forEach(u -> indiv.put(u, 0.0));
                this.indivFeatCounter.put(p, indiv);
            });
                    
            data.getAllUsers().forEach(u -> 
            {
                this.sum.put(u, 0.0);

                // Initialize the counter map:
                Double2IntMap pcount = new Double2IntOpenHashMap();
                pcount.defaultReturnValue(0);
                pcount.put(0.0, this.count);
                this.featCounter.put(u, pcount);
                
            });
            this.initialized = true;
        }
    }
 
}
