/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.utils.datatypes;

/**
 * Tuple that contains a long as second type.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the identifier.
 */
public class Tuple2ol<U> extends Tuple2oo<U,Long>
{

    public Tuple2ol(U first, long second) 
    {
        super(first, second);
    }
    
}
