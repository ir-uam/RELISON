/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.globalranking.communities;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.InterCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.globalranking.GlobalRankingLambdaReranker;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;

import java.util.function.Supplier;

/**
 * Global reranker for computing metrics that only use links between pairs of communities.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public abstract class InterCommunityReranker<U> extends GlobalRankingLambdaReranker<U,U>
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
     */
    public InterCommunityReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities)
    {
        super(lambda, cutoff, norm);
        this.communities = communities;
        this.graph = graph;
        CommunityGraphGenerator<U> cgg = new InterCommunityGraphGenerator<>();
        
        communityGraph = cgg.generate(graph, communities);
    }
}
