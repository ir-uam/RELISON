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
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.LUDecompositionQuick;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.GlobalMatrixBasedRecommender;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;

/**
 * Implementation of the matrix forest algorithm for contact recommendation. The score can be understood as the
 * ratio between the number of spanning divergent forests such that nodes u and v belong to the same divergent tree and
 * the total number of spanning divergent forests for the network.
 *
 * <p>
 *  <b>References:</b>
 *  <ol>
 *      <li>L. Lü, T. Zhou. Link prediction in complex networks. A survey. Physica A 390(6), pp. 1150-1170 (2011)</li>
 *      <li>P.Y. Chebotarev, E.V. Shamis. The Matrix-Forest Theorem and Measuring Relations in Small Social Groups. Automation and Remote Control 58(9), pp. 1505-1514 (1997)</li>
 *  </ol>
 * </p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MatrixForest<U> extends GlobalMatrixBasedRecommender<U>
{
    /**
     * Parameter controlling the importance of the Laplacian matrix.
     */
    private final double alpha;

    /**
     * The orientation for the Laplacian matrix.
     */
    private final EdgeOrientation orient;

    /**
     * Constructor. By default, considers the network as undirected.
     *
     * @param graph     A fast graph representing the social network.
     * @param alpha     parameter controlling the importance of the Laplacian matrix (greater than 0)
     */
    public MatrixForest(FastGraph<U> graph, double alpha)
    {
        super(graph);
        this.alpha = alpha;
        this.orient = EdgeOrientation.UND;
        this.matrix = this.getMatrix();
    }


    /**
     * Constructor.
     *
     * @param graph     A fast graph representing the social network.
     * @param alpha     parameter controlling the importance of the Laplacian matrix (greater than 0)
     * @param orient    orientation selection for the adjacency and Laplacian matrices.
     */
    public MatrixForest(FastGraph<U> graph, double alpha, EdgeOrientation orient)
    {
        super(graph);
        this.alpha = alpha;
        this.orient = orient;
        this.matrix = this.getMatrix();
    }

    @Override
    protected double[][] getJBLASMatrix()
    {
        // We first obtain the adjacency matrix
        double[][] adjAux = graph.getAdjacencyMatrix(orient);
        DoubleMatrix adj = new DoubleMatrix(adjAux);

        // Then, the degree matrix:
        DoubleMatrix sum = adj.rowSums();
        DoubleMatrix D = DoubleMatrix.zeros(numUsers());
        for(int i = 0; i < numUsers(); ++i)
        {
            D.put(i, i, sum.get(i));
        }

        // Now, the Laplacian matrix
        DoubleMatrix L = D.add(adj.mul(-1.0));
        // (I+alpha*L)
        DoubleMatrix aux = DoubleMatrix.eye(numUsers()).add(L.mul(alpha));

        // We invert the matrix, so we can find the matrix forest matrix.
        DoubleMatrix mf = Solve.solve(aux, DoubleMatrix.eye(numUsers()));
        return mf.toArray2();
    }

    @Override
    protected double[][] getCOLTMatrix()
    {
        // We first find the adjacency matrix
        double[][] adjAux = graph.getAdjacencyMatrix(orient);
        DoubleMatrix2D adj = new SparseDoubleMatrix2D(adjAux);

        // Afterwards, we find the degree matrix.
        DoubleMatrix1D rowSums = new SparseDoubleMatrix1D(adj.rows());
        rowSums.assign(1.0);

        Algebra alg = new Algebra();
        rowSums = alg.mult(adj, rowSums);

        DoubleMatrix2D D = new SparseDoubleMatrix2D(adj.rows(), adj.rows());
        for(int i = 0; i < adj.rows(); ++i)
        {
            D.setQuick(i,i, rowSums.get(i));
        }

        // And the laplacian matrix L
        DoubleMatrix2D L = D.assign(adj, (x,y) -> x - y);
        // (I+ alpha*L)
        DoubleMatrix2D aux = DoubleFactory2D.sparse.identity(adj.rows());
        aux.assign(L, (x,y) -> x + alpha*y);

        DoubleMatrix2D eye = DoubleFactory2D.sparse.identity(adj.rows());
        // We invert the matrix.
        LUDecompositionQuick lu = new LUDecompositionQuick();
        lu.decompose(aux);
        lu.solve(eye);
        return eye.toArray();
    }


    @Override
    protected  double[][] getMTJMatrix()
    {
        // We first find the adjacency matrix.
        double[][] adjAux = graph.getAdjacencyMatrix(orient);
        DenseMatrix matrix = new DenseMatrix(adjAux);

        // Now, the degree matrix:
        DenseMatrix degree = new DenseMatrix(numUsers(), numUsers());
        graph.getAllNodesIds().forEach(uidx -> degree.set(uidx, uidx, graph.degree(uidx2user(uidx), orient)));

        // We sum up the nodes:
        Matrix L = degree.add(matrix.scale(-1.0));
        // (I + alpha*L)
        Matrix eye = Matrices.identity(numUsers());
        Matrix aux = eye.add(L.scale(alpha));

        Matrix solve = Matrices.identity(this.numUsers());
        aux.solve(Matrices.identity(numUsers()), solve);

        double[][] defMatrix = new double[numUsers()][numUsers()];
        for (int i = 0; i < aux.numRows(); ++i)
            for (int j = 0; j < aux.numColumns(); ++j)
                defMatrix[i][j] = aux.get(i, j);
        return defMatrix;
    }
}
