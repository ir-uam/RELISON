/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.data;

import java.util.Objects;

/**
 * Class for the parameters we want to measure at evaluation. By now, although its values can be 
 * variated, they are treated as nominal values.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <P> parameter for the information.
 */
public class Feature<P> implements Cloneable
{
    /**
     * Value of the parameter
     */
    private final P value;
    
    /**
     * Constructor.
     * @param value Value of the parameter
     */
    public Feature(P value)
    {
        this.value = value;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj)
    {
        if(obj.getClass().equals(this.getClass()))
        {
            Feature<P> instance = (Feature<P>) obj;
            return instance.value.equals(this.value);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.value);
        return hash;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Feature<P> clone() throws CloneNotSupportedException
    {
        return (Feature<P>) super.clone();
    }
    
    /**
     * Obtains the value of the parameter
     * @return the value.
     */
    public P getValue()
    {
        return value;
    }
}
