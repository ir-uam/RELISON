/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.global.local.communities;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.sonalire.utils.indexes.GiniIndex;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Implementation of a reranking strategy for balancing the distribution of edges between pairs
 * of communities. It only considers links between communities.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class InterCommunityEdgeGiniComplement<U> extends InterCommunityReranker<U>
{
    /**
     * Number of edges between communities
     */
    private final List<Double> matrix;
    /**
     * Total number of edges between communities.
     */
    private double sum;
    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param communities   the relation between users and communities.
     */
    public InterCommunityEdgeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities)
    {
        super(lambda, cutoff, norm, graph, communities);
        
        matrix = new ArrayList<>();
        long vertexcount = communityGraph.getVertexCount();
        
        if(communityGraph.isDirected())
        {
            for(int i = 0; i < vertexcount; ++i)
            {
                for(int j = 0; j < vertexcount; ++j)
                {
                    if(i != j )
                        matrix.add(communityGraph.getNumEdges(i, j)+0.0);
                }
            }
        }
        else
        {
            for(int i = 0; i < vertexcount; ++i)
                for(int j = 0; j < i; ++j)
                    matrix.add(communityGraph.getNumEdges(i, j)+0.0);
        }

        sum = communityGraph.getEdgeCount();
    }

    @Override
    protected double nov(U u, Tuple2od<U> tpld)
    {
        U recomm = tpld.v1;

        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);

        GiniIndex gini = new GiniIndex();
        if(communityGraph.isDirected())
        {
            int idx = userComm*communities.getNumCommunities() + recommComm - (userComm + 1);
            DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> (index == idx ? (matrix.get(index) + 1) : matrix.get(index)));
            double giniValue = 1.0 - gini.compute(stream.boxed(), true, communities.getNumCommunities()*(communities.getNumCommunities()-1), this.sum+1);
            return 1.0-giniValue;
        }
        else
        {
            int idx = Math.min(userComm, recommComm)*communities.getNumCommunities() + Math.max(userComm, recommComm) - (Math.min(userComm,recommComm) + 1);
            double value = this.matrix.get(idx);
            this.matrix.set(idx, value+1);
            double giniValue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*(communities.getNumCommunities()-1)/2, this.sum+1);
            this.matrix.set(idx, value);
            return 1.0-giniValue;
        }
    }
    
    @Override
    protected void innerUpdate(U user, Tuple2od<U> tpld)
    {
        U recomm = tpld.v1;

        int userComm = communities.getCommunity(user);
        int recommComm = communities.getCommunity(recomm);

        if(communityGraph.isDirected() || !this.graph.containsEdge(user, recomm))
        {
            // We first update the value if userComm != recommComm:
            if(userComm != recommComm)
            {
                if (communityGraph.isDirected())
                {
                    int idx = userComm * communities.getNumCommunities() + recommComm - (userComm + 1);
                    matrix.set(idx, matrix.get(idx) + 1);
                }
                else
                {
                    int idx = Math.min(userComm, recommComm) * communities.getNumCommunities() + Math.max(userComm, recommComm) - (Math.min(userComm, recommComm) + 1);
                    matrix.set(idx, matrix.get(idx) + 1);
                }
                sum++;
            }
        }
    }

    @Override
    protected void update(Recommendation<U, U> reranked) {
    }
}
