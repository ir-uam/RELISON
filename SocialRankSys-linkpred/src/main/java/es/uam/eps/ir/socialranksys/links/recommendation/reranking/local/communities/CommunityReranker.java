/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.local.communities;


import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.ranksys.novdiv.reranking.LambdaReranker;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.InterCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;

/**
 * Reranker that uses community metrics of the user graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public abstract class CommunityReranker<U> extends LambdaReranker<U,U>
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
     * true if the original score and the metric value require normalization
     */
    protected boolean norm;
    /**
     * The user graph.
     */
    protected final Graph<U> graph;
    
    /**
     * Constructor
     * @param lambda A trait-off between the original score and the metric value
     * @param cutoff The number of items to rerank
     * @param norm true if the original score and the metric value require normalization
     * @param graph The user graph
     * @param communities A relation between communities and users.
     */
    public CommunityReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, Communities<U> communities) 
    {
        super(lambda, cutoff, norm);
        this.communities = communities;
        this.norm = norm;
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
         * @param recommendation the recommendation to be reranked.
         * @param maxLength the maximum length of the definitive ranking.
         * @param communityGraph the community graph.
         * @param communities the number of communities.
         */
        public CommunityMetricUserReranker(Recommendation<U, U> recommendation, int maxLength, MultiGraph<Integer> communityGraph, Communities<U> communities) {
            super(recommendation, maxLength);
            this.communityGraph = communityGraph;
            this.communities = communities;
        }
        
        @Override
        protected double norm(double score, Stats stats)
        {
            if(norm)
            {
                return (score - stats.getMin())/(stats.getMax()-stats.getMin());
            }
            else
                return score;
        }
        
        
    }
    
}
