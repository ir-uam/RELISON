/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.utils.datatypes;

import java.util.Objects;

/**
 * Class that represents a pair of objects of different type.
 *
 * @param <U> Type of first object.
 * @param <I> Type of second object.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Tuple2oo<U, I>
{
    /**
     * First object
     */
    private final U first;
    /**
     * Second object
     */
    private final I second;

    /**
     * Constructor.
     *
     * @param first  First object.
     * @param second Second object.
     */
    public Tuple2oo(U first, I second)
    {
        this.first = first;
        this.second = second;
    }

    /**
     * Gets the first object of the pair.
     *
     * @return the first object of the pair.
     */
    public U v1()
    {
        return first;
    }

    /**
     * Gets the second object of the pair.
     *
     * @return the second object of the pair.
     */
    public I v2()
    {
        return second;
    }

    @Override
    public boolean equals(Object u)
    {
        if (u.getClass().equals(this.getClass()))
        {
            Tuple2oo<?, ?> pair = (Tuple2oo<?, ?>) u;
            return first.equals(pair.first) && second.equals(pair.second);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.first);
        hash = 83 * hash + Objects.hashCode(this.second);
        return hash;
    }
}


