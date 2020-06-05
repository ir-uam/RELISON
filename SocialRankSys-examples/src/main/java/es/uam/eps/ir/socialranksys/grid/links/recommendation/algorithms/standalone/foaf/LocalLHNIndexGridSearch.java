/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.foaf;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.foaf.LocalLHNIndex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers.LOCALLHN;

/**
 * Grid search generator for local Leicht-Holme-Newman algorithm (term-based version).
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class LocalLHNIndexGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for the orientation of the target user neighborhood
     */
    private static final String USEL = "uSel";
    /**
     * Identifier for the orientation of the target user neighborhood
     */
    private static final String VSEL = "vSel";

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid) 
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();

        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        

        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                recs.put(LOCALLHN + "_" + uSel + "_" + vSel, (graph, prefData) ->
                    new LocalLHNIndex<>(graph, uSel, vSel))
            )
        );

        return recs;    
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData) 
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();

        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        

        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                recs.put(LOCALLHN + "_" + uSel + "_" + vSel, () ->
                    new LocalLHNIndex<>(graph, uSel, vSel))
            )
        );

        return recs;    
    }
}