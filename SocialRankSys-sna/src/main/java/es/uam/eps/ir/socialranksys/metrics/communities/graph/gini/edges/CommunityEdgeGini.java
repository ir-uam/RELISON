/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.communities.graph.gini.edges;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityNoAutoloopsGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.metrics.CommunityMetric;
import es.uam.eps.ir.socialranksys.metrics.graph.EdgeGini;
import es.uam.eps.ir.socialranksys.metrics.graph.EdgeGiniMode;

/**
 * Computes the community edge Gini of the graph, i.e. the Gini coefficient for the
 * number of edges between each pair of communities.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public abstract class CommunityEdgeGini<U> implements CommunityMetric<U>
{
    /**
     * EdgeGini execution mode
     * @see EdgeGiniMode
     */
    private final EdgeGiniMode mode;
    
    /**
     * Indicates if autoloops are allowed.
     */
    private final boolean autoloops;
    /**
     * Constructor.
     * @param mode EdgeGini execution mode.
     * @param autoloops true if autoloops are allowed, false if they are not.
     */
    public CommunityEdgeGini(EdgeGiniMode mode, boolean autoloops)
    {
        this.mode = mode;
        this.autoloops = autoloops;
    }

    @Override
    public double compute(Graph<U> graph, Communities<U> comm)
    {
        CommunityGraphGenerator<U> cgg = autoloops ? new CompleteCommunityGraphGenerator<>() : new CompleteCommunityNoAutoloopsGraphGenerator<>();
        MultiGraph<Integer> commGraph = cgg.generate(graph, comm);
        
        EdgeGini<Integer> pairGini = new EdgeGini<>(mode);
        return pairGini.compute(commGraph);
    }
}
