/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm.gini.edge;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.lang.Integer.max;
import static java.lang.Integer.min;

/**
 * Swap reranker that optimizes the Gini index of the distribution of edges between communities.
 * Both edges between different communities and links inside of communities are considered,
 * but links between communities are all stored in a single group when computing the Gini
 * coefficient.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class SemiCompleteCommunityEdgeGiniComplement<U> extends AbstractCommunityEdgeGiniComplement<U>
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
    protected SemiCompleteCommunityEdgeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean selfloops, boolean outer)
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
    public SemiCompleteCommunityEdgeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean selfloops)
    {
        super(lambda, cutoff, norm, graph, communities, selfloops, false);
    }

    @Override
    protected int findIndex(int userComm, int recommComm)
    {
        int numComm = this.communities.getNumCommunities();
        int numElems = (this.graph.isDirected()) ? numComm*(numComm-1) + 1 : numComm*(numComm-1)/2 + 1;

        if(userComm < 0 || userComm >= numComm || recommComm < 0 || recommComm >= numComm)
        {
            return -1;
        }
        
        if(userComm == recommComm)
        {
            return numElems - 1;
        }
        else if(this.graph.isDirected())
        {
            return userComm*numComm + recommComm - userComm - ((userComm < recommComm)? 1 : 0);
        }
        else
        {            
            int auxNewIdx = 0;

            for(int i = 0; i < min(userComm, recommComm); ++i)
            {
                auxNewIdx += numComm - i - 1;
            }
            auxNewIdx += max(userComm, recommComm) - min(userComm, recommComm) - 1;
            return auxNewIdx;
        }
    }

    @Override
    protected Map<Integer, Long> computeInitialFrequencies()
    {
        Map<Integer, Long> map = new HashMap<>();
        long sum = 0L;
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

                sum += communityGraph.getNumEdges(i,i);
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
                sum += communityGraph.getNumEdges(i,i);
            }
        }
        map.put(map.size(), sum);
        return map;
    }
}
