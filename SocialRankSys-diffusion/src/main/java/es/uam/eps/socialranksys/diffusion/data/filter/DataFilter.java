/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.data.filter;

import es.uam.eps.socialranksys.diffusion.data.Data;

import java.io.Serializable;

/**
 * Interface for filtering unnecessary data for simulations.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 * @param <I> type of the items.
 * @param <P> type of hte parameters.
 */
public interface DataFilter<U extends Serializable,I extends Serializable,P>
{
    /**
     * Filters the data
     * @param fullData Data to be filtered.
     * @return The filtered data.
     */
    Data<U,I,P> filter(Data<U, I, P> fullData);
    
    
}
