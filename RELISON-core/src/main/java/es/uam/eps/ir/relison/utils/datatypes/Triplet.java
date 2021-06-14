/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.utils.datatypes;

/**
 * Element that stores three different values.
 *
 * @param <X> Type of the first element.
 * @param <Y> Type of the second element.
 * @param <Z> Type of the third element.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Triplet<X, Y, Z>
{
    /**
     * First element.
     */
    private final X v1;
    /**
     * Second element.
     */
    private final Y v2;
    /**
     * Third element.
     */
    private final Z v3;

    /**
     * Constructor
     *
     * @param v1 first element.
     * @param v2 second element.
     * @param v3 third element.
     */
    public Triplet(X v1, Y v2, Z v3)
    {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    /**
     * Gets the first element.
     *
     * @return the first element.
     */
    public X v1()
    {
        return v1;
    }

    /**
     * Gets the second element.
     *
     * @return the second element.
     */
    public Y v2()
    {
        return v2;
    }

    /**
     * Gets the third element.
     *
     * @return the third element.
     */
    public Z v3()
    {
        return v3;
    }


}
