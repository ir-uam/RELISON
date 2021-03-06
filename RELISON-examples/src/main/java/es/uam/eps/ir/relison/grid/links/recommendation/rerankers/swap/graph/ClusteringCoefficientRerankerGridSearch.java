/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.rerankers.swap.graph;

import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.recommendation.rerankers.GlobalRerankerFunction;
import es.uam.eps.ir.relison.grid.links.recommendation.rerankers.RerankerGridSearch;
import es.uam.eps.ir.relison.links.recommendation.reranking.global.GlobalReranker;
import es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.graph.ClusteringCoefficientReranker;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.links.recommendation.rerankers.RerankerIdentifiers.CLUSTCOEF;

/**
 * Grid search for a reranker that promotes the clustering coefficient in the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * 
 */
public class ClusteringCoefficientRerankerGridSearch<U> implements RerankerGridSearch<U>
{
    /**
     * Identifier for the parameter that takes the trade-off between relevance and diversity.
     */
    private final String LAMBDA = "lambda";

    @Override
    public Map<String, Supplier<GlobalReranker<U, U>>> grid(Grid grid, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> comms)
    {
        Map<String, Supplier<GlobalReranker<U,U>>> rerankers = new HashMap<>();
        
        grid.getDoubleValues(LAMBDA).forEach(lambda ->
            rerankers.put(CLUSTCOEF + "-" + lambda, () ->
                new ClusteringCoefficientReranker<>(lambda, cutoff, norm, graph))
        );
        
        return rerankers;
    }

    @Override
    public Map<String, GlobalRerankerFunction<U>> grid(Grid grid)
    {
        Map<String, GlobalRerankerFunction<U>> rerankers = new HashMap<>();

        grid.getDoubleValues(LAMBDA).forEach(lambda ->
            rerankers.put(CLUSTCOEF + "-" + lambda, (cutoff, norm, graph, comms) ->
                new ClusteringCoefficientReranker<>(lambda, cutoff, norm, graph))
        );

        return rerankers;
    }
    
}
