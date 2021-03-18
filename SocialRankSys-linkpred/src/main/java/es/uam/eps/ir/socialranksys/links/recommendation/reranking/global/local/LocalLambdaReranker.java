/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;
import java.util.function.Supplier;

/**
 * Generalization of local greedy reranking strategies, for processing several recommendations at a time.
 *
 * These rerankers, given a set of recommendations, sequentially process them one by one. Those
 * recommendations which are processed later are aware of the previously processed recommendations.
 *
 * They jointly optimize the accuracy and the novelty/diversity of the recommendations, by establishing a trade-off
 * between them.
 *
 * @param <U> type of the users.
 * @param <I> type of the items.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class LocalLambdaReranker<U,I> extends LocalGreedyReranker<U,I> 
{
    /**
     * Statistics for the original scores.
     */
    protected Normalizer<I> relStats;
    /**
     * Statistics for the novelty scores
     */
    protected Normalizer<I> novStats;
    /**
     * Map containing the novelty of the items.
     */
    protected Object2DoubleMap<I> novMap;
    /**
     * Trade-off between the original and novelty scores
     */
    private final double lambda;
    /**
     * True if the scores have to be normalized, false if not.
     */
    private final Supplier<Normalizer<I>> norm;
    
    /**
     * Constructor.
     * @param cutOff    maximum length of the definitive ranking.
     * @param lambda    trade-off between the original and novelty scores
     * @param norm      the normalization strategy.
     */
    public LocalLambdaReranker(int cutOff, double lambda, Supplier<Normalizer<I>> norm)
    {
        super(cutOff);
        this.lambda = lambda;
        this.norm = norm;
    }

    /**
     * Constructor.
     * @param cutOff    maximum length of the definitive ranking.
     * @param lambda    trade-off between the original and novelty scores
     * @param norm      the normalization strategy.
     * @param seed      the random seed.
     */
    public LocalLambdaReranker(int cutOff, double lambda, Supplier<Normalizer<I>> norm, int seed)
    {
        super(cutOff, seed);
        this.lambda = lambda;
        this.norm = norm;
    }
    
    @Override
    protected int[] rerankPermutation(Recommendation<U, I> rec, int maxLength)
    {
        if(lambda == 0.0) return getBasePerm(maxLength);
        else
            return super.rerankPermutation(rec, maxLength);
    }
    
    @Override
    protected double value(Tuple2od<I> iv)
    {
        return (1.0-lambda)*relStats.norm(iv.v1, iv.v2) + lambda*novStats.norm(iv.v1, novMap.getDouble(iv.v1));
    }

    @Override
    protected int selectItem(U u, IntSortedSet remainingI, List<Tuple2od<I>> list)
    {
        novMap = new Object2DoubleOpenHashMap<>();
        relStats = norm.get();
        novStats = norm.get();

        remainingI.intStream().forEach(i ->
        {
           Tuple2od<I> itemValue = list.get(i);
           double nov = this.nov(u, itemValue);
           novMap.put(itemValue.v1, nov);
           relStats.add(itemValue.v1, itemValue.v2);
           novStats.add(itemValue.v1, nov);;
        });

        return super.selectItem(u, remainingI, list);
        
    }

    /**
     * Novelty score.
     * @param u         the target user.
     * @param itemValue the recommended item and its recommendation score.
     * @return the novelty for this pair user-item.
     */
    protected abstract double nov(U u, Tuple2od<I> itemValue);
    
}
