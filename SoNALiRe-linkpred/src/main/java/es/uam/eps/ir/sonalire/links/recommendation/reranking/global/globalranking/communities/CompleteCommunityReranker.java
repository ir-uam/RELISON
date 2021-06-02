/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.global.globalranking.communities;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.community.graph.CommunityGraphGenerator;
import es.uam.eps.ir.sonalire.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.sonalire.community.graph.CompleteCommunityNoSelfLoopsGraphGenerator;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.multigraph.MultiGraph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.global.globalranking.GlobalRankingLambdaReranker;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;

import java.util.function.Supplier;

/**
 * Reranker that uses community metrics of the user graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public abstract class CompleteCommunityReranker<U> extends GlobalRankingLambdaReranker<U,U>
{
    /**
     * Communities of the graph.
     */
    protected final Communities<U> communities;
    /**
     * The community graph.
     */
    protected final MultiGraph<Integer> communityGraph;
    /**
     * The original graph.
     */
    protected final Graph<U> graph;

    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param communities   the relation between users and communities.
     * @param selfloops     true if self-loops are allowed, false otherwise.
     */
    public CompleteCommunityReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean selfloops)
    {
        super(lambda, cutoff, norm);
        this.communities = communities;
        this.graph = graph;
        CommunityGraphGenerator<U> cgg = selfloops ? new CompleteCommunityGraphGenerator<>() : new CompleteCommunityNoSelfLoopsGraphGenerator<>();
        communityGraph = cgg.generate(graph, communities);
    }
    
    

    
}
