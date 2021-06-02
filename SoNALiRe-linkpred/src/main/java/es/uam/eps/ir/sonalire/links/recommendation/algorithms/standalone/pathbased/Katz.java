/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.pathbased;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.LUDecompositionQuick;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.GlobalMatrixBasedRecommender;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;

/**
 * Katz algorithm. This algorithm weights the paths between two users, giving more weight to those at
 * closer distances.
 *
 * <br>
 *     <b>References:</b>
 *      <ol>
 *          <li>L. Katz. A new status index derived from sociometric analysis. Psychometrika 18(1), 39-43 (1953)</li>
 *          <li>D. Liben-Nowell, D., J. Kleinberg. The Link Prediction Problem for Social Networks. Journal of the American Society for Information Science and Technology 58(7) (2007)</li>
 *      </ol>
 *
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Katz<U> extends GlobalMatrixBasedRecommender<U>
{
    /**
     * Dampening factor.
     */
    private final double b;

    /**
     * Edge orientation to take.
     */
    private final EdgeOrientation orient;

    /**
     * Constructor. Uses, by default, the classical orientation (OUT).
     *
     * @param graph a fast graph representing the social network.
     * @param b     the dampening factor.
     */
    public Katz(FastGraph<U> graph, double b)
    {
        super(graph);
        this.b = b;
        this.matrix = this.getMatrix();
        this.orient = EdgeOrientation.OUT;
    }

    /**
     * Constructor.
     *
     * @param graph     a fast graph representing the social network.
     * @param b         the dampening factor.
     * @param orient    the orientation for the adjacency matrix.
     */
    public Katz(FastGraph<U> graph, double b, EdgeOrientation orient)
    {
        super(graph);
        this.b = b;
        this.matrix = this.getMatrix();
        this.orient = orient;
    }

    /**
     * Obtains the matrix for the recommendation.
     * @return the matrix for the recommendation.
     */
    protected double[][] getJBLASMatrix()
    {
        double[][] adj = graph.getAdjacencyMatrix(orient);
        DoubleMatrix matrix = new DoubleMatrix(adj);
        DoubleMatrix eye = DoubleMatrix.eye(matrix.columns);
        eye = eye.add(matrix.mul(-b));
        DoubleMatrix solved = Solve.solve(eye, DoubleMatrix.eye(matrix.columns));
        return solved.toArray2();
    }

    /**
     * Obtains the matrix for the recommendation
     * @return the matrix for the recommendation
     */
    protected double[][] getCOLTMatrix()
    {
        double[][] adj = graph.getAdjacencyMatrix(orient);
        DoubleMatrix2D matrix = new SparseDoubleMatrix2D(adj);
        DoubleMatrix2D eye = DoubleFactory2D.sparse.identity(matrix.columns());
        matrix.assign(eye, (x,y) -> -b*x + y);

        LUDecompositionQuick lu = new LUDecompositionQuick();
        lu.decompose(matrix);
        lu.solve(eye);

        return eye.toArray();
    }

    /**
     * Obtains the matrix for the recommendation
     * @return the matrix for the recommendation
     */
    protected double[][] getMTJMatrix()
    {
        double[][] adj = graph.getAdjacencyMatrix(orient);
        Matrix aux = new DenseMatrix(adj);
        DenseMatrix lsm = Matrices.identity(this.numUsers());
        lsm.add(-this.b, aux);

        Matrix solution = lsm.solve(Matrices.identity(lsm.numRows()), Matrices.identity(lsm.numRows()));
        double[][] matrix = new double[numUsers()][numItems()];
        for(int i = 0; i < solution.numRows(); ++i)
            for(int j = 0; j < solution.numColumns(); ++j)
                matrix[i][j] = solution.get(i,j);
        return matrix;
    }
}
