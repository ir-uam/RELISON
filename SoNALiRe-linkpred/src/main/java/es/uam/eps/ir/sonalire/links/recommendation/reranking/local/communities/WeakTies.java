/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.local.communities;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.multigraph.MultiGraph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Implementation of a reranker which promotes the number of weak ties (links between communities).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class WeakTies<U> extends InterCommunityReranker<U>
{
    /**
     * Constructor.
     * @param lambda        param that establishes a balance between the score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization function.
     * @param graph         the graph.
     * @param communities   the relation between users and communities.
     */
    public WeakTies(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities)
    {
        super(lambda, cutoff, norm, graph, communities);
    }

    @Override
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> r, int i)
    {
        Cloner cloner = new Cloner();
        return new ModularityUserReranker(r, i, this.graph, cloner.deepClone(this.communityGraph), this.communities);
    }
    
    /**
     * Class that reranks an individual recommendation using modularity.
     */
    protected class ModularityUserReranker extends CommunityMetricUserReranker
    {
        /**
         * The number of edges of the user graph.
         */
        protected long numEdges;
        /**
         * The relation between the users and their in-degree.
         */
        protected Map<U, Integer> inDegree;
        /**
         * The relation between the users and their out-degree.
         */
        protected Map<U, Integer> outDegree;
        /**
         * K(G,C) = \sum_i,j |\Gamma_out(i)||\Gamma_in(j)|\delta(c_i, c_j)
         */
        protected double sum;
        
        /**
         * Constructor.
         * @param recommendation    the recommendations for the user.
         * @param maxLength         the maximum number of items to recommend.
         * @param graph             the user graph.
         * @param communityGraph    the community graph.
         * @param communities       the relation between users and communities.
         */
        public ModularityUserReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, MultiGraph<Integer> communityGraph, Communities<U> communities)
        {
            super(recommendation, maxLength, communityGraph, communities);
        }

        @Override
        protected double nov(Tuple2od<U> tpld)
        {
            U user = recommendation.getUser();
            U recomm = tpld.v1;
            int userComm = communities.getCommunity(user);
            int recommComm = communities.getCommunity(recomm);
            return ((userComm == recommComm)? 0.0 : 1.0);
        }   

        @Override
        protected void update(Tuple2od<U> tpld)
        {
            
        }
        
    }
}
