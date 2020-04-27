/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor;

/**
 * Types of the features.
 * @author Javier Sanz-Cruzado Puig
 */
public enum FeatureType 
{
    CONTINUOUS, NOMINAL;
    
    /**
     * Converts a String into a feature type.
     * @param name the string to convert.
     * @return the type if it is correct, null otherwise
     */
    public static FeatureType getValue(String name)
    {
        if(name.equalsIgnoreCase("nominal"))
        {
           return NOMINAL; 
        }
        else if(name.equalsIgnoreCase("continuous"))
        {
            return CONTINUOUS;
        }
        
        return null;
    }
    
    /**
     * Obtains the String value of a feature type
     * @param featType the feature type value.
     * @return the string if everything is OK, null otherwise.
     */
    public static String toString(FeatureType featType)
    {
        switch(featType)
        {
            case NOMINAL: return "nominal";
            case CONTINUOUS: return "continuous";
            default: return null;
        }
    }
}
