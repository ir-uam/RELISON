/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.ml.balance;

import es.uam.eps.ir.socialranksys.links.data.letor.FeatureInformation;
import es.uam.eps.ir.socialranksys.links.data.letor.FeatureType;
import es.uam.eps.ir.socialranksys.links.data.letor.Instance;
import es.uam.eps.ir.socialranksys.links.data.letor.InstanceSet;
import es.uam.eps.ir.socialranksys.utils.generator.Generator;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.*;

/**
 * Balances a dataset using the Synthetic Minority Over-Sampling Technique (SMOTE)
 * 
 * Chawla, N.V, Bowyer, K.W., Hall, L.O., Kegelmeyer, W.P. SMOTE: Synthetic Minority Over-sampling Technique. 
 * Journal of Artificial Intelligence Research 16 (2002),pp. 321-357.
 * 
 * @author Javier Sanz-Cruzado Puig
 */
public class SMOTEBalancer<U> implements Balancer<U>
{
    
    /**
     * Number of neighbours.
     */
    private final int k;
    
    private final Generator<U> gen;
    /**
     * Constructor
     * @param k Number of neighbours.
     */
    public SMOTEBalancer(int k, Generator<U> gen, U init)
    {
        this.k = k;
        this.gen = gen;
        gen.reset(init);
    }
    
    @Override
    public InstanceSet<U> balance(InstanceSet<U> original)
    {
        // Initialize the new pattern set with the original values.
        FeatureInformation featInfo = original.getFeatInfo();
        FeatureInformation newFeatInfo = new FeatureInformation(featInfo.getFeatureDescriptions(), featInfo.getFeatureTypes());
        InstanceSet<U> patternSet = new InstanceSet<>(newFeatInfo, original.getAllInstances());
        
        // Divide the different patterns according to the classes.
        Map<Integer, List<Instance<U>>> classpatterns = new HashMap<>();
        Set<Integer> categories = original.getClasses();
        List<Integer> classes = new ArrayList<>(categories);
        classes.forEach(clase -> classpatterns.put(clase, new ArrayList<>()));
        
        original.getAllInstances().forEach(pat -> classpatterns.get(classes.get(pat.getCategory())).add(pat));

        // Compute the number of patterns of the most populated class.
        int max = Integer.MIN_VALUE;
        for(int cl : classes)
        {
            int listsize = classpatterns.get(cl).size();
            if(listsize > max)
            {
                max = listsize;
            }
        }
        
        
        int numAttr = original.getFeatInfo().numFeats();
        List<String> attrNames = new ArrayList<>();
        List<FeatureType> types = new ArrayList<>();
        
        for(int i = 0; i < featInfo.numFeats(); ++i)
        {
            attrNames.add(featInfo.getFeatureDescription(i));
            types.add(featInfo.getFeatureType(i));
        }
          
        List<Instance<U>> patterns = new ArrayList<>();
        List<Integer> patClass = new ArrayList<>();
        
        for(int cl: classes)
        {
            // First, we store the already existent patterns.
            classpatterns.get(cl).forEach(pat -> {
               patterns.add(pat);
               patClass.add(cl);
            });
            
            if(classpatterns.get(cl).size() < max)
            {
                int numNewInstances = max - classpatterns.get(cl).size();
                List<Instance<U>> newInstances = this.generateNewInstances(numNewInstances, this.k, classpatterns.get(cl), types);
                patterns.addAll(newInstances);

                for (Instance<U> newInstance : newInstances)
                {
                    patternSet.addInstance(newInstance);
                }
            }
        }
        
        return patternSet;
    }
    
    /**
     * Given a pattern and its neighbour, generates a new pattern.
     * @param numExtra Number of extra examples to compute.
     * @param p the pattern
     * @param neighbourhood the neighbourhood of the pattern
     * @param types types of the attributes.
     * @return the number of attributes.
     */
    private List<Instance<U>> populate(int numExtra, Instance<U> p, List<Instance<U>> neighbourhood, List<FeatureType> types)
    {
        Random r = new Random();
        
        List<Instance<U>> newpatterns = new ArrayList<>();
        for(int i = numExtra; i > 0; --i)
        {
            int selection = r.nextInt(neighbourhood.size());
            Instance<U> q = neighbourhood.get(selection);
            
            List<Double> list = new ArrayList<>();
            for(int j = 0; j < p.getValues().size(); ++j)
            {
                double variable;
                if(types.get(j) == FeatureType.CONTINUOUS)
                {
                    double dif = q.getValue(j) - p.getValue(j);
                    double gap = r.nextDouble();
                    variable = p.getValue(j) + gap*dif;
                }
                else
                {
                    variable = (r.nextDouble() > 0.5 ? p.getValue(j) : q.getValue(j));
                }
                
                list.add(variable);
            }
            
            U u = this.gen.generate();
            Instance<U> pat = new Instance<>(u,u,list,p.getCategory());
            newpatterns.add(pat);
        }
        
        return newpatterns;
    }
    
    /**
     * Computes the distance between two patterns. It is computed using the euclidean
     * distance. In case the attribute is nominal, it is considered that two different
     * values are at distance equal to 1.
     * @param p1 first pattern
     * @param p2 second pattern
     * @param types types of the attributes
     * @return the distance.
     */
    private double distance(Instance<U> p1, Instance<U> p2, List<FeatureType> types)
    {
        double distance = 0.0;
                
        for(int i = 0; i < p1.getValues().size(); ++i)
        {
            if(types.get(i) == FeatureType.CONTINUOUS)
            {
                distance += Math.pow(p1.getValue(i) - p2.getValue(i),2.0);
            }
            else // if (types.get(i) == NOMINAL)
            {
                distance += (p1.getValue(i) == p2.getValue(i) ? 0.0 : 1.0);
            }
        }
        
        return Math.sqrt(distance);
    }

    private List<Instance<U>> generateNewInstances(int numNewInstances, int k, List<Instance<U>> minInstances, List<FeatureType> types) 
    {
        Random r = new Random();
        List<Instance<U>> newpatterns = new ArrayList<>();
        
        if(numNewInstances <= 0) //If no new set of patterns has to be computed.
            return newpatterns;
        
        double[][] distances = new double[minInstances.size()][minInstances.size()];
        
        for(int i = 0; i < minInstances.size(); ++i)
        {
            for(int j = 0; j <= i; ++j)
            {
                distances[i][j] = this.distance(minInstances.get(i), minInstances.get(j), types);
                distances[j][i] = distances[i][j];
            }
        }
    
        double percent = (numNewInstances + 0.0) / (minInstances.size() + 0.0);
        int numExtra = Double.valueOf(Math.ceil(percent)).intValue();

        for(int i = 0; i < minInstances.size(); ++i)
        {
            if(percent < 1.0)
            {
                if(r.nextDouble() >= percent)
                {
                    continue;
                }
            }
            
            // Compute the neighborhood
            Queue<Tuple2id> queue = new PriorityQueue<>(k, Comparator.comparingDouble((Tuple2id a) -> a.v2));
            
            for(int j = 0; j < minInstances.size(); ++j)
            {
                if(i != j)
                    queue.add(new Tuple2id(j, distances[i][j]));
            }
            
            int n = 0;
            List<Instance<U>> neighbourhood = new ArrayList<>();
            while(n < k && !queue.isEmpty())
            {
                neighbourhood.add(minInstances.get(queue.poll().v1));
                ++n;
            }
            
            List<Instance<U>> extra = this.populate(numExtra, minInstances.get(i), neighbourhood, types);
            newpatterns.addAll(extra);
        }
        return newpatterns;
    }
}
