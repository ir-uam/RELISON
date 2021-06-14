/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.data.ml.features;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the information about an attribute.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 */
public class Feature
{
    /**
     * Name of the attribute.
     */
    private final String name;
    /**
     * Type of the attribute.
     */
    private final FeatureType type;
    /**
     * The list of possible values of a nominal attribute.
     */
    private final List<String> values;
    
    /**
     * Constructor.
     * @param name Name of the attribute.
     * @param type Type of the attribute.
     */
    public Feature(String name, FeatureType type)
    {
        this.name = name;
        this.type = type;
        this.values = new ArrayList<>();
    }
    
    /**
     * In case the attribute is not continuous, adds a new possible value.
     * @param value the value.
     * @return true if the value is correctly added, false if it is not.
     */
    public boolean addValue(String value)
    {
        if(!type.equals(FeatureType.CONTINUOUS) && values.contains(value))
        {
            values.add(value);
            return true;
        }
        return false;
    }

    /**
     * Get the attribute name.
     * @return The attribute name.
     */
    public String getName() 
    {
        return name;
    }

    /**
     * Get the attribute type.
     * @return The attribute type.
     */
    public FeatureType getType()
    {
        return type;
    }

    /**
     * Get the attribute values.
     * @return  the attribute values if it is not continuous, an empty list if not.
     */
    public List<String> getValues() 
    {
        return values;
    }
    
    /**
     * Get an individual attribute value.
     * @param attr the index of the value to retrieve.
     * @return the attribute value if it exists, an empty list if not.
     */
    public String getValue(int attr) 
    {
        if(this.type.equals(FeatureType.CONTINUOUS) || attr < 0 || attr > this.values.size())
            return null;
        return values.get(attr);
    }
    
    
}
