/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.graph;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.LocalLambdaReranker;
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
public class ClusteringCoefficientReranker<U> extends LocalLambdaReranker<U,U>
{

    /**
     * Indicates if the scores have to be normalized.
     */
    protected final boolean norm;
    /**
     * The original graph
     */
    private final Graph<U> graph;
    /**
     * The number of triplets
     */
    private double triplets;
    /**
     * The number of closed triplets or triangles
     */
    private double triangles;
    
    /**
     * Constructor.
     * @param lambda Param that establishes a balance between the score and the 
     * novelty/diversity value.
     * @param cutoff Number of elements to take.
     * @param norm Indicates if scores have to be normalized.
     * @param graph The graph.
     * @param rank Indicates if the normalization is by ranking (true) or by score (false)
     */
    public ClusteringCoefficientReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph) 
    {
        super(cutoff, lambda, norm, rank);
        this.norm = norm;
        this.graph = graph;
        
        graph.getAllNodes().forEach((u)->
            graph.getIncidentNodes(u).forEach((v)->
                graph.getAdjacentNodes(u).forEach((w)->
                {
                    if (!w.equals(v))
                    {
                        ++this.triplets;
                        if (graph.containsEdge(v, w))
                        {
                            ++this.triangles;
                        }
                    }
                })
            )
        );
    }
    
    @Override
    protected double nov(U user, Tuple2od<U> tpld)
    {
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
    protected void update(U user, Tuple2od<U> tpld)
    {
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

    @Override
    protected void update(Recommendation<U, U> reranked) {
    }
}
