/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Unsupervised link prediction algorithm.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class RecommendationLinkPredictor<U> extends AbstractLinkPredictor<U>
{
    /**
     * The unsupervised method.
     */
    private final Recommender<U,U> recommender;
    
    /**
     * Constructor.
     * @param graph The graph we are going to perform link prediction in.
     * @param comparator The comparator for ordering the pairs
     * @param recommender The unsupervised method.
     */
    public RecommendationLinkPredictor(Graph<U> graph, Comparator<Tuple2od<Pair<U>>> comparator, Recommender<U,U> recommender)
    {
        super(graph, comparator);
        this.recommender = recommender;
    }

    @Override
    public List<Tuple2od<Pair<U>>> getPrediction(int maxLength, Predicate<Pair<U>> filter)
    {  
        SortedSet<Tuple2od<Pair<U>>> auxSet = new TreeSet<>(this.getComparator());
        
        // Add the scores for each pair of users which pass the filter.
        this.getGraph().getAllNodes().forEach(u -> 
        {
            Set<U> vFilter = this.getGraph().getAllNodes().filter(v -> filter.test(new Pair<>(u,v))).collect(Collectors.toCollection(HashSet::new));
            int size = vFilter.size();
            Recommendation<U,U> rec = this.recommender.getRecommendation(u, vFilter.stream());
            rec.getItems().forEach(score ->
            {
                auxSet.add(new Tuple2od<>(new Pair<>(u, score.v1), score.v2));
                vFilter.remove(score.v1);
            });
            if(!vFilter.isEmpty())
                vFilter.forEach(v -> auxSet.add(new Tuple2od<>(new Pair<>(u, v), Double.NEGATIVE_INFINITY))); // If some link does not appear in the recommendation.
        });

        return auxSet.stream().limit(maxLength).collect(Collectors.toCollection(ArrayList::new));
    }    
}
