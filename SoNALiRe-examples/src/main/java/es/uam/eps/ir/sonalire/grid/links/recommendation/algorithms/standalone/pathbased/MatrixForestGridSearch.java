/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.pathbased;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.AlgorithmIdentifiers;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.pathbased.MatrixForest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Grid search generator for Matrix Forest algorithm.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see MatrixForest
 */
public class MatrixForestGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for the importance of the Laplacian matrix.
     */
    private static final String ALPHA = "alpha";
    /**
     * Identifier for the orientation selection for the adjacency matrix.
     */
    private static final String ORIENT = "orient";

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U, U>>> recs = new HashMap<>();

        List<Double> alphas = grid.getDoubleValues(ALPHA);
        if(grid.getOrientationValues().containsKey(ORIENT))
        {
            List<EdgeOrientation> orients = grid.getOrientationValues(ORIENT);
            orients.forEach(orient ->
                alphas.forEach(alpha ->
                    recs.put(AlgorithmIdentifiers.MATRIXFOREST + "_" + orient + "_" + alpha, () -> new MatrixForest<>(graph, alpha, orient))
                )
            );
        }
        else
        {
            alphas.forEach(alpha ->
                recs.put(AlgorithmIdentifiers.MATRIXFOREST + "_" + alpha, () -> new MatrixForest<>(graph, alpha)));
        }

        return recs;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();

        List<Double> alphas = grid.getDoubleValues(ALPHA);

        if(grid.getOrientationValues().containsKey(ORIENT))
        {
            List<EdgeOrientation> orients = grid.getOrientationValues(ORIENT);
            orients.forEach(orient ->
                alphas.forEach(alpha ->
                   recs.put(AlgorithmIdentifiers.MATRIXFOREST + "_" + orient + "_" + alpha, (graph, prefData) -> new MatrixForest<>(graph, alpha, orient))
                )
            );
        }
        else
        {
            alphas.forEach(alpha ->
                recs.put(AlgorithmIdentifiers.MATRIXFOREST + "_" + alpha, (graph, prefData) -> new MatrixForest<>(graph, alpha)));
        }

        return recs;
    }

}
