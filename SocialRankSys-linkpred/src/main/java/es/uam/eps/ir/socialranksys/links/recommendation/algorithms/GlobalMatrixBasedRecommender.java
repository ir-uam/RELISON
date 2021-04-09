/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.utils.matrix.MatrixLibrary;

/**
 * Contact recommendation algorithm that on operations over a global matrix.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class GlobalMatrixBasedRecommender<U> extends MatrixBasedRecommender<U>
{
    /**
     * The matrix.
     */
    protected double[][] matrix;

    /**
     * Constructor.
     *
     * @param graph     a fast graph representing the social network.
     * @param library   the matrix library to use.
     */
    public GlobalMatrixBasedRecommender(FastGraph<U> graph, MatrixLibrary library)
    {
        super(graph, library);
    }

    /**
     * Constructor.
     * @param graph a fast graph representing the social network.
     */
    public GlobalMatrixBasedRecommender(FastGraph<U> graph)
    {
        super(graph);
    }

    /**
     * Obtains the unique matrix for the system.
     * @return the matrix.
     */
    protected double[][] getMatrix()
    {
        if(matrix == null)
        {
            return switch (library)
            {
                case JBLAS -> matrix = getJBLASMatrix();
                case MTJ -> matrix = getMTJMatrix();
                case COLT -> matrix = getCOLTMatrix();
            };
        }
        return matrix;
    }

    /**
     * Obtains the matrix using the JBLAS library.
     * @return the matrix.
     */
    protected abstract double[][] getJBLASMatrix();
    /**
     * Obtains the matrix using the COLT library.
     * @return the matrix.
     */
    protected abstract double[][] getCOLTMatrix();
    /**
     * Obtains the matrix using the MTJ library.
     * @return the matrix.
     */
    protected abstract double[][] getMTJMatrix();

    @Override
    protected double[][] getCOLTMatrix(int uidx)
    {
        if(matrix == null) matrix = getCOLTMatrix();
        return matrix;
    }

    @Override
    protected double[][] getMTJMatrix(int uidx)
    {
        if(matrix == null) matrix = getMTJMatrix();
        return matrix;
    }

    @Override
    protected double[][] getJBLASMatrix(int uidx)
    {
        if(matrix == null) matrix = getJBLASMatrix();
        return matrix;
    }
}
