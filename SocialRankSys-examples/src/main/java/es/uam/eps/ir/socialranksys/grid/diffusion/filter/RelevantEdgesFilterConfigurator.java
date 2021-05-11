/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.filter;

import es.uam.eps.ir.socialranksys.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.socialranksys.diffusion.data.filter.RelevantEdgesFilter;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.grid.Parameters;

import java.io.Serializable;

/**
 * Configures a filter which, if a recommendation has been added to the original graph, keeps only those relevant
 * recommended edges.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the information pieces.
 * @param <F> Type of the features of the users / information pieces.
 *
 * @see RelevantEdgesFilter
 */
public class RelevantEdgesFilterConfigurator<U extends Serializable,I extends Serializable, F> implements FilterConfigurator<U,I, F>
{
    /**
     * The test graph, for checking whether the links are relevant or not.
     */
    private final Graph<U> test;

    /**
     * Constructor.
     * @param test the test graph, for checking whether the links are relevant (appear in the network) or not.
     */
    public RelevantEdgesFilterConfigurator(Graph<U> test)
    {
        this.test = test;
    }

    @Override
    public DataFilter<U, I, F> getFilter(Parameters fgs)
    {
        return new RelevantEdgesFilter<>(test);
    }
    
}
