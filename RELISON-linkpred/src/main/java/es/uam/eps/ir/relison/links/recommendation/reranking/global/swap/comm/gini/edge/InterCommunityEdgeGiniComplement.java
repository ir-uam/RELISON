/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.comm.gini.edge;

import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.lang.Integer.max;
import static java.lang.Integer.min;

/**
 * Swap reranker that optimizes the Gini index of the distribution of edges between communities.
 * This reranker only considers, for the metric, edges between different communities.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class InterCommunityEdgeGiniComplement<U> extends AbstractCommunityEdgeGiniComplement<U>
{
    /**
     * Constructor.
     * @param lambda        establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff        the maximum number of contacts in the definitive rankings.
     * @param norm          the normalization scheme.
     * @param graph         the original graph.
     * @param communities   the relation between communities and users in the graph.
     * @param outer         true if we want to force links to go outside communities.
     */
    protected InterCommunityEdgeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean outer)
    {
        super(lambda, cutoff, norm, graph, communities, false, outer);

    }

    /**
     * Constructor. By default, this algorithm does not force links to go outside communities.
     * @param lambda        establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff        the maximum number of contacts in the definitive rankings.
     * @param norm          the normalization scheme.
     * @param graph         the original graph.
     * @param communities   the relation between communities and users in the graph.
     */
    public InterCommunityEdgeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities)
    {
        super(lambda, cutoff, norm, graph, communities, false, false);
    }

    @Override
    protected int findIndex(int userComm, int recommComm)
    {
        
        int numComm = this.communities.getNumCommunities();
        if(userComm < 0 || userComm >= numComm || recommComm < 0 || recommComm >= numComm)
        {
            return -1;
        }
        
        if(userComm == recommComm)
        {
            return -1;
        }
        else if(communityGraph.isDirected()) // In case the graph is directed.
        {
            return userComm*(communities.getNumCommunities()-1) + recommComm - (userComm > recommComm ? 0 : 1);
        }
        else // In case the graph is undirected.
        {
            int auxNewIdx = 0;
            for(int i = 0; i < min(userComm, recommComm); ++i)
            {
                auxNewIdx += communities.getNumCommunities() - i - 1;
            }
            auxNewIdx += max(userComm, recommComm) - min(userComm, recommComm) - 1;
            return auxNewIdx;
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
                    if(i!= j)
                        map.put(map.size(), (long) communityGraph.getNumEdges(i, j));
                }
            }
        }
        else
        {
            for(int i = 0; i < numComms; ++i)
            {
                for(int j = i+1; j < numComms; ++j)
                {
                    map.put(map.size(), (long) communityGraph.getNumEdges(i, j));
                }
            }
        }
        return map;
    }

}
