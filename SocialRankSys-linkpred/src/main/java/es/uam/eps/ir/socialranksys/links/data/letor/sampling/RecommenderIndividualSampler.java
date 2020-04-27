/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor.sampling;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.Graph;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Samples the top k of a contact recommendation algorithm.
 * @author Javier Sanz-Cruzado
 * @param <U> Type of the users.
 */
public class RecommenderIndividualSampler<U> extends AbstractIndividualSampler<U>
{
    private final Recommender<U,U> rec;
    private final int k;
    /**
     * Constructor.
     * @param graph the graph.
     * @param rec a recommendation algorithm.
     * @param k the cutoff of the recommendation.
     */
    public RecommenderIndividualSampler(Graph<U> graph, Recommender<U,U> rec, int k)
    {
        super(graph);
        this.rec = rec;
        this.k = k;
    }
    
    @Override
    public Set<U> sampleUsers(U u, Predicate<U> filter)
    {
        Set<U> sample = new HashSet<>();
        Recommendation<U,U> recommendation = rec.getRecommendation(u, k, filter);
        recommendation.getItems().forEach(v -> sample.add(v.v1()));
        return sample;
    }
    
}
