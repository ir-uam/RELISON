/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data.letor;

/**
 * Types of the features.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 */
public enum FeatureType 
{
    CONTINUOUS, NOMINAL;
    
    /**
     * Converts a String into a feature type.
     * @param name the string to convert.
     * @return the type if it is correct, null otherwise.
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
     * Obtains the String value of a feature type.
     * @param featType the feature type value.
     * @return the string if everything is OK, null otherwise.
     */
    public static String toString(FeatureType featType)
    {
        return switch (featType)
        {
            case NOMINAL -> "nominal";
            case CONTINUOUS -> "continuous";
        };
    }
}
