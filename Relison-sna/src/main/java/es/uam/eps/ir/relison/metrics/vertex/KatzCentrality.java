/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics.vertex;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.LUDecompositionQuick;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.utils.matrix.MatrixLibrary;
import no.uib.cipr.matrix.*;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;

/**
 * Finds the Katz centrality of the nodes, which estimates the importance of a node considering the
 * paths between the node and the rest of the network.
 *
 * Katz centrality of the nodes.
 * <p>
 * <b>Reference: </b> Katz, L. A new status index derived from sociometric analysis. Psychometrika 18(1), pp. 33-43 (1953)
 * </p>
 */
public class KatzCentrality<U> extends MatrixBasedVertexMetric<U>
{
    /**
     * The orientation of the paths.
     */
    private final EdgeOrientation orient;
    /**
     * A dumping factor.
     */
    private final double alpha;

    /**
     * Constructor.
     * @param library   the matrix library to use.
     * @param orient    the orientation to choose for the adjacency matrix.
     * @param alpha     the dumping factor for paths at large distances.
     */
    public KatzCentrality(MatrixLibrary library, EdgeOrientation orient, double alpha)
    {
        super(library);
        this.orient = orient;
        this.alpha = alpha;
    }

    /**
     * Constructor. By default, it considers the paths leading to the user.
     * @param library   the matrix library to use.
     * @param alpha     the dumping factor for paths at large distances.
     */
    public KatzCentrality(MatrixLibrary library, double alpha)
    {
        this(library, EdgeOrientation.IN, alpha);
    }

    /**
     * Constructor. It uses the default matrix libraries.
     * @param orient    the orientation to choose for the adjacency matrix.
     * @param alpha     the dumping factor for paths at large distances.
     */
    public KatzCentrality(EdgeOrientation orient, double alpha)
    {
        super();
        this.orient = orient;
        this.alpha = alpha;
    }

    /**
     * Constructor. It uses the default matrix libraries and considers the paths leading to the users.
     * @param alpha     the dumping factor for paths at large distances.
     */
    public KatzCentrality(double alpha)
    {
        this(EdgeOrientation.IN, alpha);
    }

    @Override
    protected double[] getJBLASScores(Graph<U> graph)
    {
        double[][] adj = graph.getAdjacencyMatrix(orient);
        DoubleMatrix matrix = new DoubleMatrix(adj);
        DoubleMatrix eye = DoubleMatrix.eye(matrix.columns);
        eye = eye.add(matrix.mul(-alpha));
        DoubleMatrix solved = Solve.solve(eye, DoubleMatrix.eye(matrix.columns));

        solved.add(DoubleMatrix.eye(matrix.columns).mul(-1.0));
        DoubleMatrix scores = solved.muli(DoubleMatrix.ones(matrix.columns, 1));
        return scores.data;
    }

    @Override
    protected double[] getMTJScores(Graph<U> graph)
    {
        int numUsers = graph.getAdjacencyMatrixMap().numObjects();
        double[][] adj = graph.getAdjacencyMatrix(orient);
        Matrix aux = new DenseMatrix(adj);
        DenseMatrix lsm = Matrices.identity(numUsers);
        lsm.add(-alpha, aux);

        Matrix solution = lsm.solve(Matrices.identity(lsm.numRows()), Matrices.identity(lsm.numRows()));
        solution.add(Matrices.identity(numUsers).scale(-1.0));
        Vector v = new DenseVector(numUsers);
        for(int i = 0; i < numUsers; ++i) v.set(i, 1.0);
        Vector scores = new DenseVector(numUsers);

        scores = solution.mult(v, scores);
        double[] defScores = new double[numUsers];
        for(int i = 0; i < numUsers; ++i) defScores[i] = scores.get(i);
        return defScores;
    }

    @Override
    protected double[] getCOLTScores(Graph<U> graph)
    {
        double[][] adj = graph.getAdjacencyMatrix(orient);
        DoubleMatrix2D matrix = new SparseDoubleMatrix2D(adj);
        DoubleMatrix2D eye = DoubleFactory2D.sparse.identity(matrix.columns());
        matrix.assign(eye, (x,y) -> -alpha*x + y);

        LUDecompositionQuick lu = new LUDecompositionQuick();
        lu.decompose(matrix);
        lu.solve(eye);

        DoubleMatrix2D eye2 = DoubleFactory2D.sparse.identity(matrix.columns());
        eye2.assign(x -> -x);
        eye.assign(eye2, Double::sum);

        Algebra alg = new Algebra();
        DoubleMatrix1D ones = DoubleFactory1D.dense.make(matrix.columns(), 1.0);
        ones = alg.mult(eye, ones);
        return ones.toArray();
    }
}