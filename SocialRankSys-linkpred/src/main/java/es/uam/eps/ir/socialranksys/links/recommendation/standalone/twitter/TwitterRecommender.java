/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.standalone.twitter;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import es.uam.eps.ir.socialranksys.metrics.vertex.PageRank;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;

/**
 * Twitter Recommender. Computes a reduced training graph for each user. This reduced
 * training graph is a bipartite graph which has, on the left side, a circle of trust of the user
 * formed by the nodes with greater personalized pagerank, and on the right side, all their 
 * adjacent nodes.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public abstract class TwitterRecommender<U> extends UserFastRankingRecommender<U>
{
    /**
     * Size of the circle of trust
     */
    private final int circlesize;
    /**
     * Teleport rate for the circle f trust computation.
     */
    private final double r;
    /**
     * Reduced training graphs.
     */
    protected final Map<U, FastGraph<U>> circles;
    
    /**
     * Constructor.
     * @param graph Original graph.
     * @param circlesize Size of the circle of trust.
     * @param r Teleport rate for the circle of trust.
     */
    public TwitterRecommender(FastGraph<U> graph, int circlesize, double r) 
    {
        super(graph);
        this.circlesize = circlesize;        
        this.r = r;
        this.circles = new HashMap<>();
        graph.getAllNodes().forEach(u -> circles.put(u, this.trainingGraph(u)));
    }
    
    /**
     * Computes the circle of trust for a single user.
     * @param u The user
     * @return The circle of trust
     */
    private Set<U> getCircleOfTrust(U u)
    {
        Comparator<Tuple2od<U>> comparator = (Tuple2od<U> t1, Tuple2od<U> t2) -> {
            if(t1.v2 > t2.v2)
                return -1;
            else if(t1.v2 < t2.v2)
                return 1;
            else
            {
                return uIndex.user2uidx(t1.v1) - uIndex.user2uidx(t2.v1);
            }
        };
            
        TreeSet<Tuple2od<U>> treeSet = new TreeSet<>(comparator);
        
        PageRank<U> pr = new PageRank<>(r, u);
        Map<U, Double> pranks = pr.compute(this.getGraph());
        
        pranks.entrySet().stream().filter(entry -> entry.getKey() != u).forEach(entry -> treeSet.add(new Tuple2od<>(entry.getKey(), entry.getValue())));
        
        Set<U> circle = new HashSet<>();
        for(int i = 0; i < this.circlesize && !treeSet.isEmpty(); ++i)
        {
            Tuple2od<U> tuple = treeSet.pollFirst();
            assert tuple != null;
            circle.add(tuple.v1);
        }
        
        return circle;
    }
    
    /**
     * Computes a reduced training graph
     * @param u user
     * @return the reduced training graph for that user.
     */
    private FastGraph<U> trainingGraph(U u)
    {
        EmptyGraphGenerator<U> empty = new EmptyGraphGenerator<>();
        empty.configure(this.getGraph().isDirected(), this.getGraph().isWeighted());
        
        try 
        {
            Graph<U> graph = empty.generate();
            Set<U> circle = this.getCircleOfTrust(u);
            
            for(U v : circle)
            {
                graph.addNode(v);
                this.getGraph().getAdjacentNodes(v).forEach(adj -> 
                {
                    graph.addNode(adj);
                    graph.addEdge(v, adj);
                });
            }
            
            graph.addNode(u);
            this.getGraph().getAdjacentNodes(u).forEach(adj -> {
                graph.addNode(adj);
                graph.addEdge(u, adj);
            });
            
            return (FastGraph<U>) graph;
        } catch (GeneratorNotConfiguredException ex) {
            return null;
        }
    }
    
    
    
    
}
