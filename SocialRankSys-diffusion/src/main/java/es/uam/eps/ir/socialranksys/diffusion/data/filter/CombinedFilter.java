/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.data.filter;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Combination of several filters, which are applied in a given order.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users
 * @param <I> Type of the information pieces
 * @param <P> Type of the parameters.
 */
public class CombinedFilter<U extends Serializable, I extends Serializable,P> implements DataFilter<U,I,P> 
{
    /**
     * Data filters.
     */
    private final List<DataFilter<U,I,P>> dataFilters;
    
    /**
     * Constructor.
     * @param dataFilters List of data filters, in application order.
     */
    public CombinedFilter(List<DataFilter<U,I,P>> dataFilters)
    {
        this.dataFilters = dataFilters;
    }
    
    @Override
    public Data<U, I, P> filter(Data<U, I, P> fullData)
    {
        Data<U,I,P> filteredData = fullData;
        for(DataFilter<U,I,P> filter : dataFilters)
        {
           filteredData = filter.filter(filteredData);
        }
        return filteredData;
    }
    
}
