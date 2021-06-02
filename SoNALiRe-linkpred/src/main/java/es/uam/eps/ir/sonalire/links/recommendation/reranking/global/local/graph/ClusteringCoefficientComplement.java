/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.global.local.graph;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.sonalire.graph.DirectedGraph;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.global.local.GraphLocalReranker;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Reranker strategy that optimizes the clustering coefficient complement of
 * the network.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ClusteringCoefficientComplement<U> extends GraphLocalReranker<U>
{
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
     * @param lambda    trade-off between the original and novelty scores
     * @param cutoff    maximum length of the definitive ranking.
     * @param norm      the normalization strategy.
     * @param graph     the original graph.
     */
    public ClusteringCoefficientComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph)
    {
        super(cutoff, lambda, norm, graph);

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
        else if(!graph.containsEdge(user, recomm))
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
    protected void innerUpdate(U user, Tuple2od<U> tpld)
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
        else if(!graph.containsEdge(user, recomm))
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
