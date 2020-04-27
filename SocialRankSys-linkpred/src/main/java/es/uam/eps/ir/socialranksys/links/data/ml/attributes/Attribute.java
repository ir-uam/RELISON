/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.ml.attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Attribute values.
 * @author Javier Sanz-Cruzado Puig
 */
public class Attribute
{
    /**
     * Name of the attribute
     */
    private final String name;
    /**
     * Type of the attribute
     */
    private final AttrType type;
    /**
     * Values of the nominal attribute
     */
    private final List<String> values;
    
    /**
     * Constructor
     * @param name Name of the attribute.
     * @param type Type of the attribute.
     */
    public Attribute(String name, AttrType type)
    {
        this.name = name;
        this.type = type;
        this.values = new ArrayList<>();
    }
    
    /**
     * In case the attribute is not continuous, adds a new possible value
     * @param value The value
     * @return true if the value is correctly added, false if it is not.
     */
    public boolean addValue(String value)
    {
        if(!type.equals(AttrType.CONTINUOUS) && values.contains(value))
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
    public AttrType getType() 
    {
        return type;
    }

    /**
     * Get the attribute values
     * @return  the attribute values if it is not continuous, an empty list if not.
     */
    public List<String> getValues() 
    {
        return values;
    }
    
    /**
     * Get an individual attribute value
     * @param attr The index of the value.
     * @return  the attribute value if it exists, an empty list if not.
     */
    public String getValue(int attr) 
    {
        if(this.type.equals(AttrType.CONTINUOUS) || attr < 0 || attr > this.values.size())
            return null;
        return values.get(attr);
    }
    
    
}
