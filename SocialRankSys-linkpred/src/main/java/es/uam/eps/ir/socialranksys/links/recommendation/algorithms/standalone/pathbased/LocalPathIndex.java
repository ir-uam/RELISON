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

/**
 * Local path index recommender.
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class LocalPathIndex<U> extends MatrixBasedRecommender<U>
{
    private final double beta;
    private final double k;

    /**
     * Constructor.
     *
     * @param graph A fast graph representing the social network.
     */
    public LocalPathIndex(FastGraph<U> graph, double beta, int k)
    {
        super(graph);
        this.beta = beta;
        this.k = k;
        this.matrix = this.getMatrix();

    }

    @Override
    protected DoubleMatrix getMatrix()
    {
        DoubleMatrix adj = graph.getJBLASAdjacencyMatrix(EdgeOrientation.OUT);
        DoubleMatrix matrix = DoubleMatrix.zeros(adj.rows, adj.columns);
        DoubleMatrix aux = DoubleMatrix.eye(adj.rows);
        aux.mmuli(adj);
        double auxBeta = 1.0;
        for(int i = 2; i <= k; ++i)
        {
            matrix = matrix.add(aux.mmuli(adj).mul(auxBeta));
            auxBeta *= beta;
        }

        return matrix;
    }

}
