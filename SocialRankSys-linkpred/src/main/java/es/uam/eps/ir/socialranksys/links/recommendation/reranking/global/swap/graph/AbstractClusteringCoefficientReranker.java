/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.graph;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.GraphSwapReranker;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Swap reranker that promotes a metric related to the global clustering coefficient of the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 *
 * @see es.uam.eps.ir.socialranksys.metrics.graph.ClusteringCoefficient
 */
public abstract class AbstractClusteringCoefficientReranker<U> extends GraphSwapReranker<U>
{
    /**
     * Number of triplets
     */
    private double triplets;
    /**
     * Number of triangles or closed triplets
     */
    private double triangles;
    /**
     * Indicates if the metric has to be promoted or its complement has to be promoted.
     */
    private final boolean promote;
    
    /**
     * Constructor
     * @param lambda    trade-off between the original and novelty score (clustering coefficient)
     * @param cutoff    maximum length of the recommendation ranking
     * @param norm      the normalization scheme.
     * @param graph     the original graph.
     * @param promote   true if the metric has to be promoted, false if the complementary metric has to be promoted.
     */
    public AbstractClusteringCoefficientReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, boolean promote)
    {
        super(lambda, cutoff, norm, graph);
        this.promote = promote;
    }

    /**
     * Computes the new clustering coefficient for a given user
     * @param user The user
     * @param updated The score for the new recommended user.
     * @param old The score for the old recommended user.
     * @return the updated number of triplets and triangles
     */
    protected Pair<Double> newcoef(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        Pair<Double> newCoef = this.newcoefdelete(user, old, triplets, triangles);
        return this.newcoefadd(user, updated, newCoef.v1(), newCoef.v2());
    }
    
    /**
     * Computes the resulting clustering coefficient from removing a link
     * @param user The origin node of the link
     * @param old The user to remove along the old score.
     * @param numTriplets the current number of triplets
     * @param numTriangles the current number of triangles.
     * @return the updated number of triplets and triangles.
     */
    protected Pair<Double> newcoefdelete(U user, Tuple2od<U> old, double numTriplets, double numTriangles)
    {
        U recomm = old.v1;


        if(graph.isDirected())
        {
            DirectedGraph<U> dgraph = (DirectedGraph<U>) graph;
            List<U> adj = dgraph.getAdjacentNodes(recomm).collect(Collectors.toCollection(ArrayList::new));
            List<U> inc = dgraph.getIncidentNodes(recomm).collect(Collectors.toCollection(ArrayList::new));
            numTriangles -= dgraph.getAdjacentNodes(user).filter(adj::contains).count();
            numTriangles -= dgraph.getAdjacentNodes(user).filter(inc::contains).count();
            numTriangles -= dgraph.getIncidentNodes(user).filter(inc::contains).count();
            numTriplets -= dgraph.outDegree(recomm) + dgraph.inDegree(user);

            if(graph.containsEdge(recomm, user))
            {
                numTriplets += 1.0;
            }

            if(graph.containsEdge(user, user))
            {
                numTriplets += 1.0;
                if(graph.containsEdge(recomm,user))
                {
                    numTriangles += 3.0;
                }
            }

            if(graph.containsEdge(recomm, recomm))
            {
                numTriplets += 1.0;
                if(graph.containsEdge(user, recomm))
                {
                    numTriangles += 3.0;
                }
            }
        }
        else
        {
            numTriangles -= 6.0*Stream.concat(graph.getNeighbourNodes(recomm), graph.getNeighbourNodes(user)).distinct().count();
            numTriplets -= (2.0*graph.degree(user) + 2.0*graph.degree(recomm));


            if(graph.containsEdge(recomm, user))
            {
                numTriplets += 2.0;
            }

            if(graph.containsEdge(user, user))
            {
                numTriplets += 2.0;
                if(graph.containsEdge(recomm,user))
                {
                    numTriangles += 6.0;
                }
            }

            if(graph.containsEdge(recomm, recomm))
            {
                numTriplets += 2.0;
                if(graph.containsEdge(user, recomm))
                {
                    numTriangles += 6.0;
                }
            }
        }
        
        return new Pair<>(numTriplets, numTriangles);
    }
    
    /**
     * Computes the resulting clustering coefficient from adding a link
     * @param user The origin node of the link
     * @param updated The user to add along its score.
     * @param numTriplets the current number of triplets
     * @param numTriangles the current number of triangles.
     * @return the updated number of triplets and triangles.
     */
    protected Pair<Double> newcoefadd(U user, Tuple2od<U> updated, double numTriplets, double numTriangles)
    {
        U recomm = updated.v1;


        if(graph.isDirected())
        {
            DirectedGraph<U> dgraph = (DirectedGraph<U>) graph;
            List<U> adj = dgraph.getAdjacentNodes(recomm).collect(Collectors.toCollection(ArrayList::new));
            List<U> inc = dgraph.getIncidentNodes(recomm).collect(Collectors.toCollection(ArrayList::new));
            numTriangles += dgraph.getAdjacentNodes(user).filter(adj::contains).count();
            numTriangles += dgraph.getAdjacentNodes(user).filter(inc::contains).count();
            numTriangles += dgraph.getIncidentNodes(user).filter(inc::contains).count();
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
        
        return new Pair<>(numTriplets, numTriangles);
    }

   
    @Override
    protected void update(Recommendation<U, U> reranked) {
    }
    
    @Override
    protected void innerUpdate(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        Pair<Double> pair;
        if(this.graph.isDirected())
        {
            pair = newcoef(user, updated, old);
        }
        else if(this.recs.get(old.v1).contains(user) && this.recs.get(updated.v1).contains(user))
        {
            pair = new Pair<>(this.triplets, this.triangles);
        }
        else if(this.recs.get(old.v1).contains(user))
        {
            pair = this.newcoefadd(user, updated, triplets, triangles);
        }
        else if(this.recs.get(updated.v1).contains(user))
        {
            pair = this.newcoefdelete(user, old, triplets, triangles);
        }
        else
        {
            pair = this.newcoef(user, updated, old);
        }
        
        this.triplets = pair.v1();
        this.triangles = pair.v2();
        if(this.promote)
        {
            this.globalvalue = triangles/triplets;
        }
        else
        {
            this.globalvalue = 1.0 - triangles/triplets;
        }
        
    }

    @Override
    protected double novAddDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        Pair<Double> pair = newcoef(u, itemValue, compared);
        if(this.promote)
            return pair.v2()/pair.v1();
        else
            return 1.0 - pair.v2()/pair.v1();
    }
    
    @Override
    protected double novAdd(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        Pair<Double> pair = this.newcoefadd(u, itemValue, triplets, triangles);
        if(this.promote)
            return pair.v2()/pair.v1();
        else
            return 1.0 - pair.v2()/pair.v1();
    }

    @Override
    protected double novDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared) {
        Pair<Double> pair = this.newcoefdelete(u, compared, triplets, triangles);
        if(this.promote)
            return pair.v2()/pair.v1();
        else
            return 1.0 - pair.v2()/pair.v1();
    }

    @Override
    protected void computeGlobalValue() 
    {
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
        
        if(this.promote)
        {
            this.globalvalue = triangles/triplets;
        }
        else
        {
            this.globalvalue = 1.0 - triangles/triplets;
        }
        
    }

    
}
