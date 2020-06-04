/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.filter;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.socialranksys.diffusion.data.filter.DataFilter;
import es.uam.eps.socialranksys.diffusion.data.filter.RelevantEdgesFilter;

import java.io.Serializable;

/**
 *
 * @author Javier
 * @param <U>
 * @param <I>
 * @param <P>
 */
public class RelevantEdgesFilterConfigurator<U extends Serializable,I extends Serializable,P> implements FilterConfigurator<U,I,P>
{

    private final Graph<U> test;
    public RelevantEdgesFilterConfigurator(Graph<U> test)
    {
        this.test = test;
    }
    @Override
    public DataFilter<U, I, P> getFilter(FilterParamReader fgs)
    {
        return new RelevantEdgesFilter<>(test);
    }
    
}
