/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.data.filter;

import es.uam.eps.ir.relison.diffusion.data.Data;

import java.io.Serializable;

/**
 * Interface for filtering unnecessary data for simulations.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public interface DataFilter<U extends Serializable,I extends Serializable, F>
{
    /**
     * Filters the data.
     * @param fullData Data to be filtered.
     * @return The filtered data.
     */
    Data<U,I, F> filter(Data<U, I, F> fullData);
    
    
}
