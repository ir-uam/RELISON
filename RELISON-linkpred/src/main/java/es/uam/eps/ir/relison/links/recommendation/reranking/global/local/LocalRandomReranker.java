/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.local;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Random reranker. Reorders recommendations (gradually) at random.
 +
 * @param <U> type of the users.
 * @param <I> type of the items.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class LocalRandomReranker<U,I> extends LocalLambdaReranker<U,I>
{
    /**
     * Random seed.
     */
    private final int seed;
    
    /**
     * A map containing the random scores for the different user-item pairs.
     */
    private final Map<U, Map<I, Double>> map;

    /**
     * Constructor.
     * @param cutOff    maximum length of the definitive ranking.
     * @param lambda    trade-off between the original and novelty scores
     * @param norm      the normalization strategy.
     */
    public LocalRandomReranker(int cutOff, double lambda, Supplier<Normalizer<I>> norm)
    {
        this(cutOff, lambda, norm, 0);
    }

    /**
     * Constructor.
     * @param cutOff    maximum length of the definitive ranking.
     * @param lambda    trade-off between the original and novelty scores
     * @param norm      the normalization strategy.
     * @param seed      the random seed.
     */
    public LocalRandomReranker(int cutOff, double lambda, Supplier<Normalizer<I>> norm, int seed)
    {
        super(cutOff, lambda, norm, seed);
        this.seed = seed;
        this.map = new HashMap<>();
    }

    @Override
    public Stream<Recommendation<U, I>> rerankRecommendations(Stream<Recommendation<U, I>> recommendation, int maxLength)
    {
        Random rng = new Random(this.seed);
        List<Recommendation<U,I>> recommendations = recommendation.collect(Collectors.toCollection(ArrayList::new));

        this.map.clear();
        for(Recommendation<U,I> rec : recommendations)
        {
            U u = rec.getUser();
            map.put(u, new HashMap<>());
            for(Tuple2od<I> tuple : rec.getItems())
            {
                map.get(u).put(tuple.v1, rng.nextDouble());
            }
        }
        
        return super.rerankRecommendations(recommendations.stream(), maxLength);
    }
    
    @Override
    protected double nov(U u, Tuple2od<I> itemValue)
    {
        return this.map.get(u).get(itemValue.v1);
    }

    @Override
    protected void update(U user, Tuple2od<I> bestItemValue)
    {
        
    }

    @Override
    protected void update(Recommendation<U, I> reranked) 
    {
        
    }

    
    
}
