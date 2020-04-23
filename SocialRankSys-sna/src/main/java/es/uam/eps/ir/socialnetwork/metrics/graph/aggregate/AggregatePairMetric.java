/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialnetwork.metrics.graph.aggregate;

import es.uam.eps.ir.socialnetwork.metrics.GraphMetric;
import es.uam.eps.ir.socialnetwork.metrics.PairMetric;
import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Aggregate pair metric
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @param <U> type of the users.
 */
public class AggregatePairMetric<U> implements GraphMetric<U>
{
    /**
     * Pair metric
     */
    private final PairMetric<U> pairMetric;
    
    /**
     * Constructor.
     * @param pairMetric Pair metric.
     */
    public AggregatePairMetric(PairMetric<U> pairMetric)
    {
        this.pairMetric = pairMetric;
    }
    
    @Override
    public double compute(Graph<U> graph)
    {
        return pairMetric.averageValue(graph);
    }

}
