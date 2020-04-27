/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Auxiliar class for combining instance set in different manners.
 * @author Javier Sanz-Cruzado
 * @param <U> Type of the users.
 */
public class InstanceSetCombiner<U>
{
    /**
     * Combines two instance sets. The combined set only contains instances which
     * are present in both sets, and share the same category.
     * 
     * This approach just concatenates the patterns.
     * 
     * @param first the first instance set.
     * @param second the second instance set.
     * @return the combined instance set.
     */
    public InstanceSet<U> combine(InstanceSet<U> first, InstanceSet<U> second)
    {
        // First, combine the feature information:
        FeatureInformation firstFeatInfo = first.getFeatInfo();
        FeatureInformation secondFeatInfo = second.getFeatInfo();
        
        // Then, just append the feature names:
        List<String> names = new ArrayList<>();
        List<FeatureType> types;

        firstFeatInfo.getFeatureDescriptions().forEach(descr -> names.add("1-" + descr));
        types = new ArrayList<>(firstFeatInfo.getFeatureTypes());
        secondFeatInfo.getFeatureDescriptions().forEach(descr -> names.add("2-" + descr));
        types.addAll(secondFeatInfo.getFeatureTypes());
        
        FeatureInformation featInfo = new FeatureInformation(names, types);
        InstanceSet<U> instanceSet = new InstanceSet<>(featInfo);
        
        first.getAllInstances().forEach(instance -> 
        {
            U u = instance.getOrigin();
            U v = instance.getDest();
            int cat = instance.getCategory();
            
            Optional<Instance<U>> opt = second.getInstance(u, v);
            if(opt.isPresent())
            {
                Instance<U> sec = opt.get();
                if(cat == sec.getCategory())
                {
                    Instance<U> newInst = this.combine(instance, sec);
                    instanceSet.addInstance(newInst);
                }
            }
        });
        
        return instanceSet;
        
    }
    
    /**
     * Combines two instance sets.The combined set only contains instances which
     * are present in both sets, and share the same category.
     * 
     * This approach selects a subset of the features for each instance.
     * 
     * @param first the first instance set.
     * @param firstFeats the indexes of the features to use from the first set.
     * @param second the second instance set.
     * @param secondFeats the indexes of the features to use from the second set.
     * @return the combined instance set.
     */
    public InstanceSet<U> combine(InstanceSet<U> first, List<Integer> firstFeats, InstanceSet<U> second, List<Integer> secondFeats)
    {
        // First, combine the feature information:
        FeatureInformation firstFeatInfo = first.getFeatInfo();
        FeatureInformation secondFeatInfo = second.getFeatInfo();
        
        // Then, just append the feature names:
        List<String> names = new ArrayList<>();
        List<FeatureType> types = new ArrayList<>();
        
        for(int idx : firstFeats)
        {
            names.add("1-"+firstFeatInfo.getFeatureDescription(idx));
            types.add(firstFeatInfo.getFeatureType(idx));
        }
        
        for(int idx : secondFeats)
        {
            names.add("2-"+secondFeatInfo.getFeatureDescription(idx));
            types.add(secondFeatInfo.getFeatureType(idx));
        }
                
        FeatureInformation featInfo = new FeatureInformation(names, types);
        InstanceSet<U> instanceSet = new InstanceSet<>(featInfo);
        
        first.getAllInstances().forEach(instance -> 
        {
            U u = instance.getOrigin();
            U v = instance.getDest();
            int cat = instance.getCategory();
            
            Optional<Instance<U>> opt = second.getInstance(u, v);
            if(opt.isPresent())
            {
                Instance<U> sec = opt.get();
                if(cat == sec.getCategory())
                {
                    Instance<U> newInst = this.combine(instance, firstFeats, sec, secondFeats);
                    instanceSet.addInstance(newInst);
                }
            }
        });
        
        return instanceSet;
        
    }
    
    /**
     * Combines two instances.
     * @param first the first instance.
     * @param second the second instance.
     * @return the combined instance.
     */
    private Instance<U> combine(Instance<U> first, Instance<U> second)
    {
        U u = first.getOrigin();
        U v = first.getDest();
        
        List<Double> values = new ArrayList<>();
        values.addAll(first.getValues());
        values.addAll(second.getValues());
        
        int cat = first.getCategory();
        
        return new Instance<>(u,v,values,cat);
    }

    /**
     * Combines two instances.
     * @param first the first instance.
     * @param firstFeats the features to use from the first instance.
     * @param second the second instance.
     * @param secondFeats the features to use from the second instance.
     * @return the combined instance.
     */
    private Instance<U> combine(Instance<U> first, List<Integer> firstFeats, Instance<U> second, List<Integer> secondFeats)
    {
        U u = first.getOrigin();
        U v = first.getDest();
        
        List<Double> values = new ArrayList<>();
        for(int idx : firstFeats)
        {
            values.add(first.getValue(idx));
        }
        
        for(int idx : secondFeats)
        {
            values.add(second.getValue(idx));
        }
        
        int cat = first.getCategory();
        
        return new Instance<>(u,v,values,cat);
    }
    
    
}
