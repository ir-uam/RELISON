/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction;


import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract class for defining link prediction algorithms
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public abstract class AbstractLinkPredictor<U> implements LinkPredictor<U>
{
    /**
     * The graph we are going to perform link prediction in.
     */
    private final Graph<U> graph;
    /**
     * The comparator of pairs to return a sorted list.
     */
    private final Comparator<Tuple2od<Pair<U>>> comparator;
    
    /**
     * Constructor.
     * @param graph The graph we are going to perform link prediction in.
     * @param comparator Comparator for reordering the pairs
     */
    public AbstractLinkPredictor(Graph<U> graph, Comparator<Tuple2od<Pair<U>>> comparator)
    {
        this.graph = graph;
        this.comparator = comparator;
    }

    @Override
    public List<Tuple2od<Pair<U>>> getPrediction()
    {
        return this.getPrediction(Integer.MAX_VALUE);
    }

    @Override
    public List<Tuple2od<Pair<U>>> getPrediction(int maxLength)
    {
        return this.getPrediction(maxLength, pair -> true);
    }

    @Override
    public List<Tuple2od<Pair<U>>> getPrediction(Predicate<Pair<U>> filter)
    {
        return this.getPrediction(Integer.MAX_VALUE, filter);
    }

    @Override
    public abstract List<Tuple2od<Pair<U>>> getPrediction(int maxLength, Predicate<Pair<U>> filter);

    @Override
    public List<Tuple2od<Pair<U>>> getPrediction(Stream<Pair<U>> candidates) {
        Set<Pair<U>> pairs = candidates.collect(Collectors.toCollection(HashSet::new));

        return getPrediction(pairs::contains);
    }

    @Override
    public double getPredictionScore(U u, U v) 
    {
        Pair<U> pair = new Pair<>(u,v);
        List<Pair<U>> pairs = new ArrayList<>();
        pairs.add(pair);
        
        List<Tuple2od<Pair<U>>> pred = this.getPrediction(pairs.stream());
        if(pred.isEmpty())
            return Double.NEGATIVE_INFINITY;
        else
            return pred.get(0).v2;
    }

    /**
     * Obtains the graph over which we execute the link prediction algorithm
     * @return the graph.
     */
    protected Graph<U> getGraph() {
        return graph;
    }

    /**
     * Obtains the comparator for ordering the nodes.
     * @return the comparator.
     */
    protected Comparator<Tuple2od<Pair<U>>> getComparator() {
        return comparator;
    }
    
    
    
    
}
