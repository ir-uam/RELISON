/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.globalranking.graph;

import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.globalranking.GlobalRankingLambdaReranker;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Global reranker strategy that optimizes the clustering coefficient complement of
 * the network.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ClusteringCoefficientComplement<U> extends GlobalRankingLambdaReranker<U,U>
{
    /**
     * The graph.
     */
    private final Graph<U> graph;
    /**
     * The number of triplets in the network.
     */
    private double triplets;
    /**
     * The number of triangles in the network.
     */
    private double triangles;

    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     */
    public ClusteringCoefficientComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph)
    {
        super(lambda, cutoff, norm);
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
    protected void update(U user, Tuple2od<U> selectedItem)
    {
        U recomm = selectedItem.v1;

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
