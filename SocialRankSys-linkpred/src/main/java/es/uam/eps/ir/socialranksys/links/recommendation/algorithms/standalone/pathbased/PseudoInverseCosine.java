/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.pathbased;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
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
 * Implementation of the pseudo inverse cosine algorithm for contact recommendation. This algorithm first computes the
 * pseudo inverse of the Laplacian matrix (obtaining, for each user, a vector representation).
 *
 * Then, it takes the cosine similarity (which is computed as the (u,v) coordinate of the matrix, divided by
 * the square root of the product of the (u,u) and (v,v) coordinates).
 *
 * <p>
 *  <b>References:</b>
 *  <ol>
 *      <li>F. Fouss, A. Pirotte, J-M. Renders, M. Saerens. Random-walk computatin of similarities between nodes of a graph with application to collaborative recommendation. IEEE TKDE 19(3), pp. 355-369 (2007)</li>
 *  </ol>
 * </p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class PseudoInverseCosine<U> extends GlobalMatrixBasedRecommender<U>
{
    /**
     * The orientation for the Laplacian matrix.
     */
    private final EdgeOrientation orient;

    /**
     * Constructor. By default, considers network as undirected.
     *
     * @param graph a fast graph representing the social network.
     */
    public PseudoInverseCosine(FastGraph<U> graph)
    {
        super(graph);
        this.orient = EdgeOrientation.UND;
        this.matrix = this.getMatrix();
    }

    /**
     * Constructor.
     *
     * @param graph  a fast graph representing the social network.
     * @param orient the orientation selection for the adjacency and Laplacian matrices.
     */
    public PseudoInverseCosine(FastGraph<U> graph, EdgeOrientation orient)
    {
        super(graph);
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

        // Now, we find the pseudo-inverse of the Laplacian:
        DoubleMatrix ones = DoubleMatrix.ones(numUsers(), numUsers()).muli(1.0/(numUsers()+0.0));

        DoubleMatrix aux = L.add(ones.mul(-1.0));
        L = Solve.solve(aux, DoubleMatrix.eye(numUsers()));
        L.addi(ones);

        for(int i = 0; i < numUsers(); ++i)
        {
            for(int j = 0; j < numUsers(); ++j)
            {
                L.put(i, j, L.get(i,j)/Math.sqrt(L.get(i,i)*L.get(j,j)));
            }
        }

        return L.toArray2();
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

        DoubleMatrix2D eye = new DenseDoubleMatrix2D(numUsers(),numUsers());
        eye.assign(1.0/(numUsers()+0.0));

        L.assign(eye, (x,y) -> x - y);

        DoubleMatrix2D aux = new DenseDoubleMatrix2D(numUsers(),numUsers());
        LUDecompositionQuick lu = new LUDecompositionQuick();
        lu.decompose(L);
        lu.solve(aux);

        aux.assign(eye, Double::sum);

        for(int i = 0; i < numUsers(); ++i)
        {
            for(int j = 0; j < numUsers(); ++j)
            {
                aux.setQuick(i, j, aux.getQuick(i,j)/Math.sqrt(aux.get(i,i)*aux.get(j,j)));
            }
        }

        return aux.toArray();
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

        Matrix eye = new DenseMatrix(this.numUsers(), this.numUsers());
        for(int i = 0; i < this.numUsers(); ++i)
            for(int j = 0; j < this.numUsers(); ++j)
                eye.set(i,j, 1.0/(numUsers()+0.0));

        Matrix solve = Matrices.identity(this.numUsers());
        L.solve(Matrices.identity(numUsers()), solve);

        L = solve.add(eye);

        double[][] defMatrix = new double[numUsers()][numUsers()];
        for (int i = 0; i < L.numRows(); ++i)
            for (int j = 0; j < L.numColumns(); ++j)
                defMatrix[i][j] = L.get(i, j)/Math.sqrt(L.get(i,i)*L.get(j,j));
        return defMatrix;
    }
}
