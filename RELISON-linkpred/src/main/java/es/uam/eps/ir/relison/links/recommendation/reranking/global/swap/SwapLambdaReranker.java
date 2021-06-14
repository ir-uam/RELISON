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
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.List;
import java.util.function.Supplier;

/**
 * Abstract implementation of the greedy swap strategy that allows to optimize
 * at the same time the accuracy of the system (given by the original ranking) and the
 * global property we want to optimize.
 *
 * We keep for that a trade-off between the original ranking and the new one.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 */
public abstract class SwapLambdaReranker<U,I> extends SwapGreedyReranker<U,I> 
{
    /**
     * Statistics for the original scores.
     */
    protected Normalizer<I> relStats;
    /**
     * Statistics for the novelty scores.
     */
    protected Normalizer<I> novStats;
    /**
     * Novelty of the items.
     */
    protected Object2DoubleMap<I> novMap;
    /**
     * Trade-off between original and novelty scores.
     */
    private final double lambda;
    /**
     * The normalization algorithm to apply.
     */
    private final Supplier<Normalizer<I>> norm;

    /**
     * Constructor.
     * @param cutOff    maximum length of the recommendation ranking.
     * @param lambda    trade-off between original and novelty scores.
     * @param norm      the normalization scheme to apply.
     */
    public SwapLambdaReranker(double lambda, int cutOff, Supplier<Normalizer<I>> norm)
    {
        super(cutOff);
        this.lambda = lambda;
        this.norm = norm;
    }

    @Override
    protected int selectItem(U u, IntSortedSet remainingI, Tuple2od<I> oldValue, List<Tuple2od<I>> list)
    {
        novMap = new Object2DoubleOpenHashMap<>();
        relStats = norm.get();
        novStats = norm.get();
                
        remainingI.intStream().forEach(i ->
        {
            Tuple2od<I> itemValue = list.get(i);
            double nov = this.nov(u, itemValue, oldValue);
            novMap.put(itemValue.v1, nov);
            relStats.add(itemValue.v1, itemValue.v2);
            novStats.add(itemValue.v1, nov);
        });
        
        relStats.add(oldValue.v1, oldValue.v2);
        novStats.add(oldValue.v1, this.globalvalue);

        return super.selectItem(u, remainingI, oldValue, list);
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
        return (1.0-lambda) * relStats.norm(iv.v1, iv.v2) + lambda*novStats.norm(iv.v1, novMap.getDouble(iv.v1));
    }

    @Override
    protected double valuetop(Tuple2od<I> iv)
    {
        return (1.0-lambda) * relStats.norm(iv.v1, iv.v2) + lambda * novStats.norm(iv.v1, this.globalvalue);
    }

    /**
     * Computes the novelty score of an edge.
     * @param u         the user
     * @param newValue  the value of the new candidate item.
     * @param oldValue  the value of the original candidate item.
     * @return the novelty value
     */
    protected abstract double nov(U u, Tuple2od<I> newValue, Tuple2od<I> oldValue);
}
