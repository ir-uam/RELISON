/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.ml.weka;

import es.uam.eps.ir.socialranksys.links.data.letor.*;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Class for changing between different pattern sets and Weka ones.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class WekaInstanceTranslator<U>
{
    /**
     * Translates a pattern set to a WekaInstanceSet
     * @param patternSet the original pattern set.
     * @param name the name of the dataset (ex: train/test).
     * @return the Weka pattern set.
     */
    public WekaInstanceSet<U> toWeka(InstanceSet<U> patternSet, String name)
    {
        FastVector attributes = translateFeatureInfo(patternSet);
        WekaInstanceSet<U> wekaSet = new WekaInstanceSet<>(attributes, name);
        
        patternSet.getAllInstances().forEach(pattern -> 
        {
            U u = pattern.getOrigin();
            U v = pattern.getDest();
            weka.core.Instance instance = this.translateInstance(pattern, patternSet.getFeatInfo(), wekaSet);
            wekaSet.addInstance(u,v,instance);
        });
        
        return wekaSet;
    }
    
    /**
     * Translates the information about features from the InstanceSet one
     * to Weka.
     * @param patternSet the original pattern set.
     * @return the Weka pattern set.
     */
    private FastVector translateFeatureInfo(InstanceSet<U> patternSet)
    {
        FastVector attributes = new FastVector();
        FeatureInformation featInfo = patternSet.getFeatInfo();
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
        Set<Integer> classes = patternSet.getClasses();
        FastVector classVector = new FastVector();
        for(int cat : classes)
        {
            classVector.addElement(cat+"");
        }
        attributes.addElement(new Attribute("class", classVector));
        return attributes;
    }
    
    /**
     * Translates a pattern to a Weka instance.
     * @param pattern the pattern.
     * @param wekaInstanceSet the original pattern set.
     * @return the Weka instance.
     */
    private weka.core.Instance translateInstance(Instance<U> pattern, FeatureInformation featInfo, WekaInstanceSet<U> wekaInstanceSet)
    {
        Instances instances = wekaInstanceSet.getInstances();
        
        int numFeats = featInfo.numFeats();
        List<FeatureType> types = featInfo.getFeatureTypes();
        
        weka.core.Instance inst = new weka.core.Instance(numFeats+1);
        inst.setDataset(instances);
        
        List<Double> values = pattern.getValues();
        int cat = pattern.getCategory();
        
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
