/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.ml.features;

/**
 * Enumeration for the different types that a feature can take.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public enum FeatureType
{
    /**
     * Indicates that the feature can only take some fixed values.
     */
    NOMINAL,
    /**
     * Indicates that the feature can take any real value (or, at least, a continouos interval)
     */
    CONTINUOUS,
    /**
     * Indicates the class of the machine learning pattern.
     */
    CLASS;

    /**
     * Given a string, obtains the value of the feature type
     * @param name the string containing the possible value.
     * @return the feature type.
     */
    public static FeatureType getValue(String name)
    {
        if(name.equalsIgnoreCase("nominal"))
        {
           return NOMINAL; 
        }
        else if(name.equalsIgnoreCase("class"))
        {
            return CLASS;
        }
        else
        {
            return CONTINUOUS;
        }
    }
}
