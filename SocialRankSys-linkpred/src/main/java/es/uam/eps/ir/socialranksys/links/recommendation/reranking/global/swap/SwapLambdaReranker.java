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
import es.uam.eps.ir.ranksys.core.util.Stats;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;

/**
 * Generalization of the local rerankers for processing several of them in a row.
 * @author Javier Sanz-Cruzado Puig.
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 */
public abstract class SwapLambdaReranker<U,I> extends SwapGreedyReranker<U,I> 
{
    /**
     * Statistics for the original scores.
     */
    protected Stats relStats;
    /**
     * Statistics for the novelty scores.
     */
    protected Stats novStats;
    /**
     * Relevance ranking
     */
    protected Map<I,Integer> relRanking;
    /**
     * Novelty ranking
     */
    protected Map<I,Integer> novRanking;
    /**
     * Novelty of the items.
     */
    protected Object2DoubleMap<I> novMap;
    /**
     * Trade-off between original and novelty scores.
     */
    private final double lambda;
    /**
     * Indicates if both original and novelty scores have to be normalized.
     */
    private final boolean norm;
    /**
     * Indicates if the normalization is by ranking (true) or by score (false)
     */
    private final boolean rank;
    
    /**
     * Constructor.
     * @param cutOff Maximum length of the recommendation ranking.
     * @param lambda Trade-off between original and novelty scores.
     * @param norm true if scores have to be normalized, false if not.
     * @param rank Indicates if the normalization is by ranking (true) or by score (false)
     */
    public SwapLambdaReranker(double lambda, int cutOff, boolean norm, boolean rank)
    {
        super(cutOff);
        this.lambda = lambda;
        this.norm = norm;
        this.rank = rank;
    }

    @Override
    protected int selectItem(U u, IntSortedSet remainingI, Tuple2od<I> oldValue, List<Tuple2od<I>> list)
    {
        novMap = new Object2DoubleOpenHashMap<>();
        relStats = new Stats();
        novStats = new Stats();
        relRanking = new HashMap<>();
        novRanking = new HashMap<>();
        
        Comparator<Tuple2od<I>> comp = (Tuple2od<I> t, Tuple2od<I> t1) ->
        {
            double val = Double.compare(t1.v2, t.v2);
                        
            if(val == 0.0)
            {
                val = ((Comparable<I>)t1.v1).compareTo(t.v1);
            }

            return Double.compare(val, 0.0);
        };
        
        TreeSet<Tuple2od<I>> auxRelRank = new TreeSet<>(comp);
        TreeSet<Tuple2od<I>> auxNovRank = new TreeSet<>(comp);
        remainingI.stream().mapToInt(i->i).forEach(i ->
        {
            Tuple2od<I> itemValue = list.get(i);
            double nov = this.nov(u, itemValue, oldValue);
            novMap.put(itemValue.v1, nov);
            relStats.accept(itemValue.v2);
            novStats.accept(nov);
            auxRelRank.add(new Tuple2od<>(itemValue.v1, itemValue.v2));
            auxNovRank.add(new Tuple2od<>(itemValue.v1, nov));
            
        });
        
        relStats.accept(oldValue.v2);
        novStats.accept(this.globalvalue);
        auxRelRank.add(new Tuple2od<>(oldValue.v1, oldValue.v2));
        auxNovRank.add(new Tuple2od<>(oldValue.v1, this.globalvalue));
 
        int size = auxRelRank.size();
        int i = 0;
        while(!auxRelRank.isEmpty() && !auxNovRank.isEmpty())
        {
            Tuple2od<I> rel = auxRelRank.pollFirst();
            assert rel != null;
            relRanking.put(rel.v1, i);

            Tuple2od<I> nov = auxNovRank.pollFirst();
            assert nov != null;
            novRanking.put(nov.v1, i);

            ++i;
        }
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
        if(rank)
        {
            return (1-lambda) * norm(iv.v1, relRanking) + lambda * norm(iv.v1, novRanking);
        }
        else
        {
            return (1 - lambda) * norm(iv.v2, relStats) + lambda * norm(novMap.getDouble(iv.v1), novStats);
        }
    }

    @Override
    protected double valuetop(Tuple2od<I> iv)
    {
        if(rank)
        {
            return (1-lambda) * norm(iv.v1, relRanking) + lambda * norm(iv.v1, novRanking);
        }
        else
        {
            return (1 - lambda) * norm(iv.v2, relStats) + lambda * norm(this.globalvalue, novStats);
        }
        
    }

    /**
     * Computes the novelty score of an edge.
     * @param u the user
     * @param newValue the value of the original candidate item.
     * @param oldValue the value of the new candidate item.
     * @return the novelty value
     */
    protected abstract double nov(U u, Tuple2od<I> newValue, Tuple2od<I> oldValue);

    /**
     * Normalizes the scores using the min-max score
     * @param score the score.
     * @param stats the statistics.
     * @return the normalized value.
     */
    private double norm(double score, Stats stats) 
    {
        if(norm)
        {
            return (score - stats.getMin())/(stats.getMax() - stats.getMin());
        }
        return score;
    }
    
    /**
     * Normalizes the scores using the rank-sim score
     * @param i element to rank
     * @param stats the map containing the ranking positions
     * @return the normalized value
     */
    private double norm(I i, Map<I, Integer> stats)
    {
        double pos = stats.get(i);
        double size = stats.size();
        return 1.0 - pos/size;
    }

    
    
    
}
