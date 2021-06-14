/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.utils.datatypes;

/**
 * Tuple that contains a long as second type.
 *
 * @param <U> Type of the identifier.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Tuple2ol<U> extends Tuple2oo<U, Long>
{
    /**
     * Constructor.
     * @param first the object representing the first element of the tuple.
     * @param second the long representing the second element of the tuple.
     */
    public Tuple2ol(U first, long second)
    {
        super(first, second);
    }

}
