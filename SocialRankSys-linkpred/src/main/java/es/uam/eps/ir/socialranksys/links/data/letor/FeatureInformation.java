/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor;

import es.uam.eps.ir.ranksys.core.util.Stats;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for storing some information about the features for ML patterns.
 * @author Javier Sanz-Cruzado 
 */
public class FeatureInformation 
{
    /**
     * Number of different features.
     */
    private final int numFeats;
    /**
     * Description of the different features.
     */
    private final List<String> descriptions;
    /**
     * Types of the different features
     */
    private final List<FeatureType> types;
    /**
     * Statistics for the different features.
     */
    private final List<Stats> stats;
    
    /**
     * Constructor.
     * @param featureNames description of the different features.
     */
    public FeatureInformation(List<String> featureNames, List<FeatureType> types)
    {
        this.numFeats = featureNames.size();
        this.descriptions = featureNames;
        this.stats = new ArrayList<>();
        this.types = types;
        for(int i = 0; i < numFeats; ++i) 
        {
            if(types.get(i).equals(FeatureType.CONTINUOUS))
            {
                this.stats.add(new Stats());
            }
            else
            {
                this.stats.add(new NominalStats());
            }
        }
    }
    
    /**
     * The number of features.
     * @return the number of features.
     */
    public int numFeats()
    {
        return numFeats;
    }
    
    /**
     * Updates the statistics of the different features for the collection.
     * @param pattern the new pattern.
     */
    public void updateStats(Instance<?> pattern)
    {
        List<Double> values = pattern.getValues();
        for(int i = 0; i < numFeats; ++i)
        {
            if(Double.isFinite(values.get(i)))
                this.stats.get(i).accept(values.get(i));
        }
    }
    
    /**
     * Gets the statistics for the different features.
     * @return the statistics for the feature.
     */
    public List<Stats> getStats()
    {
        return this.stats;
    }
    
    /**
     * Gets the statistics for a single feature
     * @param i the identifier of the feature.
     * @return the statistics if they exist, null otherwise
     */
    public Stats getStats(int i)
    {
        if(i < 0 || i >= this.numFeats)
            return null;
        return this.stats.get(i);
    }
    
    /**
     * Obtains the descriptions of the different features.
     * @return the descriptions of the different features.
     */
    public List<String> getFeatureDescriptions()
    {
        return this.descriptions;
    }
    
    /**
     * Obtains the description of a single feature.
     * @param i the identifier of the feature.
     * @return the descriptions of the feature if it exists, null otherwise.
     */
    public String getFeatureDescription(int i)
    {
        if(i < 0 || i >= this.numFeats)
            return null;
        return this.descriptions.get(i);
    }
    
    /**
     * Obtains the types of the different features.
     * @return the types of the different features.
     */
    public List<FeatureType> getFeatureTypes()
    {
        return this.types;
    }
    
    /**
     * Obtains the description of a single feature.
     * @param i the identifier of the feature.
     * @return the descriptions of the feature if it exists, null otherwise.
     */
    public FeatureType getFeatureType(int i)
    {
        if(i < 0 || i >= this.numFeats)
            return null;
        return this.types.get(i);
    }
    
}
