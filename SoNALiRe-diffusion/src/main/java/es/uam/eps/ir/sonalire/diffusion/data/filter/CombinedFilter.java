/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.data.filter;

import es.uam.eps.ir.sonalire.diffusion.data.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Combination of several filters, which are applied in a given order.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class CombinedFilter<U extends Serializable, I extends Serializable, F> implements DataFilter<U,I, F>
{
    /**
     * Data filters.
     */
    private final List<DataFilter<U,I, F>> dataFilters;
    
    /**
     * Constructor.
     * @param dataFilters List of data filters, in application order.
     */
    public CombinedFilter(List<DataFilter<U,I, F>> dataFilters)
    {
        this.dataFilters = dataFilters;
    }
    
    @Override
    public Data<U, I, F> filter(Data<U, I, F> fullData)
    {
        Data<U,I, F> filteredData = fullData;
        for(DataFilter<U,I, F> filter : dataFilters)
        {
           filteredData = filter.filter(filteredData);
        }
        return filteredData;
    }
    
}
