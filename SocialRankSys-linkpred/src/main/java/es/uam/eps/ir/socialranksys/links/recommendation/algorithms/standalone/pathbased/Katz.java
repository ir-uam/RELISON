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
import org.jblas.DoubleMatrix;
import org.jblas.Solve;

/**
 * Katz algorithm.
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Katz<U> extends MatrixBasedRecommender<U>
{
    private final double b;

    /**
     * Constructor.
     *
     * @param graph A fast graph representing the social network.
     */
    public Katz(FastGraph<U> graph, double b)
    {
        super(graph);
        this.b = b;
        this.matrix = this.getMatrix();
    }

    /**
     * Obtains the matrix for the recommendation.
     * @return the matrix for the recommendation.
     */
    protected DoubleMatrix getMatrix()
    {
        DoubleMatrix matrix = graph.getJBLASAdjacencyMatrix(EdgeOrientation.OUT);
        DoubleMatrix eye = DoubleMatrix.eye(matrix.columns);
        eye = eye.add(matrix.mul(-b));
        return Solve.solve(eye, DoubleMatrix.eye(matrix.columns));
    }
}
