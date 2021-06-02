/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.sonalire.metrics.communities.graph;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.metrics.CommunityMetric;
import es.uam.eps.ir.sonalire.metrics.IndividualCommunityMetric;

/**
 * Aggregate individual community metric.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AggregateIndividualCommMetric<U> implements CommunityMetric<U>
{
    /**
     * Individual community metric.
     */
    private final IndividualCommunityMetric<U> indivMetric;

    /**
     * Constructor.
     *
     * @param indivMetric the individual community metric.
     */
    public AggregateIndividualCommMetric(IndividualCommunityMetric<U> indivMetric)
    {
        this.indivMetric = indivMetric;
    }

    @Override
    public double compute(Graph<U> graph, Communities<U> comm)
    {
        return this.indivMetric.averageValue(graph, comm);
    }

}
