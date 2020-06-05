/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
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
 *
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class CompleteCommPairGiniUserInDegreeReranker<U> extends CommunityReranker<U> 
{
    private final List<Double> matrix;
    private final Map<U,Double> inDegrees;
    
    private double sum = 0.0;
    
    private MultiGraph<Integer> commGraph;
    
    public CompleteCommPairGiniUserInDegreeReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> communities, boolean autoloops)
    {
        super(lambda, cutoff, norm, rank, graph, communities, autoloops);
        
        this.matrix = new ArrayList<>();
        this.inDegrees = new HashMap<>();
    }

  
    @Override
    protected double novAddDelete(U u, Tuple2od<U> newItem, Tuple2od<U> oldItem)
    {
        U recomm = newItem.v1();
        U del = oldItem.v1();
        
        GiniIndex gini = new GiniIndex();
        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);
        
        int numComm = communities.getNumCommunities();
        if(this.commGraph.isDirected())
        {
            int newIdx = userComm*communities.getNumCommunities() + recommComm;
            int oldIdx = userComm*communities.getNumCommunities() + delComm;
            
            AtomicDouble auxSum = new AtomicDouble();
            auxSum.set(0.0);
            
            
            DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
            {
                if(index == newIdx)
                {
                    double value = (matrix.get(index) + this.inDegrees.get(recomm));
                    auxSum.addAndGet(value);
                    return value;
                }
                else if(index == oldIdx)
                {
                    double value = (matrix.get(index) - this.inDegrees.get(del));
                    auxSum.addAndGet(value);
                    return value;
                }
                else    
                {
                    double value = matrix.get(index);
                    auxSum.addAndGet(value);
                    return value;
                }
            });

            return 1.0 - gini.compute(stream.boxed(), true, numComm*numComm, auxSum.get());
        }
        else
        {
            throw new UnsupportedOperationException("Undirected graphs not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    @Override
    protected double novAdd(U u, Tuple2od<U> newItem, Tuple2od<U> oldItem)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected double novDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {
        
    }

    @Override
    protected void innerUpdate(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        U recomm = updated.v1();
        U del = old.v1();
        
        GiniIndex gini = new GiniIndex();
        int userComm = communities.getCommunity(user);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);
        
        int numComm = communities.getNumCommunities();
        if(this.graph.isDirected())
        {
            int newIdx = userComm*communities.getNumCommunities() + recommComm;
            int oldIdx = userComm*communities.getNumCommunities() + delComm;
             

            AtomicDouble auxSum = new AtomicDouble();
            auxSum.set(0.0);
                       
            for(int index = 0; index < matrix.size(); ++index)
            {
                if(index == newIdx)
                {
                    double value = (matrix.get(index) + this.inDegrees.get(recomm) + 1);
                    matrix.set(index, value);
                    auxSum.addAndGet(value);
                }
                else if(index == oldIdx)
                {
                    double value = matrix.get(index) + this.inDegrees.get(del) - 1;
                    auxSum.addAndGet(value);
                    matrix.set(index, value);
                }
                else
                {
                    double value = matrix.get(index);
                    auxSum.addAndGet(value);
                }

            }
            
            this.inDegrees.put(recomm, this.inDegrees.get(recomm) + 1);
            this.inDegrees.put(del, this.inDegrees.get(del) - 1);

            this.sum = auxSum.get();
            
            this.globalvalue = 1.0 - gini.compute(matrix, true, numComm*numComm, this.sum);
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
        
        CompleteCommunityGraphGenerator<U> generator = new CompleteCommunityGraphGenerator<>();
        this.commGraph = generator.generate(this.graph, this.communities);
        long vertexcount = this.commGraph.getVertexCount();
        
        
        this.graph.getAllNodes().forEach(user -> this.inDegrees.put(user, this.graph.getIncidentNodesCount(user)+0.0));
        
        if(this.graph.isDirected())
        {
            for(int i = 0; i < vertexcount; ++i)
                for(int j = 0; j < vertexcount; ++j)
                    this.matrix.add(0.0);


            this.graph.getAllNodes().forEach(u -> 
            {
                int commU = this.communities.getCommunity(u);

                this.graph.getAdjacentNodes(u).forEach(v -> 
                {
                    int commV = this.communities.getCommunity(v);
                    int idx = commU*this.communities.getNumCommunities() + commV;
                    this.matrix.set(idx, matrix.get(idx) + this.graph.getIncidentNodesCount(v));
                });
            });

            this.sum = this.inDegrees.values().stream().mapToDouble(val -> val*val).sum();
            this.globalvalue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*communities.getNumCommunities(), sum);
        }
        else
        {
            throw new UnsupportedOperationException("Undirected graphs not supported yet."); //To change body of generated methods, choose Tools | Templates.

        }
    }
    
    
}
