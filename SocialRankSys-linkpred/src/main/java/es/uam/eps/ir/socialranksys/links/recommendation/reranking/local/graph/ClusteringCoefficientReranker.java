/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.local.graph;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.ranksys.novdiv.reranking.LambdaReranker;
import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Reranker that tries to promote the opposite of the clustering coefficient of
 * the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class ClusteringCoefficientReranker<U> extends LambdaReranker<U,U>
{

    /**
     * Indicates if the scores have to be normalized.
     */
    protected final boolean norm;
    
    private final Graph<U> graph;
    
    private double triplets;
    private double triangles;
    
    public ClusteringCoefficientReranker(double lambda, int cutoff, boolean norm, Graph<U> graph)
    {
        super(lambda, cutoff, norm);
        this.norm = norm;
        this.graph = graph;
        
        graph.getAllNodes().forEach((u)->
            graph.getIncidentNodes(u).forEach((v)->
                graph.getAdjacentNodes(u).forEach((w)->
                {
                    if(!w.equals(v))
                    {
                        ++this.triplets;
                        if(graph.containsEdge(v,w))
                        {
                            ++this.triangles;
                        }
                    }
                })
            )
        );
            
    }

    @Override
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> r, int i)
    {
        Cloner c = new Cloner();
        return new ClusteringCoefficientUserReranker(r, i, c.deepClone(this.graph), triplets, triangles);
    }
    
    protected class ClusteringCoefficientUserReranker extends LambdaUserReranker
    {
        private final Graph<U> graph;
        private double triplets;
        private double triangles;

        public ClusteringCoefficientUserReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, double triplets, double triangles)
        {
            super(recommendation, maxLength);
            this.triplets = triplets;
            this.triangles = triangles;
            this.graph = graph;

            
        }
        
        @Override
        protected double norm(double score, Stats stats)
        {
            if(norm)
            {
                return (score - stats.getMin())/(stats.getMax()-stats.getMin());
            }
            else
                return score;
        }

        @Override
        protected double nov(Tuple2od<U> tpld)
        {
            U user = recommendation.getUser();
            U recomm = tpld.v1;
            
            double numTriplets = this.triplets;
            double numTriangles = this.triangles;
            
            if(graph.isDirected())
            {
                DirectedGraph<U> dgraph = (DirectedGraph<U>) graph;
                List<U> adj = dgraph.getAdjacentNodes(recomm).collect(Collectors.toCollection(ArrayList::new));
                numTriangles += 3.0*dgraph.getIncidentNodes(user).filter(adj::contains).count();
                numTriplets += dgraph.outDegree(recomm) + dgraph.inDegree(user);
                
                if(graph.containsEdge(recomm, user))
                {
                    numTriplets -= 1.0;
                }
                
                if(graph.containsEdge(user, user))
                {
                    numTriplets -= 1.0;
                    if(graph.containsEdge(recomm,user))
                    {
                        numTriangles -= 3.0;
                    }
                }
                
                if(graph.containsEdge(recomm, recomm))
                {
                    numTriplets -= 1.0;
                    if(graph.containsEdge(user, recomm))
                    {
                        numTriangles -= 3.0;
                    }
                }
            }
            else
            {
                numTriangles += 6.0*Stream.concat(graph.getNeighbourNodes(recomm), graph.getNeighbourNodes(user)).distinct().count();
                numTriplets += 2.0*graph.degree(user) + 2.0*graph.degree(recomm);
                
                
                if(graph.containsEdge(recomm, user))
                {
                    numTriplets -= 2.0;
                }
                
                if(graph.containsEdge(user, user))
                {
                    numTriplets -= 2.0;
                    if(graph.containsEdge(recomm,user))
                    {
                        numTriangles -= 6.0;
                    }
                }
                
                if(graph.containsEdge(recomm, recomm))
                {
                    numTriplets -= 2.0;
                    if(graph.containsEdge(user, recomm))
                    {
                        numTriangles -= 6.0;
                    }
                }
            }
            
            return 1.0 - numTriangles / numTriplets;
        }

        @Override
        protected void update(Tuple2od<U> tpld)
        {
            U user = this.recommendation.getUser();
            U recomm = tpld.v1;
            
            if(graph.isDirected())
            {
                DirectedGraph<U> dgraph = (DirectedGraph<U>) graph;
                List<U> adj = dgraph.getAdjacentNodes(recomm).collect(Collectors.toCollection(ArrayList::new));
                this.triangles += 3.0*dgraph.getIncidentNodes(user).filter(adj::contains).count();
                this.triplets += dgraph.outDegree(recomm) + dgraph.inDegree(user);
                
                if(graph.containsEdge(recomm, user))
                {
                    this.triplets -= 1.0;
                }
                
                if(graph.containsEdge(user, user))
                {
                    this.triplets -= 1.0;
                    if(graph.containsEdge(recomm,user))
                    {
                        this.triangles -= 3.0;
                    }
                }
                
                if(graph.containsEdge(recomm, recomm))
                {
                    this.triplets -= 1.0;
                    if(graph.containsEdge(user, recomm))
                    {
                        this.triangles -= 3.0;
                    }
                }
            }
            else
            {
                this.triangles += 6.0*Stream.concat(graph.getNeighbourNodes(recomm), graph.getNeighbourNodes(user)).distinct().count();
                this.triplets += 2.0*graph.degree(user) + 2.0*graph.degree(recomm);
                
                
                if(graph.containsEdge(recomm, user))
                {
                    this.triplets -= 2.0;
                }
                
                if(graph.containsEdge(user, user))
                {
                    this.triplets -= 2.0;
                    if(graph.containsEdge(recomm,user))
                    {
                        this.triangles -= 6.0;
                    }
                }
                
                if(graph.containsEdge(recomm, recomm))
                {
                    this.triplets -= 2.0;
                    if(graph.containsEdge(user, recomm))
                    {
                        this.triangles -= 6.0;
                    }
                }
            }
        }
        
    }
}
