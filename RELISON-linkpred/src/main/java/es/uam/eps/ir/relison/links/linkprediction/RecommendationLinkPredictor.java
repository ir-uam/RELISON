/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.linkprediction;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.utils.ComputationMonitor;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Link prediction algorithm based on a contact recommendation algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class RecommendationLinkPredictor<U> extends AbstractLinkPredictor<U>
{
    /**
     * The original contact recommendation algorithm.
     */
    private final Recommender<U,U> recommender;
    
    /**
     * Constructor.
     * @param graph         the social network graph.
     * @param comparator    the comparator for sorting the pairs.
     * @param recommender   the original contact recommendation algorithm.
     */
    public RecommendationLinkPredictor(Graph<U> graph, Comparator<Tuple2od<Pair<U>>> comparator, Recommender<U,U> recommender)
    {
        super(graph, comparator);
        this.recommender = recommender;
    }

    /**
     * Constructor.
     * @param graph         the social network graph.
     * @param recommender   the original contact recommendation algorithm.
     */
    public RecommendationLinkPredictor(Graph<U> graph, Recommender<U,U> recommender)
    {
        super(graph);
        this.recommender = recommender;
    }

    @Override
    public Prediction<U> getPrediction(int maxLength, Predicate<Pair<U>> filter)
    {  
        if (maxLength < 0) throw new IllegalArgumentException("maxLength must be non-negative");
        ComputationMonitor monitor = ComputationMonitor.current();
        monitor.phase("Predicting links by source node");
        if (maxLength == 0) return new Prediction<>(new ArrayList<>());
        Comparator<Tuple2od<Pair<U>>> comparator = this.getComparator();
        // The worst retained link is at the head, so better candidates can replace it.
        PriorityQueue<Tuple2od<Pair<U>>> best = new PriorityQueue<>(11, comparator.reversed());
        // Preserve TreeSet's comparator-based uniqueness, including for custom comparators.
        SortedSet<Tuple2od<Pair<U>>> retained = new TreeSet<>(comparator);
        List<U> nodes = this.getGraph().getAllNodes().collect(Collectors.toList());
        long completed = 0;
        
        // Add the scores for each pair of users which pass the filter.
        for (U u : nodes)
        {
            monitor.checkCancelled();
            Set<U> vFilter = this.getGraph().getAllNodes().filter(v -> filter.test(new Pair<>(u,v))).collect(Collectors.toCollection(HashSet::new));
            Recommendation<U,U> rec = this.recommender.getRecommendation(u, vFilter.stream());
            for (Tuple2od<U> score : rec.getItems())
            {
                monitor.checkCancelled();
                retain(best, retained, new Tuple2od<>(new Pair<>(u, score.v1), score.v2), maxLength);
                vFilter.remove(score.v1);
            }
            // Missing recommendation scores still participate with negative infinity.
            for (U v : vFilter)
            {
                monitor.checkCancelled();
                retain(best, retained, new Tuple2od<>(new Pair<>(u, v), Double.NEGATIVE_INFINITY), maxLength);
            }
            monitor.checkCancelled();
            monitor.progress("Predicting links by source node", ++completed, nodes.size(), "source nodes");
        }

        monitor.checkCancelled();
        List<Tuple2od<Pair<U>>> ranking = new ArrayList<>(best);
        ranking.sort(comparator);
        monitor.checkCancelled();
        return new Prediction<>(ranking);
    }

    /** Retains at most maxLength distinct candidates in the global ranking. */
    private void retain(PriorityQueue<Tuple2od<Pair<U>>> best, SortedSet<Tuple2od<Pair<U>>> retained,
                        Tuple2od<Pair<U>> candidate, int maxLength)
    {
        if (best.size() == maxLength && this.getComparator().compare(candidate, best.peek()) >= 0) return;
        if (!retained.add(candidate)) return;
        if (best.size() == maxLength) retained.remove(best.poll());
        best.add(candidate);
    }
}
