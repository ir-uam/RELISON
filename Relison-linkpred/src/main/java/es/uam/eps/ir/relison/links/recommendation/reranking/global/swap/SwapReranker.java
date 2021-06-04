/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.swap;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.links.recommendation.reranking.global.GlobalReranker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract implementation for a family of reranking strategies for optimizing a global
 * parameter of the recommendations (e.g. Gini).
 *
 * These reranking strategies start from a system which has a certain value of the property
 * to optimize. Then, the rerankers consider an initial scenario where all the user-item pairs
 * among the top-k recommended items for each user are added to the system.
 *
 * Then, we run over the top-k recommended items. For each of them, we check whether it is better
 * to keep this item in the ranking, or choose one of the remaining (the not recommended ones) instead.
 * If we decide that it is better to change them, we swap them, and update the global property accordingly.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the items.
 */
public abstract class SwapReranker<U,I> implements GlobalReranker<U,I>
{
    /**
     * Seed for the order of the users
     */
    private final long seed;
    /**
     * Current global value of the metric.
     */
    protected double globalvalue = 0.0;
    
    /**
     * Constructor. Default seed, not randomly chosen.
     */
    public SwapReranker()
    {
        seed = 0;
    }
    
    /**
     * Constructor.
     * @param seed random seed for establishing the order in which we run over the users.
     */
    public SwapReranker(long seed)
    {
        this.seed = seed;
    }
    
    @Override
    public Stream<Recommendation<U, I>> rerankRecommendations(Stream<Recommendation<U, I>> recommendation, int maxLength)
    {
        List<Recommendation<U,I>> recommendations = recommendation.collect(Collectors.toCollection(ArrayList::new));
        
        // Randomly reorders the recommendations
        Collections.shuffle(recommendations, new Random(seed));
        
        
        List<Recommendation<U,I>> output = new ArrayList<>();
        // Reranks every recommendation.
        for(Recommendation<U,I> rec : recommendations)
        {
            Recommendation<U,I> reranked = this.rerankRecommendation(rec, maxLength);
            output.add(reranked);
            this.update(reranked);
        }
        
        // returns the new rankings
        return output.stream();
    }

    /**
     * Updates the reranking algorithm values, using a certain recommendation.
     * @param reranked the recommendation.
     */
    protected abstract void update(Recommendation<U, I> reranked);

    /**
     * Reranks the recommendation for a user.
     * @param rec       the original recommendation.
     * @param maxLength maximum length of the definitive ranking.
     * @return the new recommendation.
     */
    protected abstract Recommendation<U, I> rerankRecommendation(Recommendation<U, I> rec, int maxLength);
    
}
