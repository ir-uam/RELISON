/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialnetwork.metrics.communities.graph;

import es.uam.eps.ir.socialnetwork.metrics.CommunityMetric;
import es.uam.eps.ir.socialnetwork.metrics.IndividualCommunityMetric;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Aggregate individual community metric.
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @param <U> Type of the users.
 */
public class AggregateIndividualCommMetric<U> implements CommunityMetric<U>
{
    /**
     * Individual community metric
     */
    private final IndividualCommunityMetric<U> indivMetric;
    
    /**
     * Constructor. 
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
