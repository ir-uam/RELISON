/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data.letor;

import java.util.*;
import java.util.stream.Stream;

/**
 * Class that represents a machine learning dataset for link prediction / contact
 * recommendation.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 *
 * @param <U> Type of the users.
 */
public class InstanceSet<U> 
{
    /**
     * Information about the different features in the dataset.
     */
    private final FeatureInformation featuresInfo;
    /**
     * Instances identified by the origin node of the edge.
     */
    private final Map<U, Map<U, Instance<U>>> originInstances;
    /**
     * Instances identified by the destination node of the edge.
     */
    private final Map<U, Map<U, Instance<U>>> destInstances;
    /**
     * Possible classes.
     */
    private final Set<Integer> classes;
    /**
     * Size of the instance set.
     */
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
     * @param featuresInfo  information about the features.
     * @param instances     a list of instances.
     */
    public InstanceSet(FeatureInformation featuresInfo, Stream<Instance<U>> instances)
    {
        this.featuresInfo = featuresInfo;
        this.originInstances = new HashMap<>();
        this.destInstances = new HashMap<>();
        this.classes = new HashSet<>();
        this.size = 0;
        int numFeats = featuresInfo.numFeats();
        instances.forEach(pat ->
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
     * @param instances    a list of instances.
     */
    public InstanceSet(List<String> featureNames, List<FeatureType> featureTypes, Stream<Instance<U>> instances)
    {
        this(new FeatureInformation(featureNames, featureTypes), instances);
    }
    
    /**
     * Obtain the whole set of instances.
     * @return a stream with all the instances.
     */
    public Stream<Instance<U>> getAllInstances()
    {
        List<Instance<U>> pats = new ArrayList<>();
        originInstances.forEach((u,val) -> val.forEach((v, pattern) -> pats.add(pattern)));
        return pats.stream();
    }
    
    /**
     * Obtain the set of instances for a user (as edge origin).
     * @param u the user.
     * @return the set of instances for the user.
     */
    public Stream<Instance<U>> getAllInstancesWithOrigin(U u)
    {
        List<Instance<U>> pats = new ArrayList<>();
        originInstances.get(u).forEach((v,pattern) -> pats.add(pattern));
        return pats.stream();
    }
    
    /**
     * Obtain the set of instances for a user (as edge destination).
     * @param v the user.
     * @return the set of instances for the user.
     */
    public Stream<Instance<U>> getAllInstancesWithDest(U v)
    {
        List<Instance<U>> pats = new ArrayList<>();
        destInstances.get(v).forEach((u,pattern) -> pats.add(pattern));
        return pats.stream();
    }
    
    /**
     * Gets an individual instance (if it exists).
     * @param u the origin user.
     * @param v the destination user.
     * @return the instance if it exists, an empty object otherwise.
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
     * Adds an instance to the set.
     * @param instance the instance.
     */
    public void addInstance(Instance<U> instance)
    {
        U u = instance.getOrigin();
        U v = instance.getDest();
        
        if(!this.originInstances.containsKey(u))
        {
            this.originInstances.put(u, new HashMap<>());
        }
        this.originInstances.get(u).put(v, instance);

        if(!this.destInstances.containsKey(v))
        {
            this.destInstances.put(v, new HashMap<>());
        }
        this.destInstances.get(v).put(u, instance);
        this.classes.add(instance.getCategory());
        this.featuresInfo.updateStats(instance);
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

    /**
     * Obtains the set of classes.
     * @return the set of classes.
     */
    public Set<Integer> getClasses()
    {
        return this.classes;
    }

    /**
     * Obtains the number of instances in the set.
     * @return the number of instances in the set.
     */
    public int getNumInstances()
    {
        return this.size;
    }

    /**
     * Obtains the users with adjacent links in the set.
     * @return the users with adjacent links in the set.
     */
    public Set<U> getOriginUsers()
    {
        return this.originInstances.keySet();
    }

    /**
     * Obtains the users with incident links in the set.
     * @return the users with incident links in the set.
     */
    public Set<U> getDestUsers()
    {
        return this.destInstances.keySet();
    }
}
