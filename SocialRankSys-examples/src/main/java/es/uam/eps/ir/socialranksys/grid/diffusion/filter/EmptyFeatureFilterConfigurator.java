/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.filter;


import es.uam.eps.ir.socialranksys.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.socialranksys.diffusion.data.filter.EmptyFeatureFilter;

import java.io.Serializable;

/**
 * Class for configuring a Basic filter.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * @param <I> Type of the items
 * @param <P> Type of the parameters
 */
public class EmptyFeatureFilterConfigurator<U extends Serializable,I extends Serializable,P> implements FilterConfigurator<U,I,P>
{
    
    private final P defaultValue;
    
    public EmptyFeatureFilterConfigurator(P defaultValue)
    {
        this.defaultValue = defaultValue;
    }
    @Override
    public DataFilter<U,I,P> getFilter(FilterParamReader fgs)
    {
        return new EmptyFeatureFilter<>(defaultValue);
    }
}
