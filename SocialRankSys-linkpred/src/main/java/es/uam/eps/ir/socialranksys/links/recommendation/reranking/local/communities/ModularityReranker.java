/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.local.communities;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.Map;

/**
 * Reranks a recommendation by reducing the value of the Modularity of the different 
 * different communities in a community graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class ModularityReranker<U> extends CommunityReranker<U>
{
    
    /**
     * Constructor.
     * @param lambda Establishes the trait-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     */
    public ModularityReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, Communities<U> communities)
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
         * @param recommendation The recommendations for the user.
         * @param maxLength The maximum number of items to recommend.
         * @param graph The user graph.
         * @param communityGraph The community graph.
         * @param communities The relation between users and communities.
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
        protected double value(Tuple2od<U> iv)
        {
            if(lambda == 1.0)
            {
                return norm(iv.v2, relStats) + norm(novMap.getDouble(iv.v1), novStats);
            }
            else
            {
                return super.value(iv);
            }
        }

        @Override
        protected void update(Tuple2od<U> tpld)
        {
            
        }
        
    }
}
