/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.data;

import java.util.Objects;

/**
 * Class for the parameters we want to measure at evaluation. By now, although its values can be 
 * variated, they are treated as nominal values.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <F> parameter for the information.
 */
public class Feature<F> implements Cloneable
{
    /**
     * Value of the parameter.
     */
    private final F value;
    
    /**
     * Constructor.
     * @param value Value of the parameter.
     */
    public Feature(F value)
    {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if(obj.getClass().equals(this.getClass()))
        {
            Feature<F> instance = (Feature<F>) obj;
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
    
    @Override
    public Feature<F> clone() throws CloneNotSupportedException
    {
        return (Feature<F>) super.clone();
    }
    
    /**
     * Obtains the value of the parameter.
     * @return the value.
     */
    public F getValue()
    {
        return value;
    }
}
