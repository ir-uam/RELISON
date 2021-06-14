/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.local.communities;


import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.graph.InterCommunityGraphGenerator;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.links.recommendation.reranking.local.LambdaReranker;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;

import java.util.function.Supplier;

/**
 * Reranker that uses community metrics of the user graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public abstract class InterCommunityReranker<U> extends LambdaReranker<U,U>
{
    /**
     * The set of communities of the graph.
     */
    protected final Communities<U> communities;
    /**
     * The community graph.
     */
    protected final MultiGraph<Integer> communityGraph;
    /**
     * The user graph.
     */
    protected final Graph<U> graph;

    /**
     * Constructor.
     * @param lambda        param that establishes a balance between the score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization function.
     * @param graph         the graph.
     * @param communities   the relation between users and communities.
     */
    public InterCommunityReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities)
    {
        super(lambda, cutoff, norm);
        this.communities = communities;
        this.graph = graph;
        InterCommunityGraphGenerator<U> cgg = new InterCommunityGraphGenerator<>();
        
        communityGraph = cgg.generate(graph, communities);
        
    }

    /**
     * Class that reranks an individual recommendation using community metrics.
     */
    protected abstract class CommunityMetricUserReranker extends LambdaUserReranker
    {
        /**
         * The community graph
         */
        protected final MultiGraph<Integer> communityGraph;
        /**
         * The communities.
         */
        protected final Communities<U> communities;
        
        /**
         * Constructor
         * @param recommendation    the recommendation to be reranked.
         * @param maxLength         the maximum length of the definitive ranking.
         * @param communityGraph    the community graph.
         * @param communities       the number of communities.
         */
        public CommunityMetricUserReranker(Recommendation<U, U> recommendation, int maxLength, MultiGraph<Integer> communityGraph, Communities<U> communities)
        {
            super(recommendation, maxLength);
            this.communityGraph = communityGraph;
            this.communities = communities;
        }
    }
    
}
