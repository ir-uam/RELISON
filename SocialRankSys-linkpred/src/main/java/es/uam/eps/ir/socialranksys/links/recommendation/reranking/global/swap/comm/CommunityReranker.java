/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityNoAutoloopsGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.SwapRerankerGraph;

/**
 * Reranker that uses community metrics of the user graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public abstract class CommunityReranker<U> extends SwapRerankerGraph<U>
{
    /**
     * Communities.
     */
    protected final Communities<U> communities;
    /**
     * Community graph.
     */
    protected MultiGraph<Integer> communityGraph;
    /**
     * Indicates if autoloops are allowed or not
     */
    protected boolean autoloops;
    
    /**
     * Constructor
     * @param lambda A trait-off between the original score and the metric value
     * @param cutoff The number of items to rerank
     * @param norm true if the original score and the metric value require optimization
     * @param rank true if the normalization is by ranking position, false if it is by score
     * @param graph The user graph
     * @param communities A relation between communities and users.
     * @param autoloops true if autoloops are allowed, false if they are not.
     */
    public CommunityReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> communities, boolean autoloops)
    {
        super(lambda, cutoff, norm, rank, graph);
        this.communities = communities;
        this.autoloops = autoloops;
    }
    
    @Override
    protected void computeGlobalValue()
    {
        CommunityGraphGenerator<U> cgg = autoloops ? new CompleteCommunityGraphGenerator<>() : new CompleteCommunityNoAutoloopsGraphGenerator<>();
        this.communityGraph = cgg.generate(this.graph, this.communities);       
    }
}
