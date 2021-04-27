/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.randomwalks;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.randomwalk.PropFlow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers.PROPFLOW;

/**
 * Grid search generator for PropFlow algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.randomwalk.PropFlow
 */
public class PropFlowGridSearch<U> implements AlgorithmGridSearch<U>
{   
    /**
     * Identifier for the teleport parameter
     */
    private final static String MAXLENGTH = "maxLength";
    /**
     * Identifier for the edge orientation
     */
    private final static String ORIENTATION = "orientation";
    /**
     * Identifier for indicating whether the result is weighted or not.
     */
    private static final String WEIGHTED = "weighted";

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        
        List<Integer> maxLengths = grid.getIntegerValues(MAXLENGTH);
        List<EdgeOrientation> orientations = grid.getOrientationValues(ORIENTATION);
        List<Boolean> weighted = grid.getBooleanValues(WEIGHTED);

        if(weighted.isEmpty())
        {
            maxLengths.forEach(maxLength ->
                orientations.forEach(orientation ->
                    recs.put(PROPFLOW + "_" + orientation + "_" + maxLength, (graph, prefData) ->
                        new PropFlow<>(graph, maxLength, orientation))
                )
            );
        }
        else
        {
            maxLengths.forEach(maxLength ->
                orientations.forEach(orientation ->
                    weighted.forEach(weight ->
                        recs.put(PROPFLOW + "_" + (weight ? "wei" : "unw") + "_" + orientation + "_" + maxLength, new RecommendationAlgorithmFunction<>()
                        {
                            @Override
                            public Recommender<U, U> apply(FastGraph<U> graph, FastPreferenceData<U, U> prefData)
                            {
                                return new PropFlow<>(graph, maxLength, orientation);
                            }

                            @Override
                            public boolean isWeighted()
                            {
                                return weight;
                            }
                        })
                    )
                )
            );
        }
        return recs;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();
        
        List<Integer> maxLengths = grid.getIntegerValues(MAXLENGTH);
        List<EdgeOrientation> orientations = grid.getOrientationValues(ORIENTATION);
        maxLengths.forEach(maxLength ->
            orientations.forEach(orientation ->
                recs.put(PROPFLOW + "_" + orientation + "_" + maxLength, () ->
                    new PropFlow<>(graph, maxLength, orientation))
            )
        );
        return recs;
    }
    
}
