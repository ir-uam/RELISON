/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.filter;

import es.uam.eps.ir.sonalire.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.sonalire.diffusion.data.filter.MinimumFrequencyInformationFeatureFilter;
import es.uam.eps.ir.sonalire.grid.Parameters;

import java.io.Serializable;

/**
 * Class for configuring a filter which keeps only the information pieces feature available, at least,
 * in a minimum number of information pieces.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the information pieces.
 * @param <F> Type of the features of the users / information pieces.
 *
 * @see MinimumFrequencyInformationFeatureFilter
 */
public class MinimumFrequencyInformationFeatureFilterConfigurator<U extends Serializable, I extends Serializable, F> implements FilterConfigurator<U,I, F>
{
    /**
     * Identifier for the name of the tag field to consider.
     */
    private final static String TAGNAME = "tagName";
    /**
     * Identifier for the minimum number of pieces which have to use the tag.
     */
    private final static String MINVALUE = "minValue";

    @Override
    public DataFilter<U, I, F> getFilter(Parameters fgr)
    {
        String param = fgr.getStringValue(TAGNAME);
        long minValue = fgr.getLongValue(MINVALUE);
        return new MinimumFrequencyInformationFeatureFilter<>(minValue, param);
    }
    
}
