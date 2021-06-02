/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data.ml.balance;

import es.uam.eps.ir.sonalire.links.data.letor.FeatureInformation;
import es.uam.eps.ir.sonalire.links.data.letor.FeatureType;
import es.uam.eps.ir.sonalire.links.data.letor.Instance;
import es.uam.eps.ir.sonalire.links.data.letor.InstanceSet;
import es.uam.eps.ir.sonalire.utils.generator.Generator;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.*;

/**
 * Balances a dataset using the Synthetic Minority Over-Sampling Technique (SMOTE).
 * This method creates new instances by joining two different instances from the class.
 * 
 * <p><b>Reference:</b>Chawla, N.V, Bowyer, K.W., Hall, L.O., Kegelmeyer, W.P. SMOTE: Synthetic Minority Over-sampling Technique.
 * Journal of Artificial Intelligence Research 16 (2002),pp. 321-357.</p>
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SMOTEBalancer<U> implements Balancer<U>
{
    /**
     * Number of neighbours.
     */
    private final int k;

    /**
     * A user generator.
     */
    private final Generator<U> gen;

    /**
     * Constructor.
     * @param k number of neighbors of each instance.
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
        // First, we initialize the new pattern set with the original values:
        FeatureInformation featInfo = original.getFeatInfo();
        FeatureInformation newFeatInfo = new FeatureInformation(featInfo.getFeatureDescriptions(), featInfo.getFeatureTypes());
        InstanceSet<U> patternSet = new InstanceSet<>(newFeatInfo, original.getAllInstances());
        
        // We divide the pattern according to the classes:
        Map<Integer, List<Instance<U>>> classpatterns = new HashMap<>();
        Set<Integer> categories = original.getClasses();
        List<Integer> classes = new ArrayList<>(categories);
        classes.forEach(cat -> classpatterns.put(cat, new ArrayList<>()));
        
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

        // Then:
        int numFeats = original.getFeatInfo().numFeats();
        List<String> featNames = new ArrayList<>();
        List<FeatureType> types = new ArrayList<>();
        
        for(int i = 0; i < numFeats; ++i)
        {
            featNames.add(featInfo.getFeatureDescription(i));
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
     * Given an instance and its neighbours, generates a new list of instances.
     * @param numExtra          number of extra instances to compute.
     * @param p                 the instance.
     * @param neighbourhood     the neighbourhood of the instance
     * @param types             types of the attributes.
     * @return the number of attributes.
     */
    private List<Instance<U>> populate(int numExtra, Instance<U> p, List<Instance<U>> neighbourhood, List<FeatureType> types)
    {
        Random r = new Random();

        // Initialize the list of new instances.
        List<Instance<U>> newInstances = new ArrayList<>();
        for(int i = numExtra; i > 0; --i)
        {
            // We randomly choose a neighbor q
            int selection = r.nextInt(neighbourhood.size());
            Instance<U> q = neighbourhood.get(selection);

            // We generate a list of features.
            List<Double> list = new ArrayList<>();

            // For each feature
            for(int j = 0; j < p.getValues().size(); ++j)
            {
                double variable;
                // If it is continuous, we take a random point between the values
                // of both features.
                if(types.get(j) == FeatureType.CONTINUOUS)
                {
                    double dif = q.getValue(j) - p.getValue(j);
                    double gap = r.nextDouble();
                    variable = p.getValue(j) + gap*dif;
                }
                else // If it is nominal, we choose one of the values at random.
                {
                    variable = (r.nextDouble() > 0.5 ? p.getValue(j) : q.getValue(j));
                }
                
                list.add(variable);
            }

            // We do generate the new instance.
            U u = this.gen.generate();
            Instance<U> pat = new Instance<>(u,u,list,p.getCategory());
            newInstances.add(pat);
        }
        
        return newInstances;
    }
    
    /**
     * Computes the distance between two instances. It is computed using the euclidean
     * distance. In case the feature is nominal, it is considered that two different
     * values are at distance equal to 1.
     * @param p1    first instance.
     * @param p2    second instance.
     * @param types types of the features.
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

    /**
     * Generates new instances.
     * @param numNewInstances   the number of new instances to generate.
     * @param k                 the number of neighbors of an instance to take.
     * @param minInstances      the instances from the minority class.
     * @param types             the types of the different features.
     * @return a list of new instances.
     */
    private List<Instance<U>> generateNewInstances(int numNewInstances, int k, List<Instance<U>> minInstances, List<FeatureType> types) 
    {
        Random r = new Random();
        List<Instance<U>> newpatterns = new ArrayList<>();
        
        if(numNewInstances <= 0) //If no new set of patterns has to be computed.
            return newpatterns;

        // First, we compute the distances between the instances in the minority class.
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

        // Then, for each instance in the set:
        for(int i = 0; i < minInstances.size(); ++i)
        {
            // if percent < 1.0 (i.e. if we have to reduce the number of instances)
            if(percent < 1.0)
            {
                // we do not use this instance in this case.
                if(r.nextDouble() >= percent)
                {
                    continue;
                }
            }
            
            // We compute the neighborhood of the instance (i.e. the set of k closest instances)
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

            // Generate the additional instances taking instance i as a basis.
            List<Instance<U>> extra = this.populate(numExtra, minInstances.get(i), neighbourhood, types);
            newpatterns.addAll(extra);
        }
        return newpatterns;
    }
}
