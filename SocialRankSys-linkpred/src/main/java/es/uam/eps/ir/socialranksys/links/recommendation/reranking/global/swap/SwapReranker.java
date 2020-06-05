/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.GlobalReranker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reranker for optimizing a certain global parameter of the recommendations (e.g. Gini).
 * This rerankers start from a graph containing all the recommended edges. Then, edges are
 * swapped with some of the not recommended edges, in order to improve the global parameter.
 * @author Javier Sanz-Cruzado Puig.
 * @param <U> Type of the users.
 * @param <I> Type of the items.
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
     * @param seed Seed of the reranking.
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
     * @param reranked The recommendation.
     */
    protected abstract void update(Recommendation<U, I> reranked);

    /**
     * Reranks the recommendation for a user.
     * @param rec the original recommendation.
     * @param maxLength maximum length of the reranking.
     * @return the new recommendation.
     */
    protected abstract Recommendation<U, I> rerankRecommendation(Recommendation<U, I> rec, int maxLength);
    
}
