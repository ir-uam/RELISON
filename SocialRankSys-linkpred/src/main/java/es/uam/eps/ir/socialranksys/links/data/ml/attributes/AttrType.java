/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.ml.attributes;

/**
 * Different possible types for attributes.
 * @author Javier Sanz-Cruzado Puig
 */
public enum AttrType 
{
    NOMINAL, CONTINUOUS, CLASS;

    public static AttrType getValue(String name) 
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
