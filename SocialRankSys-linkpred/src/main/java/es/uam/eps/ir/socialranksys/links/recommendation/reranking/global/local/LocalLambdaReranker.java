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
public abstract class LocalLambdaReranker<U,I> extends LocalGreedyReranker<U,I> 
{
    /**
     * Statistics for the original scores.
     */
    protected Stats relStats;
    /**
     * Statistics for the novelty scores
     */
    protected Stats novStats;
    /**
     * Map containing the novelty of the items.
     */
    protected Object2DoubleMap<I> novMap;
    /**
     * 
     */
    protected Map<I, Integer> relRanking;
    /**
     * 
     */
    protected Map<I, Integer> novRanking;
    /**
     * Trade-off between the original and novelty scores
     */
    private final double lambda;
    /**
     * True if the scores have to be normalized, false if not.
     */
    private final boolean norm;
    /**
     * Indicates if normalization is by ranking (true) or by score (false)
     */
    private final boolean rank;
    
    /**
     * Constructor.
     * @param cutOff Maximum length of the definitive ranking.
     * @param lambda Trade-off between the original and novelty scores
     * @param norm True if the scores have to be normalized, false if not.
     * @param rank Indicates if the normalization is by ranking (true) or by score (false)
     */
    public LocalLambdaReranker(int cutOff, double lambda, boolean norm, boolean rank)
    {
        super(cutOff);
        this.lambda = lambda;
        this.norm = norm;
        this.rank = rank;
    }
    
    /**
     * Constructor.
     * @param cutOff Maximum length of the definitive ranking.
     * @param lambda Trade-off between the original and novelty scores
     * @param norm True if the scores have to be normalized, false if not.
     * @param rank Indicates if the normalization is by ranking (true) or by score (false)
     * @param seed the random seed.
     */
    public LocalLambdaReranker(int cutOff, double lambda, boolean norm, boolean rank, int seed)
    {
        super(cutOff, seed);
        this.lambda = lambda;
        this.norm = norm;
        this.rank = rank;
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
            return (1-lambda) * norm(iv.v1, relRanking) + lambda*norm(iv.v1, novRanking);
        }
        else
        {
            return (1 - lambda) * norm(iv.v2, relStats) + lambda * norm(novMap.getDouble(iv.v1), novStats);
        }
    }
    
    

    @Override
    protected int selectItem(U u, IntSortedSet remainingI, List<Tuple2od<I>> list)
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
                val = ((Comparable<I>) t1.v1).compareTo(t.v1);
            }

            return Double.compare(val, 0.0);
        };
        
        TreeSet<Tuple2od<I>> auxRelRank = new TreeSet<>(comp);
        TreeSet<Tuple2od<I>> auxNovRank = new TreeSet<>(comp);

        remainingI.stream().mapToInt(i->i).forEach(i ->
        {
           Tuple2od<I> itemValue = list.get(i);
           double nov = this.nov(u, itemValue);
           novMap.put(itemValue.v1, nov);
           relStats.accept(itemValue.v2);
           novStats.accept(nov);
           auxRelRank.add(new Tuple2od<>(itemValue.v1, itemValue.v2));
           auxNovRank.add(new Tuple2od<>(itemValue.v1, nov));
        });
        
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
        
        return super.selectItem(u, remainingI, list);
        
    }

    /**
     * Novelty score
     * @param u User
     * @param itemValue Recommended item and its recommendation score
     * @return the novelty for this pair user-item.
     */
    protected abstract double nov(U u, Tuple2od<I> itemValue);

    /**
     * Normalizes the scores.
     * @param score The original score
     * @param stats The statistics
     * @return The normalized value.
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
