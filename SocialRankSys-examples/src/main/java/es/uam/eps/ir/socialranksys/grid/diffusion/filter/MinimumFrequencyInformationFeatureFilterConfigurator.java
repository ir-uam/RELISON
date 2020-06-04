/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.filter;

import es.uam.eps.socialranksys.diffusion.data.filter.DataFilter;
import es.uam.eps.socialranksys.diffusion.data.filter.MinimumFrequencyInformationFeatureFilter;

import java.io.Serializable;

/**
 *
 * @author Javier
 */
public class MinimumFrequencyInformationFeatureFilterConfigurator<U extends Serializable, I extends Serializable, P> implements FilterConfigurator<U,I,P>
{
    /**
     * Identifier for the name of the tag field to consider.
     */
    private final static String TAGNAME = "tagName";
    /**
     * Identifier for the name of the tag field to consider.
     */
    private final static String MINVALUE = "minValue";
    @Override
    public DataFilter<U, I, P> getFilter(FilterParamReader fgr)
    {
        String param = fgr.getParams().getStringValue(TAGNAME);
        long minValue = fgr.getParams().getLongValue(MINVALUE);
        return new MinimumFrequencyInformationFeatureFilter<>(minValue, param);
    }
    
}
