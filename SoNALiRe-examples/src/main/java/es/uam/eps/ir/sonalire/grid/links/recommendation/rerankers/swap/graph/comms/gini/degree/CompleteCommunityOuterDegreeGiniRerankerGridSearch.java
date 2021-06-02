/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.GlobalRerankerFunction;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.RerankerGridSearch;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.global.GlobalReranker;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.global.swap.comm.gini.degree.OuterCompleteCommunityDegreeGiniComplement;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.RerankerIdentifiers.OUTERCDEGREEGINI;


/**
 * Grid search for a reranker that reduces the Gini index of the degrees of the communities (restricted
 * to links between communities). It also powers the presence of links between communities.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * 
 */
public class CompleteCommunityOuterDegreeGiniRerankerGridSearch<U> implements RerankerGridSearch<U>
{
    /**
     * Identifier for the parameter that takes the trade-off between relevance and diversity.
     */
    private final String LAMBDA = "lambda";
    
    /**
     * Identifier for the neighborhood selection.
     */
    private final String ORIENTATION = "orientation";
    
    /**
     * Identifier for the field that indicates if autoloops are considered or not.
     */
    private final String AUTOLOOPS = "autoloops";

    @Override
    public Map<String, Supplier<GlobalReranker<U, U>>> grid(Grid grid, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> comms)
    {
        Map<String, Supplier<GlobalReranker<U,U>>> rerankers = new HashMap<>();
        
        grid.getDoubleValues(LAMBDA).forEach(lambda ->
            grid.getOrientationValues(ORIENTATION).forEach(orient ->
                grid.getBooleanValues(AUTOLOOPS).forEach(autoloop ->
                    rerankers.put(OUTERCDEGREEGINI + "-" + orient + "-" + (autoloop ? "autoloops" : "noautoloops") + "-" + lambda, () ->
                        new OuterCompleteCommunityDegreeGiniComplement<>(lambda, cutoff, norm, graph, comms, autoloop, orient))
                )
            )
        );
        
        return rerankers;
    }

    @Override
    public Map<String, GlobalRerankerFunction<U>> grid(Grid grid)
    {
        Map<String, GlobalRerankerFunction<U>> rerankers = new HashMap<>();

        grid.getDoubleValues(LAMBDA).forEach(lambda ->
            grid.getOrientationValues(ORIENTATION).forEach(orient ->
                grid.getBooleanValues(AUTOLOOPS).forEach(autoloop ->
                    rerankers.put(OUTERCDEGREEGINI + "-" + orient + "-" + (autoloop ? "autoloops" : "noautoloops") + "-" + lambda, (cutoff, norm, graph, comms) ->
                        new OuterCompleteCommunityDegreeGiniComplement<>(lambda, cutoff, norm, graph, comms, autoloop, orient))
                )
            )
        );

        return rerankers;
    }
    
}
