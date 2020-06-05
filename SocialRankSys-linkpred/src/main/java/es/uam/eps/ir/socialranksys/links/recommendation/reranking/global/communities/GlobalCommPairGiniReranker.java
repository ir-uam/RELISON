/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.communities;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Reranks a recommendation by improving the Gini Index of the number of edges 
 * between two different communities in a community graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class GlobalCommPairGiniReranker<U> extends GlobalCommunityReranker<U> 
{
    /**
     * A matrix representing the number of edges between each pair of communities.
     */
    private final List<Double> matrix;
    /**
     * The total number of edges between communities.
     */
    private double sum;
    
    /**
     * Constructor
     * @param lambda Trade-off between the original and novelty scores.
     * @param cutoff Maximum length of the recommendation
     * @param norm true if the scores have to be normalized, false if not.
     * @param graph the original graph.
     * @param communities the communities.
     */
    public GlobalCommPairGiniReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, Communities<U> communities)
    {
        super(lambda, cutoff, norm, graph, communities);
        
        this.matrix = new ArrayList<>();
        if(communityGraph.isDirected())
            {
                for(int i = 0; i < communityGraph.getVertexCount(); ++i)
                {
                    for(int j = 0; j < communityGraph.getVertexCount(); ++j)
                    {
                        if(i != j)
                            matrix.add(communityGraph.getNumEdges(i, j)+0.0);
                    }
                }
            }
            else
            {
                for(int i = 0; i < communityGraph.getVertexCount(); ++i)
                    for(int j = 0; j < i; ++j)
                        matrix.add(communityGraph.getNumEdges(i, j)+0.0);
            }
            
            sum = communityGraph.getEdgeCount();
    }

    @Override
    protected double nov(U user, Tuple2od<U> item)
    {
       U recomm = item.v1;
            
        int userComm = communities.getCommunity(user);
        int recommComm = communities.getCommunity(recomm);

        GiniIndex gini = new GiniIndex();
        if(communityGraph.isDirected())
        {
            int idx = userComm*communities.getNumCommunities() + recommComm - (userComm + 1);
            DoubleStream stream = IntStream.range(0, matrix.size()).parallel().mapToDouble(value -> {
                if(value == idx)
                    return matrix.get(value) + 1.0;
                else
                    return matrix.get(value);
            });
            
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
    protected void update(U user, Tuple2od<U> selectedItem)
    {
        U recomm = selectedItem.v1;
            
        int userComm = communities.getCommunity(user);
        int recommComm = communities.getCommunity(recomm);

        matrix.set( userComm*communities.getNumCommunities()-(userComm+1), 
                    matrix.get(userComm*communities.getNumCommunities()-userComm-1)+1);
        if(communityGraph.isDirected())
        {
            int idx = userComm*communities.getNumCommunities() + recommComm - (userComm + 1);
            matrix.set(idx, matrix.get(idx)+1);
        }
        else
        {
            int idx = Math.min(userComm, recommComm)*communities.getNumCommunities() + Math.max(userComm, recommComm) - (Math.min(userComm,recommComm) + 1);
            matrix.set(idx, matrix.get(idx)+1);
        }
        sum++;
    }  
}
