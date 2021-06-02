/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data.ml.weka;

import es.uam.eps.ir.sonalire.utils.datatypes.Pair;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instance set to use with Weka classifiers.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class WekaInstanceSet<U>
{
    /**
     * The information about the different features in the dataset.
     */
    private final FastVector features;
    /**
     * The set of Weka instances of the dataset.
     */
    private final Instances values;
    /**
     * A map that links the (u,v) link with the instance, indexed by the origin node.
     */
    private final Map<U, Map<U, Integer>> origInstances;
    /**
     * A map that links the (u,v) link with the instance, indexed by the destination node.
     */
    private final Map<U, Map<U, Integer>> destInstances;
    /**
     * The set of user pairs in the instance set.
     */
    private final List<Pair<U>> users;
    /**
     * The total number of instances.
     */
    private int numInstances;
    
    /**
     * Constructor.
     * @param features the information about the features in the dataset.
     * @param name name of the dataset (f.ex: train/test).
     */
    public WekaInstanceSet(FastVector features, String name)
    {
        this.features = features;
        this.values = new Instances(name, features, 0);
        this.origInstances = new HashMap<>();
        this.destInstances = new HashMap<>();
        this.numInstances = 0;
        this.users = new ArrayList<>();
    }
    
    /**
     * Adds a pattern associated to a given edge.
     * @param u the origin user.
     * @param v the destination user.
     * @param instance the pattern.
     */
    public void addInstance(U u, U v, Instance instance)
    {
        instance.setDataset(values);
        this.values.add(instance);
        
        if(!origInstances.containsKey(u))
            this.origInstances.put(u, new HashMap<>());
        this.origInstances.get(u).put(v, numInstances+1);
        
        if(!destInstances.containsKey(v))
            this.destInstances.put(v, new HashMap<>());
        this.destInstances.get(v).put(u, numInstances+1);
        
        this.users.add(new Pair<>(u,v));
        this.numInstances++;
        
    }
    
    /**
     * Obtains the whole set of instances.
     * @return the whole set of instances.
     */
    public Instances getInstances()
    {
        return this.values;
    }
    
    /**
     * Obtains the vector containing the attributes.
     * @return the attributes.
     */
    public FastVector getFeatures()
    {
        return this.features;
    }
    
    /**
     * Obtains an individual instance.
     * @param u the origin user.
     * @param v the destination user.
     * @return the instance if it exists, null otherwise
     */
    public Instance getInstance(U u, U v)
    {
        int idx = -1;
        if(origInstances.containsKey(u))
        {
            if(origInstances.get(u).containsKey(v))
                idx = origInstances.get(u).get(v);
        }
        
        if(idx != -1)
        {
            return values.instance(idx);
        }
        
        return null;
    }
    
    /**
     * Gets the set of instances which have a user as origin of the edge.
     * @param u the origin of the edge.
     * @return a map containing the instances which have u as origin user.
     */
    public Map<U, Instance> getInstancesWithOrigin(U u)
    {
        Map<U, Instance> map = new HashMap<>();
        if(origInstances.containsKey(u))
        {
            origInstances.get(u).forEach((v,inst) -> map.put(v, values.instance(inst)));
        }
        return map;
    }
    
    /**
     * Gets the set of instances which have a user as destination of the edge.
     * @param v the destination of the edge.
     * @return a map containing the instances which have v as destination user.
     */
    public Map<U, Instance> getInstancesWithDest(U v)
    {
        Map<U, Instance> map = new HashMap<>();
        if(destInstances.containsKey(v))
        {
            destInstances.get(v).forEach((u,inst) -> map.put(u, values.instance(inst)));
        }
        return map;
    }

    /**
     * Obtains a relationship between pairs of users and instances.
     * @return the relationship between pairs of users and instances.
     */
    public Map<Pair<U>, Integer> getInstanceMap()
    {
        Map<Pair<U>, Integer> map = new HashMap<>();
        for(int i = 0; i < users.size(); ++i)
        {
            map.put(users.get(i), i);
        }
        return map;
    }
    
}
