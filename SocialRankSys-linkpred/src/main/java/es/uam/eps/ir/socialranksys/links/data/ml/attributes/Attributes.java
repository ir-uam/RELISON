/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.ml.attributes;

import java.util.List;

/**
 * Set of attributes of a machine learning approach.
 * @author Javier Sanz-Cruzado Puig
 * 
 */
public class Attributes
{
    /**
     * Attribute list.
     */
    private final List<Attribute> attributes;
    /**
     * The index of the class attribute in the list.
     */
    private final int classIdx;
    
    /**
     * Constructor.
     * @param attrs Attribute list.
     * @param classIdx The index of the class attribute in the list.
     */
    public Attributes(List<Attribute> attrs, int classIdx)
    {
        this.attributes = attrs;
        this.classIdx = classIdx;
    }
    /**
     * Get the name of the attribute
     * @param idx index of the attribute
     * @return the name of the attribute, null if it does not exist.
     */
    public String getAttributeName(int idx)
    {
        if(this.attributes == null || this.attributes.isEmpty() || idx < 0 || idx >= attributes.size())
        {
            return null;
        }
        else
        {
            return this.attributes.get(idx).getName();
        }
    }
    
    /**
     * Get the attribute type.
     * @param idx index of the attribute.
     * @return the type of the attribute, null if it does not exist.
     */
    public AttrType getAttributeType(int idx)
    {
        if(this.attributes == null || this.attributes.isEmpty() || idx < 0 || idx >= attributes.size())
        {
            return null;
        }
        else
        {
            return this.attributes.get(idx).getType();
        }
    }
    
    /**
     * Get the values of an attribute.
     * @param idx index of the attribute.
     * @return the values of the attribute, an empty list if it is continuous, or null if it does not exist.
     */
    public List<String> getAttributeValues(int idx)
    {
        if(this.attributes == null || this.attributes.isEmpty() || idx < 0 || idx >= attributes.size())
        {
            return null;
        }
        else
        {
            return this.attributes.get(idx).getValues();
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
        if(this.attributes == null || this.attributes.isEmpty() || idx < 0 || idx >= attributes.size())
        {
            return null;
        }
        else
        {
            return this.attributes.get(idx).getValue(attr);
        }
    }
    
    /**
     * Get the values of the class attribute.
     * @return the values of the attribute, an empty list if it is continuous, or null if it does not exist.
     */
    public List<String> getClassValues()
    {
        if(this.attributes == null || this.attributes.isEmpty() || classIdx < 0 || classIdx >= attributes.size())
        {
            return null;
        }
        else
        {
            return this.attributes.get(classIdx).getValues();
        }
    }
    
    /**
     * Get the values of the class attribute.
     * @param attr the index of the attribute value.
     * @return the values of the attribute, an empty list if it is continuous, or null if it does not exist.
     */
    public String getClassValue(int attr)
    {
        if(this.attributes == null || this.attributes.isEmpty() || classIdx < 0 || classIdx >= attributes.size())
        {
            return null;
        }
        else
        {
            return this.attributes.get(classIdx).getValue(attr);
        }
    }    
}
