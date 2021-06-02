/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data.ml.features;

import java.util.List;

/**
 * A class for storing the set of attributes which might be used by a machine
 * learning approach.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Features
{
    /**
     * The list of features.
     */
    private final List<Feature> features;
    /**
     * The index of the class attribute in the list.
     */
    private final int classIdx;
    
    /**
     * Constructor.
     * @param attrs Attribute list.
     * @param classIdx The index of the class attribute in the list.
     */
    public Features(List<Feature> attrs, int classIdx)
    {
        this.features = attrs;
        this.classIdx = classIdx;
    }
    /**
     * Get the name of the attribute
     * @param idx index of the attribute
     * @return the name of the attribute, null if it does not exist.
     */
    public String getAttributeName(int idx)
    {
        if(this.features == null || this.features.isEmpty() || idx < 0 || idx >= features.size())
        {
            return null;
        }
        else
        {
            return this.features.get(idx).getName();
        }
    }
    
    /**
     * Get the attribute type.
     * @param idx index of the attribute.
     * @return the type of the attribute, null if it does not exist.
     */
    public FeatureType getAttributeType(int idx)
    {
        if(this.features == null || this.features.isEmpty() || idx < 0 || idx >= features.size())
        {
            return null;
        }
        else
        {
            return this.features.get(idx).getType();
        }
    }
    
    /**
     * Get the values of an attribute.
     * @param idx index of the attribute.
     * @return the values of the attribute, an empty list if it is continuous, or null if it does not exist.
     */
    public List<String> getAttributeValues(int idx)
    {
        if(this.features == null || this.features.isEmpty() || idx < 0 || idx >= features.size())
        {
            return null;
        }
        else
        {
            return this.features.get(idx).getValues();
        }
    }
    
    /**
     * Get the values of an attribute.
     * @param idx index of the attribute.
     * @param attr the index of the attribute value.
     * @return the values of the attribute, an empty list if it is continuous, or null if it does not exist.
     */
    public String getAttributeValue(int idx, int attr)
    {
        if(this.features == null || this.features.isEmpty() || idx < 0 || idx >= features.size())
        {
            return null;
        }
        else
        {
            return this.features.get(idx).getValue(attr);
        }
    }
    
    /**
     * Get the values of the class attribute.
     * @return the values of the attribute, an empty list if it is continuous, or null if it does not exist.
     */
    public List<String> getClassValues()
    {
        if(this.features == null || this.features.isEmpty() || classIdx < 0 || classIdx >= features.size())
        {
            return null;
        }
        else
        {
            return this.features.get(classIdx).getValues();
        }
    }
    
    /**
     * Get the values of the class attribute.
     * @param attr the index of the attribute value.
     * @return the values of the attribute, an empty list if it is continuous, or null if it does not exist.
     */
    public String getClassValue(int attr)
    {
        if(this.features == null || this.features.isEmpty() || classIdx < 0 || classIdx >= features.size())
        {
            return null;
        }
        else
        {
            return this.features.get(classIdx).getValue(attr);
        }
    }    
}
