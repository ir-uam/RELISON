/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Reranks a recommendation by improving the Gini Index of the number of edges between 
 * different communities in a community graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class CommOuterPairGiniUserInDegreeReranker<U> extends CommunityReranker<U> 
{
    /**
     * Number of edges between communities
     */
    private final List<Double> matrix;
    /**
     * Community sizes
     */
    private final Map<U, Double> inDegrees;
    /**
     * Total number of edges between communities
     */
    private double sum;
    
    private MultiGraph<Integer> commGraph;
    
    /**
     * Constructor.
     * @param lambda Establishes the trait-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param rank true if the normalization is by ranking position, false if it is by score
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     */
    public CommOuterPairGiniUserInDegreeReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> communities)
    {
        super(lambda, cutoff, norm, rank, graph, communities, false);
        
        matrix = new ArrayList<>();
        inDegrees = new HashMap<>();
        sum = 0.0;
    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {
    }

    @Override
    protected double novAddDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U recomm = itemValue.v1;
        U del = compared.v1;

        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);

        GiniIndex gini = new GiniIndex();
        if(communityGraph.isDirected())
        {
            if(userComm == recommComm) // In this case, demote this
            {
                return -1.0;
            }
            else
            {
                int newIdx = userComm*(communities.getNumCommunities()-1) + recommComm - (userComm > recommComm ? 0 : 1);
                int delIdx = userComm*(communities.getNumCommunities()-1) + delComm - (userComm > delComm ? 0 : 1);
            
            
                DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
                {
                    if(index == newIdx)
                    {
                        return matrix.get(index) + this.inDegrees.get(recomm);
                    }
                    else if(!(userComm==delComm) && index == delIdx)
                    {
                        return matrix.get(index) - this.inDegrees.get(del);
                    }
                    else
                    {
                        return matrix.get(index);
                    }
                });
                
                double auxsum = this.sum;
                if(!(userComm == delComm))
                {
                    auxsum += this.inDegrees.get(recomm) - this.inDegrees.get(del);
                }
                else
                {
                    auxsum += this.inDegrees.get(recomm);
                }

                return 1.0 - gini.compute(stream.boxed(), true, communities.getNumCommunities()*(communities.getNumCommunities()-1), auxsum);
            }
        }
        else
        {
            throw new UnsupportedOperationException("Undirected graphs not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
    @Override
    protected double novAdd(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        throw new UnsupportedOperationException("Undirected graphs not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }
    
    @Override
    protected double novDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        throw new UnsupportedOperationException("Undirected graphs not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
            

    @Override
    protected void innerUpdate(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        U recomm = updated.v1;
        U del = old.v1;

        int userComm = communities.getCommunity(user);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);
        GiniIndex gini = new GiniIndex();

        if(communityGraph.isDirected())
        {
            int newIdx = userComm*(communities.getNumCommunities()-1) + recommComm - (userComm > recommComm ? 0 : 1);
            int delIdx = userComm*(communities.getNumCommunities()-1) + delComm - (userComm > delComm ? 0 : 1);

            if(!(userComm == recommComm))
                matrix.set(newIdx, matrix.get(newIdx)+this.inDegrees.get(recomm));
            if(!(userComm == delComm))
                matrix.set(delIdx, matrix.get(delIdx)-this.inDegrees.get(del));

            double auxsum = this.sum;
            if(!(userComm == recommComm) && !(userComm == delComm))
            {
                auxsum += this.inDegrees.get(recomm) - this.inDegrees.get(del);
            }
            else if(!(userComm == recommComm))
            {
                auxsum += this.inDegrees.get(recomm);
            }
            else if(!(userComm == delComm))
            {
                auxsum -= this.inDegrees.get(del);
            }
            this.sum += auxsum;
            this.globalvalue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*(communities.getNumCommunities()-1), this.sum);
        }
        else
        {
            throw new UnsupportedOperationException("Undirected graphs not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        

    }

    @Override
    protected void computeGlobalValue() 
    {
        GiniIndex gini = new GiniIndex();

        super.computeGlobalValue();
        
        CompleteCommunityGraphGenerator<U> gen = new CompleteCommunityGraphGenerator<>();
        this.commGraph = gen.generate(graph, communities);
        
        
        long vertexcount = commGraph.getVertexCount();
            
        this.graph.getAllNodes().forEach(user -> this.inDegrees.put(user, this.graph.getIncidentNodesCount(user)+0.0));
        
        if(this.graph.isDirected())
        {
            for(int i = 0; i < vertexcount; ++i)
                for(int j = 0; j < vertexcount; ++j)
                    if(i!=j)
                        this.matrix.add(0.0);


            this.graph.getAllNodes().forEach(u -> 
            {
                int commU = this.communities.getCommunity(u);

                this.graph.getAdjacentNodes(u).forEach(v -> 
                {
                    int commV = this.communities.getCommunity(v);
                    int idx;
                    if(commU != commV)
                    {
                        idx = commU*(communities.getNumCommunities()-1) + commV - (commU > commV ? 0 : 1);
                        this.matrix.set(idx, matrix.get(idx) + this.graph.getIncidentNodesCount(v));
                    }
                });
            });

            this.sum = this.matrix.stream().mapToDouble(x -> x).sum();
            this.globalvalue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*(communities.getNumCommunities()-1), sum);
        }
        else
        {
            throw new UnsupportedOperationException("Undirected graphs not supported yet."); //To change body of generated methods, choose Tools | Templates.

        }
        
    }
}
