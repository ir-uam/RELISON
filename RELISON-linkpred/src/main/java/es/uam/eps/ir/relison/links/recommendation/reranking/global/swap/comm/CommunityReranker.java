/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.comm;

import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.graph.CommunityGraphGenerator;
import es.uam.eps.ir.relison.sna.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.relison.sna.community.graph.CompleteCommunityNoSelfLoopsGraphGenerator;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.GraphSwapReranker;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;

import java.util.function.Supplier;

/**
 * Swap reranker for modifying it according to the community metrics of the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public abstract class CommunityReranker<U> extends GraphSwapReranker<U>
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
     * Indicates if self-loops are allowed or not
     */
    protected boolean selfloops;
    
    /**
     * Constructor
     * @param lambda        a trade-off between the original score and the metric value
     * @param cutoff        the number of items to rerank
     * @param norm          the normalization scheme.
     * @param graph         the original network.
     * @param communities   relation between communities and users.
     * @param selfloops     true if selfloops are allowed, false if they are not.
     */
    public CommunityReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean selfloops)
    {
        super(lambda, cutoff, norm, graph);
        this.communities = communities;
        this.selfloops = selfloops;
    }
    
    @Override
    protected void computeGlobalValue()
    {
        CommunityGraphGenerator<U> cgg = selfloops ? new CompleteCommunityGraphGenerator<>() : new CompleteCommunityNoSelfLoopsGraphGenerator<>();
        this.communityGraph = cgg.generate(this.graph, this.communities);       
    }
}
