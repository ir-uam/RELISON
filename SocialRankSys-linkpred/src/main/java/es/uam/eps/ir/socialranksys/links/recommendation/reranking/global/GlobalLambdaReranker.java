/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global;

import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import org.ranksys.core.util.tuples.Tuple2id;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Re-ranker. Changes the position of items in a list of recommendations to optimize global criteria
 * other than relevance. Previous added recommendations are considered when computing the global value
 * we want to optimize.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 */
public abstract class GlobalLambdaReranker<U,I> extends GlobalGreedyReranker<U,I>
{
    /**
     * True if it is necessary to normalize the original and novelty scores.
     */
    protected final boolean norm;
    /**
     * Trade-off between the original and the novelty scores.
     */
    protected final double lambda;
    /**
     * Statistics for the original scores.
     */
    protected Stats recStats;
    /**
     * Statistics for the novelty scores
     */
    protected Stats novStats;
    /**
     * For each pair user-item, their associated novelty
     */
    private Map<U, Map<I, Double>> novMap;
    
    /**
     * Constructor.
     * @param lambda Trade-off between the original and the novelty scores.
     * @param cutOff Maximum size of the definitive ranking.
     * @param norm True if it is necessary to normalize the original and novelty scores
     */
    public GlobalLambdaReranker(double lambda ,int cutOff, boolean norm)
    {
        super(cutOff);
        this.norm = norm;
        this.lambda = lambda;
    }
    
    @Override
    protected Tuple2oo<U, Tuple2id> selectRecommendation(Map<U, List<Tuple2od<I>>> remainingItems)
    {
        this.recStats = new Stats();
        this.novStats = new Stats();
        this.novMap = new HashMap<>();
        
        remainingItems.keySet().forEach(user -> 
        {
            novMap.put(user, new HashMap<>());
            remainingItems.get(user).forEach(item -> 
            {
                recStats.accept(item.v2);
                double nov = this.nov(user, item);
                novStats.accept(nov);
                novMap.get(user).put(item.v1, nov);
            });
        });
        return super.selectRecommendation(remainingItems);
    }
    
    @Override
    protected double score(U user, Tuple2od<I> item)
    {
        return lambda*this.norm(item.v2, this.recStats) + (1.0 - lambda)*this.norm(novMap.get(user).get(item.v1), novStats);
        
    }
    
    protected abstract double nov(U user, Tuple2od<I> item);
    
    
    protected double norm(double value, Stats stats)
    {
        if(!norm)
            return value;
        else
            return (value - stats.getMin())/(stats.getMax() - stats.getMin());
    }
    
    
}
