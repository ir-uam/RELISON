/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.utils.generator;

/**
 * Generates objects of a certain type.
 *
 * @param <V> Type of the objects.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface Generator<V>
{

    /**
     * Generates elements of the corresponding type.
     *
     * @return a new element.
     */
    V generate();

    /**
     * Resets the generator.
     */
    void reset();

    /**
     * Resets the generator at some specific value
     *
     * @param v the value.
     */
    void reset(V v);
}
