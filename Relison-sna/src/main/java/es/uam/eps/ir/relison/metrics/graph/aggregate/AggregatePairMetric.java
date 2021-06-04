/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.relison.metrics.graph.aggregate;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.metrics.GraphMetric;
import es.uam.eps.ir.relison.metrics.PairMetric;

/**
 * Graph metric computed as the aggregation of an pair metric over the pairs of users in the network.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AggregatePairMetric<U> implements GraphMetric<U>
{
    /**
     * Pair metric.
     */
    private final PairMetric<U> pairMetric;

    /**
     * Constructor.
     *
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
