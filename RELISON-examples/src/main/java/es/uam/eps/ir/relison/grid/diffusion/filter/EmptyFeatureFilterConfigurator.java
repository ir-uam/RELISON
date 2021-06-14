/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.filter;

import es.uam.eps.ir.relison.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.relison.diffusion.data.filter.EmptyFeatureFilter;
import es.uam.eps.ir.relison.grid.Parameters;

import java.io.Serializable;

/**
 * Class for configuring a filter that adds, for each information piece without any feature, an empty feature.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the information pieces.
 * @param <F> type of the features of the users / information pieces.
 *
 * @see EmptyFeatureFilter
 */
public class EmptyFeatureFilterConfigurator<U extends Serializable,I extends Serializable, F> implements FilterConfigurator<U,I, F>
{
    /**
     * The default value of the features.
     */
    private final F defaultValue;

    /**
     * Constructor.
     * @param defaultValue the default value for the features.
     */
    public EmptyFeatureFilterConfigurator(F defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    @Override
    public DataFilter<U,I, F> getFilter(Parameters fgs)
    {
        return new EmptyFeatureFilter<>(defaultValue);
    }
}
