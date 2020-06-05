/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.RerankerGridSearch;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.GlobalReranker;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.graph.DegreeGiniReranker;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.RerankerIdentifiers.DEGREEGINICOMPL;


/**
 * Grid search for a reranker that reduces the Gini index of the degrees.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * 
 */
public class DegreeGiniComplementRerankerGridSearch<U> implements RerankerGridSearch<U>
{
    /**
     * Maximum number of edges in the definitive ranking
     */
    private final int cutoff;
    /**
     * Indicates if scores have to be normalized
     */
    private final boolean norm;
    /**
     * Training graph.
     */
    private final Graph<U> graph;

    /**
     * Indicates if the normalization is done by ranking or by score.
     */
    private final boolean rank;
    
    /**
     * Identifier for the parameter that takes the trade-off between relevance and diversity.
     */
    private final String LAMBDA = "lambda";
    
    /**
     * Identifier for the neighborhood selection.
     */
    private final String ORIENTATION = "orientation";
    
    /**
     * Constructor.
     * @param cutoff The cutoff to apply to the reranker.
     * @param norm true if the scores have to be normalized or not.
     * @param graph the training graph.
     * @param rank true if the normalization is by ranking or false if it is done by score
     */
    public DegreeGiniComplementRerankerGridSearch(int cutoff, boolean norm, boolean rank, Graph<U> graph)
    {
        this.cutoff = cutoff;
        this.norm = norm;
        this.graph = graph;
        this.rank = rank;
    }
    
    @Override
    public Map<String, Supplier<GlobalReranker<U, U>>> grid(Grid grid)
    {
        Map<String, Supplier<GlobalReranker<U,U>>> rerankers = new HashMap<>();
        
        grid.getDoubleValues(LAMBDA).forEach(lambda ->
            grid.getOrientationValues(ORIENTATION).forEach(orient ->
                rerankers.put(DEGREEGINICOMPL + "-" + orient + "-" + lambda, () ->
                    new DegreeGiniReranker<>(lambda, cutoff, norm, rank, graph, orient))
            )
        );
        
        return rerankers;
    }
    
}
