/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.pathbased;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;

/**
 * Katz algorithm.
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Katz<U> extends UserFastRankingRecommender<U>
{
    private final DoubleMatrix katzMatrix;
    /**
     * Constructor.
     *
     * @param graph A fast graph representing the social network.
     */
    public Katz(FastGraph<U> graph, double b)
    {
        super(graph);

        DoubleMatrix matrix = graph.getJBLASAdjacencyMatrix(EdgeOrientation.OUT);
        DoubleMatrix eye = DoubleMatrix.eye(matrix.columns);
        eye = eye.add(matrix.mul(-b));
        katzMatrix = Solve.solve(eye, DoubleMatrix.eye(matrix.columns));
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleMap scoresMap = new Int2DoubleOpenHashMap();
        for(int vidx = 0; vidx < katzMatrix.columns; ++vidx)
        {
            scoresMap.put(vidx, katzMatrix.get(uidx, vidx));
        }

        return scoresMap;
    }
}
