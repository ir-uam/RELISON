/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor;

import java.util.*;
import java.util.stream.Stream;

/**
 * Class that represents a Machine Learning dataset for link prediction / contact
 * recommendation.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class InstanceSet<U> 
{
    /**
     * Information about the different features in the dataset.
     */
    private final FeatureInformation featuresInfo;
    
    private final Map<U, Map<U, Instance<U>>> originInstances;
    private final Map<U, Map<U, Instance<U>>> destInstances;
    private final Set<Integer> classes;
    private int size;
    
    /**
     * Constructor.
     * @param featuresInfo information about the features.
     */
    public InstanceSet(FeatureInformation featuresInfo)
    {
        this.featuresInfo = featuresInfo;
        this.originInstances = new HashMap<>();
        this.destInstances = new HashMap<>();
        this.classes = new HashSet<>();
        this.size = 0;
    }
    
    /**
     * Constructor.
     * @param featureNames names of the features.
     * @param featureTypes types of the features.
     */
    public InstanceSet(List<String> featureNames, List<FeatureType> featureTypes)
    {
        this(new FeatureInformation(featureNames, featureTypes));
    }
    
    /**
     * Constructor.
     * @param featuresInfo information about the features.
     * @param patterns a list of patterns.
     */
    public InstanceSet(FeatureInformation featuresInfo, Stream<Instance<U>> patterns)
    {
        this.featuresInfo = featuresInfo;
        this.originInstances = new HashMap<>();
        this.destInstances = new HashMap<>();
        this.classes = new HashSet<>();
        this.size = 0;
        int numFeats = featuresInfo.numFeats();
        patterns.forEach(pat -> 
        {
            U u = pat.getOrigin();
            U v = pat.getDest();

            if(!this.originInstances.containsKey(u))
            {
                this.originInstances.put(u, new HashMap<>());
            }
            this.originInstances.get(u).put(v, pat);
            
            if(!this.destInstances.containsKey(v))
            {
                this.destInstances.put(v, new HashMap<>());
            }
            this.destInstances.get(v).put(u, pat);
            
            this.classes.add(pat.getCategory());
            this.featuresInfo.updateStats(pat);
            ++this.size;
        });
    }
    
    /**
     * Constructor.
     * @param featureNames names of the features.
     * @param featureTypes types of the features.
     * @param patterns a list of patterns.
     */
    public InstanceSet(List<String> featureNames, List<FeatureType> featureTypes, Stream<Instance<U>> patterns)
    {
        this(new FeatureInformation(featureNames, featureTypes), patterns);
    }
    
    /**
     * Obtain the whole set of patterns.
     * @return a stream with all the patterns.
     */
    public Stream<Instance<U>> getAllInstances()
    {
        List<Instance<U>> pats = new ArrayList<>();
        originInstances.forEach((u,val) -> val.forEach((v, pattern) -> pats.add(pattern)));
        return pats.stream();
    }
    
    /**
     * Obtain the set of patterns for a user (as edge origin).
     * @param u the user.
     * @return the set of patterns for the user.
     */
    public Stream<Instance<U>> getAllInstancesWithOrigin(U u)
    {
        List<Instance<U>> pats = new ArrayList<>();
        originInstances.get(u).forEach((v,pattern) -> pats.add(pattern));
        return pats.stream();
    }
    
    /**
     * Obtain the set of patterns for a user (as edge destination).
     * @param v the user.
     * @return the set of patterns for the user.
     */
    public Stream<Instance<U>> getAllInstancesWithDest(U v)
    {
        List<Instance<U>> pats = new ArrayList<>();
        destInstances.get(v).forEach((u,pattern) -> pats.add(pattern));
        return pats.stream();
    }
    
    /**
     * Gets an individual pattern (if it exists).
     * @param u the origin user.
     * @param v the destination user.
     * @return the pattern if it exists, an empty object otherwise.
     */
    public Optional<Instance<U>> getInstance(U u, U v)
    {
        if(originInstances.containsKey(u) && originInstances.get(u).containsKey(v))
        {
            return Optional.of(originInstances.get(u).get(v));
        }
        else
        {
            return Optional.empty();
        }
    }
    
    /**
     * Adds a pattern to the set.
     * @param pat the pattern.
     */
    public void addInstance(Instance<U> pat)
    {
        U u = pat.getOrigin();
        U v = pat.getDest();
        
        if(!this.originInstances.containsKey(u))
        {
            this.originInstances.put(u, new HashMap<>());
        }
        this.originInstances.get(u).put(v, pat);

        if(!this.destInstances.containsKey(v))
        {
            this.destInstances.put(v, new HashMap<>());
        }
        this.destInstances.get(v).put(u, pat);
        this.classes.add(pat.getCategory());
        this.featuresInfo.updateStats(pat);
        this.size++;
    }
    
    /**
     * Returns an object containing information about the parameters.
     * @return the information about the parameters.
     */
    public FeatureInformation getFeatInfo()
    {
        return this.featuresInfo;
    }
    
    public Set<Integer> getClasses()
    {
        return this.classes;
    }
    
    public int getNumInstances()
    {
        return this.size;
    }
    
    public Set<U> getOriginUsers()
    {
        return this.originInstances.keySet();
    }
    
    public Set<U> getDestUsers()
    {
        return this.destInstances.keySet();
    }
}
