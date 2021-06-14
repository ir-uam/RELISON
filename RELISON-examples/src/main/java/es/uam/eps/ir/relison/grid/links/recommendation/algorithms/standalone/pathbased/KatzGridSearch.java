/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.algorithms.standalone.pathbased;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmIdentifiers;
import es.uam.eps.ir.relison.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.pathbased.Katz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Grid search generator for Katz algorithm.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see Katz
 */
public class KatzGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for the dump factor of longer length paths.
     */
    private static final String B = "b";
    /**
     * Identifier for the orientation selection for the adjacency matrix.
     */
    private static final String ORIENT = "orientation";

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U, U>>> recs = new HashMap<>();

        List<Double> bs = grid.getDoubleValues(B);
        if(grid.getOrientationValues().containsKey(ORIENT))
        {
            List<EdgeOrientation> orients = grid.getOrientationValues(ORIENT);
            orients.forEach(orient ->
                bs.forEach(b ->
                    recs.put(AlgorithmIdentifiers.KATZ + "_" + orient + "_" + b, () -> new Katz<>(graph, b, orient))
                )
            );
        }
        else
        {
            bs.forEach(b ->
                recs.put(AlgorithmIdentifiers.KATZ + "_" + b, () -> new Katz<>(graph, b)));
        }

        return recs;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();

        List<Double> bs = grid.getDoubleValues(B);

        if(grid.getOrientationValues().containsKey(ORIENT))
        {
            List<EdgeOrientation> orients = grid.getOrientationValues(ORIENT);
            orients.forEach(orient ->
                bs.forEach(b ->
                   recs.put(AlgorithmIdentifiers.KATZ + "_" + orient + "_" + b, (graph, prefData) -> new Katz<>(graph, b, orient))
                )
            );
        }
        else
        {
            bs.forEach(b ->
                recs.put(AlgorithmIdentifiers.KATZ + "_" + b, (graph, prefData) -> new Katz<>(graph, b)));
        }

        return recs;
    }

}
