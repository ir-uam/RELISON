/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.data.ml.weka;

import es.uam.eps.ir.relison.links.data.letor.*;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Class for transforming InstanceSet to WekaInstanceSet.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class WekaInstanceTranslator<U>
{
    /**
     * Translates an InstanceSet to a WekaInstanceSet.
     * @param instanceSet   the original instance set.
     * @param name          the name of the dataset (ex: train/test).
     * @return the Weka pattern set.
     */
    public WekaInstanceSet<U> toWeka(InstanceSet<U> instanceSet, String name)
    {
        FastVector attributes = translateFeatureInfo(instanceSet);
        WekaInstanceSet<U> wekaSet = new WekaInstanceSet<>(attributes, name);
        
        instanceSet.getAllInstances().forEach(pattern ->
        {
            U u = pattern.getOrigin();
            U v = pattern.getDest();
            weka.core.Instance instance = this.translateInstance(pattern, instanceSet.getFeatInfo(), wekaSet);
            wekaSet.addInstance(u,v,instance);
        });
        
        return wekaSet;
    }
    
    /**
     * Given the feature information in a given InstanceSet, translates it into an object
     * containing such feature information for its use by Weka.
     *
     * @param instanceSet the original instance set.
     * @return the Weka feature information.
     */
    private FastVector translateFeatureInfo(InstanceSet<U> instanceSet)
    {
        FastVector attributes = new FastVector();
        FeatureInformation featInfo = instanceSet.getFeatInfo();
        List<String> descr = featInfo.getFeatureDescriptions();
        List<FeatureType> types = featInfo.getFeatureTypes();
        List<FastVector> nominalAttrs = new ArrayList<>();
        int numFeats = featInfo.numFeats();
        
        // Translate information about features
        for(int i = 0; i < numFeats; ++i)
        {
            String name = descr.get(i);
            FeatureType type = types.get(i);
            
            nominalAttrs.add(new FastVector());
            if(type.equals(FeatureType.CONTINUOUS))
            {
                attributes.addElement(new Attribute(name));
            }
            else
            {
                NominalStats stats = (NominalStats) featInfo.getStats(i);
                int numValues = stats.getNumValues();
                for(int j = 0; j < numValues; ++j)
                    nominalAttrs.get(i).addElement(j+"");
                attributes.addElement(new Attribute(name, nominalAttrs.get(i)));
            }
        }
        
        // Add the information about classes.
        Set<Integer> classes = instanceSet.getClasses();
        FastVector classVector = new FastVector();
        for(int cat : classes)
        {
            classVector.addElement(cat+"");
        }
        attributes.addElement(new Attribute("class", classVector));
        return attributes;
    }
    
    /**
     * Translates an individual instance into a Weka one.
     * @param instance          the original instance.
     * @param featInfo          the feature information for the original set.
     * @param wekaInstanceSet   the Weka instance set in which we want to integrate the new instance.
     * @return the Weka instance.
     */
    private weka.core.Instance translateInstance(Instance<U> instance, FeatureInformation featInfo, WekaInstanceSet<U> wekaInstanceSet)
    {
        Instances instances = wekaInstanceSet.getInstances();
        
        int numFeats = featInfo.numFeats();
        List<FeatureType> types = featInfo.getFeatureTypes();
        
        weka.core.Instance inst = new weka.core.Instance(numFeats+1);
        inst.setDataset(instances);
        
        List<Double> values = instance.getValues();
        int cat = instance.getCategory();
        
        for(int i = 0; i < numFeats; ++i)
        {
            if(types.get(i).equals(FeatureType.CONTINUOUS))
            {
                inst.setValue(i, values.get(i));
            }
            else
            {
                inst.setValue(i, ((NominalStats) featInfo.getStats(i)).indexOfValue(values.get(i))+"");
            }
        }
        inst.setValue(numFeats, cat+"");
        
        return inst;
    }    
}
