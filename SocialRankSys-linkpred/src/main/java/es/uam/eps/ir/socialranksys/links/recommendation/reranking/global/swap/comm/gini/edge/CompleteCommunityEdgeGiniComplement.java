/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm.gini.edge;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm.CommunityReranker;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Swap reranker that optimizes the Gini index of the distribution of edges between communities.
 * Both edges between different communities and links inside of communities are considered.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class CompleteCommunityEdgeGiniComplement<U> extends AbstractCommunityEdgeGiniComplement<U>
{
    /**
     * Constructor.
     * @param lambda        establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff        the maximum number of contacts in the definitive rankings.
     * @param norm          the normalization scheme.
     * @param graph         the original graph.
     * @param communities   the relation between communities and users in the graph.
     * @param selfloops     true if selfloops are allowed, false if they are not.
     * @param outer         true if we want to force links to go outside communities.
     */
    protected CompleteCommunityEdgeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean selfloops, boolean outer)
    {
        super(lambda, cutoff, norm, graph, communities, selfloops, outer);
    }

    /**
     * Constructor. By default, this algorithm does not force links to go outside communities.
     * @param lambda        establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff        the maximum number of contacts in the definitive rankings.
     * @param norm          the normalization scheme.
     * @param graph         the original graph.
     * @param communities   the relation between communities and users in the graph.
     * @param selfloops     true if selfloops are allowed, false if they are not.
     */
    public CompleteCommunityEdgeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean selfloops)
    {
        super(lambda, cutoff, norm, graph, communities, selfloops, false);
    }

    @Override
    protected int findIndex(int userComm, int recommComm)
    {
        
        int numComm = this.communities.getNumCommunities();
        if(userComm < 0 || userComm >= numComm || recommComm < 0 || recommComm >= numComm)
        {
            return -1;
        }
        
        if(this.communityGraph.isDirected())
        {
            return userComm*numComm + recommComm;
        }
        else
        {
            int minNew = Math.min(userComm, recommComm);
            int maxNew = Math.max(userComm, recommComm);
            
            return numComm*minNew - minNew*(minNew-1)/2 + (maxNew - minNew);           
        }
    }

    @Override
    protected Map<Integer, Long> computeInitialFrequencies()
    {
        Map<Integer, Long> map = new HashMap<>();
        long numComms = communityGraph.getVertexCount();
        if(communityGraph.isDirected())
        {
            for(int i = 0; i < numComms; ++i)
            {
                for(int j = 0; j < numComms; ++j)
                {
                    map.put(map.size(), (long) communityGraph.getNumEdges(i, j));
                }
            }
        }
        else
        {
            for(int i = 0; i < numComms; ++i)
            {
                for(int j = i; j < numComms; ++j)
                {
                    map.put(map.size(), (long) communityGraph.getNumEdges(i, j));
                }
            }
        }
        return map;
    }
}
