/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.local;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.RerankerGridSearch;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.GlobalReranker;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.LocalRandomReranker;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.RerankerIdentifiers.RANDOM;


/**
 * Grid search for a random reranker.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class RandomRerankerGridSearch<U> implements RerankerGridSearch<U>
{
    /**
     * Identifier for the parameter that takes the trade-off between relevance and diversity.
     */
    private final String LAMBDA = "lambda";
    
    /**
     * Maximum number of edges in the definitive ranking
     */
    private final int cutoff;
    /**
     * Indicates if scores have to be normalized
     */
    private final boolean norm;
    /**
     * Indicates if the normalization is done by ranking or by score.
     */
    private final boolean rank;
    
    public RandomRerankerGridSearch(int cutoff, boolean norm, boolean rank)
    {
        this.cutoff = cutoff;
        this.norm = norm;
        this.rank = rank;
        
    }
    @Override
    public Map<String, Supplier<GlobalReranker<U, U>>> grid(Grid grid)
    {
        Map<String, Supplier<GlobalReranker<U,U>>> rerankers = new HashMap<>();
        
        grid.getDoubleValues(LAMBDA).forEach(lambda ->
             rerankers.put(RANDOM + "-" + lambda, () -> new LocalRandomReranker<>(cutoff, lambda, norm, rank)));
        
        return rerankers;
    }
    
}
