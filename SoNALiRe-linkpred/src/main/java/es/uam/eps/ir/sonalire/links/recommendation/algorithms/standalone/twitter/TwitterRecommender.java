/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.twitter;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.sonalire.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.sonalire.links.recommendation.UserFastRankingRecommender;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.RecommenderSupplier;
import es.uam.eps.ir.sonalire.metrics.vertex.PageRank;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Twitter-based recommender. Following the details disclosed by Twitter about their contact
 * recommendation approaches, these algorithms:
 * <ol>
 * <li>for the target user, compute a reduced bipartite training graph.
 *      <ul>
 *          <li>Left side: a circle of trust of the users (nodes with greater pers. PageRank value)</li>
 *          <li>Right side: the adjacent nodes to those in the left side</li>
 *      </ul>
 * </li>
 * <li>use a given recommendation (bipartite) algorithm to finish.</li>
 * </ol>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
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
     * The recommender supplier
     */
    protected final RecommenderSupplier<U> supplier;
    
    /**
     * Constructor.
     * @param graph         original graph.
     * @param circlesize    size of the circle of trust.
     * @param r             teleport rate for the personalized PageRank algorithm for computing the circle of trust.
     * @param supplier      a supplier for the contact recommendation algorithm to apply in the reduced network.
     */
    public TwitterRecommender(FastGraph<U> graph, int circlesize, double r, RecommenderSupplier<U> supplier)
    {
        super(graph);
        this.circlesize = circlesize;        
        this.r = r;
        this.circles = new HashMap<>();

        if(circlesize >= graph.getVertexCount() || circlesize <= 0)
        {
            FastGraph<U> g = trainingGraph(graph.getAllNodes().collect(Collectors.toSet()));
            graph.getAllNodes().forEach(u -> circles.put(u, g));
        }
        else
        {
            graph.getAllNodes().forEach(u -> circles.put(u, this.trainingGraph(u)));
        }


        this.supplier = supplier;
    }
    
    /**
     * Computes the circle of trust for a single user.
     * @param u the user
     * @return the circle of trust
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
     * Builds the training bipartite graph.
     * @param nodes the set of nodes.
     * @return the graph if everything goes well, false otherwise.
     */
    private FastGraph<U> trainingGraph(Set<U> nodes)
    {
        EmptyGraphGenerator<U> empty = new EmptyGraphGenerator<>();
        empty.configure(this.getGraph().isDirected(), this.getGraph().isWeighted());

        try
        {
            Graph<U> graph = empty.generate();

            for (U v : nodes)
            {
                graph.addNode(v);
                this.getGraph().getAdjacentNodes(v).forEach(adj ->
                {
                    graph.addNode(adj);
                    graph.addEdge(v, adj);
                });
            }

            return (FastGraph<U>) graph;
        }
        catch (GeneratorNotConfiguredException ex)
        {
            return null;
        }
    }
    /**
     * Computes a reduced training graph
     * @param u user
     * @return the reduced training graph for that user.
     */
    private FastGraph<U> trainingGraph(U u)
    {
        Set<U> circle = this.getCircleOfTrust(u);
        circle.add(u);
        return this.trainingGraph(circle);
    }

    @Override
    public Int2DoubleMap getScoresMap(int uIdx)
    {
        Int2DoubleMap output = new Int2DoubleOpenHashMap();
        U u = uIndex.uidx2user(uIdx);

        FastGraph<U> graph = this.circles.get(u);
        UserFastRankingRecommender<U> rec = supplier.get(graph);
        Int2DoubleMap scores = rec.getScoresMap(rec.user2uidx(u));

        iIndex.getAllIidx().forEach(iIdx ->
        {
            if(scores.containsKey(iIdx))
            {
                output.put(iIdx, scores.get(iIdx));
            }
            else
            {
                output.put(iIdx, 0.0);
            }
        });

        return output;
    }
    
    
}
