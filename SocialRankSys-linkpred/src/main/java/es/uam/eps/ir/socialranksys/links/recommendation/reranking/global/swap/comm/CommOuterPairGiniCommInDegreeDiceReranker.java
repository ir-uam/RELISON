/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm;

import com.google.common.util.concurrent.AtomicDouble;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.community.Communities;
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
import java.util.stream.Stream;

/**
 * Reranks a recommendation by improving the Gini Index of the number of edges between 
 * different communities in a community graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class CommOuterPairGiniCommInDegreeDiceReranker<U> extends CommunityReranker<U> 
{
    /**
     * Number of edges between communities
     */
    private final List<Double> matrix;
    private final List<Double> inDegrees;
    private final List<Double> outDegrees;
    /**
     * Community sizes
     */
    private final Map<Integer, Double> sizes;
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
    public CommOuterPairGiniCommInDegreeDiceReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> communities)
    {
        super(lambda, cutoff, norm, rank, graph, communities, false);
        
        matrix = new ArrayList<>();
        inDegrees = new ArrayList<>();
        outDegrees = new ArrayList<>();
        sizes = new HashMap<>();
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
            if(userComm == recommComm)
            {
                return -1.0;
            }
            else
            {
                List<Double> auxDegree = new ArrayList<>();
                
                for(int i = 0; i < communities.getNumCommunities();++i)
                {
                    double out = this.outDegrees.get(i);
                    if(i == userComm && !(delComm==userComm))
                    {
                        out -= 1;
                    }
                    
                    if(i == userComm && delComm == userComm)
                    {
                        out += 1;
                    }
                    
                    for(int j = 0; j < communities.getNumCommunities(); ++j)
                    {
                        double in = this.inDegrees.get(j);
                        if(i != j)
                        {
                            
                            if(j == recommComm)
                            {
                                in += 1;
                            }
                            
                            if(j == delComm && !(userComm == delComm))
                            {
                                in -= 1;
                            }
                            
                            auxDegree.add(out*in);
                        }
                    }
                }
                
                
                
                
                int newIdx = userComm*(communities.getNumCommunities()-1) + recommComm - (userComm > recommComm ? 0 : 1);
                int delIdx = userComm*(communities.getNumCommunities()-1) + delComm - (userComm > delComm ? 0 : 1);
            
            
                DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
                {
                    double value = matrix.get(index);
                    if(index == newIdx)
                    {
                        value += 1.0;
                    }
                    else if(!(userComm==delComm) && index == delIdx)
                    {
                        value -= 1.0;
                    }

                    value = value/auxDegree.get(index);
                    return value;
                });
                
                double auxSum = IntStream.range(0, matrix.size()).mapToDouble(index -> 
                {
                    double value = matrix.get(index);
                    if(index == newIdx)
                    {
                        value += 1.0;
                    }
                    else if(!(userComm==delComm) && index == delIdx)
                    {
                        value -= 1.0;
                    }

                    value = value/auxDegree.get(index);
                    return value;
                }).sum();


                return 1.0 - gini.compute(stream.boxed(), true, communities.getNumCommunities()*(communities.getNumCommunities()-1), auxSum);
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

        Integer userComm = communities.getCommunity(user);
        Integer recommComm = communities.getCommunity(recomm);
        Integer delComm = communities.getCommunity(del);
        GiniIndex gini = new GiniIndex();

        AtomicDouble atomic = new AtomicDouble();
        atomic.set(0.0);
        if(communityGraph.isDirected())
        {
            List<Double> auxDegree = new ArrayList<>();
                
            for(int i = 0; i < communities.getNumCommunities();++i)
            {
                double out = this.outDegrees.get(i);
                if(i == userComm && !delComm.equals(userComm) && recommComm.equals(userComm))
                {
                    out -= 1;
                }

                if(i == userComm && !recommComm.equals(userComm) && delComm.equals(userComm))
                {
                    out += 1;
                }
                
                this.outDegrees.set(i, out);

            }
            
            for(int j = 0; j < communities.getNumCommunities(); ++j)
            {
                double in = this.inDegrees.get(j);
                
                if(j == recommComm && !userComm.equals(recommComm))
                {
                    in += 1;
                }

                if(j == delComm && !userComm.equals(delComm))
                {
                    in -= 1;
                }
                
                this.inDegrees.set(j, in);
            }
            
            for(int i = 0; i < communities.getNumCommunities(); ++i)
            {
                for(int j = 0; j < communities.getNumCommunities(); ++j)
                {
                    if(i!=j)
                    {
                        auxDegree.add(this.outDegrees.get(i)*this.inDegrees.get(j));
                    }
                }
            }
            
            int newIdx = userComm*(communities.getNumCommunities()-1) + recommComm - (userComm > recommComm ? 0 : 1);
            int delIdx = userComm*(communities.getNumCommunities()-1) + delComm - (userComm > delComm ? 0 : 1);
            
            DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
            {
                double value = matrix.get(index);
                if(!userComm.equals(recommComm) && index == newIdx)
                {
                    value += 1.0;
                    matrix.set(index, value);
                }
                else if(!userComm.equals(delComm) && index == delIdx)
                {
                    value -= 1.0;
                    matrix.set(index, value);

                }

                value = value/auxDegree.get(index);
                return value;
            });
            
            this.sum = IntStream.range(0, matrix.size()).mapToDouble(index -> 
            {
                double value = matrix.get(index);
                value = value/auxDegree.get(index);
                return value;
            }).sum();
            this.globalvalue = 1.0 - gini.compute(stream.boxed(), true, communities.getNumCommunities()*(communities.getNumCommunities()-1), this.sum);
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
        
        
        List<Double> aux = new ArrayList<>();
        
        
        
        
        long vertexcount = communityGraph.getVertexCount();
            
        this.communities.getCommunities().forEach(comm -> 
        {
            this.inDegrees.add(this.communityGraph.getIncidentEdgesCount(comm)+0.0);
            this.outDegrees.add(this.communityGraph.getAdjacentEdgesCount(comm)+0.0);
        });
        
        
        
        if(this.communityGraph.isDirected())
        {
            for(int i = 0; i < vertexcount; ++i)
            {
                for(int j = 0; j < vertexcount; ++j)
                {
                    if(i!=j)
                    {
                        this.matrix.add(this.communityGraph.getNumEdges(i, j)+0.0);
                        aux.add(this.outDegrees.get(i)*this.inDegrees.get(j));
                    }
                }
            }

            Stream<Double> stream = IntStream.range(0, matrix.size()).mapToDouble(i -> matrix.get(i)/aux.get(i)).boxed();
            
            
            this.sum = IntStream.range(0, matrix.size()).mapToDouble(i -> matrix.get(i)/aux.get(i)).sum();
            
            this.globalvalue = 1.0 - gini.compute(stream, true, communities.getNumCommunities()*(communities.getNumCommunities()-1), sum);
            
            System.err.println(this.globalvalue);
        }
        else
        {
            throw new UnsupportedOperationException("Undirected graphs not supported yet."); //To change body of generated methods, choose Tools | Templates.

        }
        
    }
}
