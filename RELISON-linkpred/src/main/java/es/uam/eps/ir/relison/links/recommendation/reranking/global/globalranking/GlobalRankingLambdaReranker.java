/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.globalranking;

import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.utils.datatypes.Tuple2oo;
import org.ranksys.core.util.tuples.Tuple2id;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Implementation of a greedy reranking strategy for optimizing global properties of the system.
 * This reranker chooses the items which have a more significant impact on the global property to optimize.
 *
 * Given a set of recommendations for different users, these rerankers iteratively choose the user-item
 * pair that maximizes a given objective function. The objective function jointly maximizes the accuracy
 * of the system and the novelty/diversity of the system (it takes a trade-off between them).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the items
 */
public abstract class GlobalRankingLambdaReranker<U,I> extends GlobalRankingGreedyReranker<U,I>
{
    /**
     * Trade-off between the original and the novelty scores.
     */
    protected final double lambda;
    /**
     * Statistics for the original scores.
     */
    protected Normalizer<I> recStats;
    /**
     * Statistics for the novelty scores
     */
    protected Normalizer<I> novStats;
    /**
     * For each pair user-item, their associated novelty
     */
    private Map<U, Map<I, Double>> novMap;
    /**
     * Supplier for the normalization strategy.
     */
    private final Supplier<Normalizer<I>> norm;
    
    /**
     * Constructor.
     * @param lambda    trade-off between the original and the novelty scores.
     * @param cutOff    maximum size of the definitive ranking.
     * @param norm      the normalization strategy.
     */
    public GlobalRankingLambdaReranker(double lambda, int cutOff, Supplier<Normalizer<I>> norm)
    {
        super(cutOff);
        this.norm = norm;
        this.lambda = lambda;
    }
    
    @Override
    protected Tuple2oo<U, Tuple2id> selectRecommendation(Map<U, List<Tuple2od<I>>> remainingItems)
    {
        this.recStats = norm.get();
        this.novStats = norm.get();
        this.novMap = new HashMap<>();
        
        remainingItems.keySet().forEach(user -> 
        {
            novMap.put(user, new HashMap<>());
            remainingItems.get(user).forEach(item -> 
            {
                recStats.add(item.v1, item.v2);
                double nov = this.nov(user, item);
                novStats.add(item.v1, nov);
                novMap.get(user).put(item.v1, nov);
            });
        });
        return super.selectRecommendation(remainingItems);
    }
    
    @Override
    protected double score(U user, Tuple2od<I> item)
    {
        return lambda*recStats.norm(item.v1, item.v2) + (1.0 - lambda)*novStats.norm(item.v1, novMap.get(user).get(item.v1));
    }

    /**
     * Finds the novelty score for a user-item pair.
     * @param user the target user.
     * @param item the candidate item (with its score).
     * @return the novelty value for the item.
     */
    protected abstract double nov(U user, Tuple2od<I> item);
}
