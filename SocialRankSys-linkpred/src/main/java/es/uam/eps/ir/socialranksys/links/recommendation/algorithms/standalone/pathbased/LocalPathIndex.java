/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.pathbased;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.GlobalMatrixBasedRecommender;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.LinkedSparseMatrix;
import org.jblas.DoubleMatrix;

/**
 * Local path index recommender.
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class LocalPathIndex<U> extends GlobalMatrixBasedRecommender<U>
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
        this.matrix = getMatrix();
    }

    @Override
    protected double[][] getJBLASMatrix()
    {
        double[][] adj = graph.getAdjacencyMatrix(EdgeOrientation.OUT);
        DoubleMatrix adjM = new DoubleMatrix(adj);
        DoubleMatrix matrix = DoubleMatrix.zeros(adjM.rows, adjM.columns);
        DoubleMatrix aux = DoubleMatrix.eye(adjM.rows);
        aux.mmuli(adjM);
        double auxBeta = 1.0;
        for(int i = 2; i <= k; ++i)
        {
            matrix = matrix.add(aux.mmuli(adjM).mul(auxBeta));
            auxBeta *= beta;
        }

        return matrix.toArray2();
    }

    @Override
    protected double[][] getCOLTMatrix()
    {
        double[][] adj = graph.getAdjacencyMatrix(EdgeOrientation.OUT);
        DoubleMatrix2D adjM = new SparseDoubleMatrix2D(adj);
        DoubleMatrix2D matrix = new SparseDoubleMatrix2D(adjM.rows(), adjM.columns());
        DoubleMatrix2D aux = DoubleFactory2D.sparse.identity(adjM.rows());

        Algebra alg = new Algebra();
        aux = alg.mult(aux, adjM);
        double auxBeta = 1.0;
        for(int i = 2; i <= k; ++i)
        {
            double currBeta = auxBeta;
            matrix = matrix.assign(aux, (x,y) -> x+currBeta*y);
            auxBeta *= beta;
        }

        return matrix.toArray();
    }

    @Override
    protected double[][] getMTJMatrix()
    {
        double[][] adj = graph.getAdjacencyMatrix(EdgeOrientation.OUT);
        Matrix aux = new DenseMatrix(adj);
        Matrix lsm = new LinkedSparseMatrix(this.uIndex.numUsers(), this.uIndex.numUsers());
        for(int i = 2; i <= k; ++i)
        {
            Matrix aux2 = new LinkedSparseMatrix(this.uIndex.numUsers(), this.uIndex.numUsers());
            lsm.mult(beta, aux, aux2);
            aux = aux2;
            lsm.add(aux);
        }
        double[][] matrix = new double[numUsers()][numItems()];
        for(int i = 0; i < lsm.numRows(); ++i)
            for(int j = 0; j < lsm.numColumns(); ++j)
                matrix[i][j] = lsm.get(i,j);
        return matrix;
    }



}
