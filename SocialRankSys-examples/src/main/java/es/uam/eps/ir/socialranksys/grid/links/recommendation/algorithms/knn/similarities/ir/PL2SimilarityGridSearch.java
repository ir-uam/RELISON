/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.ir;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.SimilarityFunction;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.SimilarityGridSearch;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.knn.similarities.ir.PL2Similarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.SimilarityIdentifiers.PL2;

/**
 * Grid search for the vector cosine similarity.
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @param <U> Type of the users.
 */
public class PL2SimilarityGridSearch<U> implements SimilarityGridSearch<U>
{
    /**
     * Identifier for the selection of neighbors for the target user.
     */
    private final String USEL = "uSel";
    /**
     * Identifier for the selection of neighbors for the neighbor user.
     */
    private final String VSEL = "vSel";
    /**
     * Identifier for the selection of neighbors of the common neighbors between both users.
     */
    private final String LAMBDA = "lambda";
    
    @Override
    public Map<String, SimilarityFunction<U>> grid(Grid grid)
    {
        Map<String, SimilarityFunction<U>> sims = new HashMap<>();
        
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<Double> lambdas = grid.getDoubleValues(LAMBDA);
        if(uSels == null || uSels.isEmpty() || vSels == null || vSels.isEmpty() || lambdas == null || lambdas.isEmpty())
        {
            return sims;
        }
        
        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                lambdas.forEach(lambda ->
                    sims.put(PL2 + "_" + uSel + "_" + vSel + "_" + lambda, (FastGraph<U> graph, FastPreferenceData<U,U> prefData) ->
                       new PL2Similarity(graph, uSel, vSel, lambda)))));
        
        return sims;
    }

    @Override
    public Map<String, Supplier<Similarity>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Similarity>> sims = new HashMap<>();
        
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<Double> lambdas = grid.getDoubleValues(LAMBDA);
        if(uSels == null || uSels.isEmpty() || vSels == null || vSels.isEmpty() || lambdas == null || lambdas.isEmpty())
        {
            return sims;
        }

        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                lambdas.forEach(lambda ->
                    sims.put(PL2 + "_" + uSel + "_" + vSel + "_" + lambda, () ->
                        new PL2Similarity(graph, uSel, vSel, lambda)))));
        
        return sims;
    }
    
}
