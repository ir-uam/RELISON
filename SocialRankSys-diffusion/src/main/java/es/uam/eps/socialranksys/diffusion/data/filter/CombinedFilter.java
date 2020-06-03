/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.data.filter;

import es.uam.eps.socialranksys.diffusion.data.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Filter that applies several filters.
 * @author Javier Sanz-Cruzado Puig
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
     * @param dataFilters List of datafilters, ordered by application.
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
