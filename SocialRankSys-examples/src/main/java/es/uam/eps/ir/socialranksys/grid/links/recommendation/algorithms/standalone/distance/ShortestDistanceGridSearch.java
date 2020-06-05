/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.distance;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.distance.ShortestDistance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers.DISTANCE;

/**
 * Grid search generator for the shortest distance algorithm.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class ShortestDistanceGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for the orientation of the paths from the origin to the source
     */
    private static final String ORIENTATION = "orientation";

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();

        List<EdgeOrientation> orientations = grid.getOrientationValues(ORIENTATION);
        
        orientations.forEach(orient ->
            recs.put(DISTANCE + "_" + orient, (graph, prefData) -> new ShortestDistance<>(graph, orient))
        );

        return recs;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();

        List<EdgeOrientation> orientations = grid.getOrientationValues(ORIENTATION);
        
        orientations.forEach(orient ->
            recs.put(DISTANCE + "_" + orient, () -> new ShortestDistance<>(graph, orient))
        );
 
        return recs;
    }
}