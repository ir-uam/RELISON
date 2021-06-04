/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.utils.datatypes;

import java.util.Objects;

/**
 * Comparable version of Tuple2oo.
 *
 * @param <X> Class of the first element. It has to implement Comparable interface.
 * @param <Y> Class of the second element. It has to implement Comparable interface.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CompTuple2oo<X extends Comparable<X>, Y extends Comparable<Y>> implements Comparable<CompTuple2oo<X, Y>>
{
    /**
     * First object
     */
    private final X first;
    /**
     * Second object
     */
    private final Y second;

    /**
     * Constructor.
     *
     * @param first  First object.
     * @param second Second object.
     */
    public CompTuple2oo(X first, Y second)
    {
        this.first = first;
        this.second = second;
    }

    /**
     * Gets the first object of the pair.
     *
     * @return the first object of the pair.
     */
    public X v1()
    {
        return first;
    }

    /**
     * Gets the second object of the pair.
     *
     * @return the second object of the pair.
     */
    public Y v2()
    {
        return second;
    }

    @Override
    public boolean equals(Object u)
    {
        if (u.getClass().equals(this.getClass()))
        {
            CompTuple2oo<X, Y> pair = (CompTuple2oo<X, Y>) u;
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

    @Override
    public int compareTo(CompTuple2oo<X, Y> t)
    {
        if (this.second.compareTo(t.second) == 0)
        {
            return this.first.compareTo(t.first);
        }
        else
        {
            return -this.second.compareTo(t.second);
        }
    }
}
