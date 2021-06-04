/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.pathbased;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.recommendation.algorithms.GlobalMatrixBasedRecommender;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.LinkedSparseMatrix;
import org.jblas.DoubleMatrix;

/**
 * Local path index recommender. It takes all paths of distances between 2 and k between the target and candidate
 * users, and weights them, so shortest paths are more important.
 *
 * <br>
 *     <b>References:</b>
 *      <ol>
 *          <li>L. Lü, C. Jin, T. Zhou. Similarity Index Based on Local Paths for Link Prediction of Complex Networks. Physical Review E 80(4): 046122 (2009)</li>
 *          <li>L. Lü, T. Zhou. Link Prediction in Complex Networks: A survey. Physica A 390(6), 1150-1170 (2011)</li>
 *      </ol>
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class LocalPathIndex<U> extends GlobalMatrixBasedRecommender<U>
{
    /**
     * Dampening factor.
     */
    private final double beta;
    /**
     * The maximum distance between users.
     */
    private final double k;
    /**
     * The orientation for the adjacency matrix.
     */
    private final EdgeOrientation orient;

    /**
     * Constructor. Takes the outgoing orientation by default.
     *
     * @param graph a fast graph representing the social network.
     * @param beta  the dampening factor.
     * @param k     the maximum distance between the target and candidate users (k greater or equal than 2)
     */
    public LocalPathIndex(FastGraph<U> graph, double beta, int k)
    {
        super(graph);
        this.beta = beta;
        this.k = k;
        this.orient = EdgeOrientation.OUT;
        this.matrix = getMatrix();
    }

    /**
     * Constructor.
     *
     * @param graph     a fast graph representing the social network.
     * @param beta      the dampening factor.
     * @param k         the maximum distance between the target and candidate users (k greater or equal than 2)
     * @param orient    the orientation for selecting the adjacency matrix.
     */
    public LocalPathIndex(FastGraph<U> graph, double beta, int k, EdgeOrientation orient)
    {
        super(graph);
        this.beta = beta;
        this.k = k;
        this.orient = orient;
        this.matrix = getMatrix();
    }

    @Override
    protected double[][] getJBLASMatrix()
    {
        double[][] adj = graph.getAdjacencyMatrix(orient);
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
