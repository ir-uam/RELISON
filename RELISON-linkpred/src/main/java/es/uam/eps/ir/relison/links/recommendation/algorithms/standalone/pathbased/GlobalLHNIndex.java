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
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.EigenvalueDecomposition;
import cern.colt.matrix.linalg.LUDecompositionQuick;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.recommendation.algorithms.GlobalMatrixBasedRecommender;
import no.uib.cipr.matrix.*;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.jblas.Solve;

/**
 * Global Leicht-Holme-Newman similarity algorithm.
 * <p>
 *  <b>Reference:</b> E.A. Leicht, P. Holme, M.E.J. Newman. Vertex similarity in networks. Physical Review E 73(2): 026120 (2006)
 * </p>
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class GlobalLHNIndex<U> extends GlobalMatrixBasedRecommender<U>
{
    /**
     * The decay factor of the similarity.
     */
    private final double phi;

    /**
     * The orientation selection for the adjacency matrix.
     */
    private final EdgeOrientation orient;

    /**
     * Constructor. By default, considers networks as undirected.
     *
     * @param graph a fast graph representing the social network.
     * @param phi   the decay factor of the similarity.
     */
    public GlobalLHNIndex(FastGraph<U> graph, double phi)
    {
        super(graph);
        this.phi = phi;
        this.matrix = this.getMatrix();
        this.orient = EdgeOrientation.UND;
    }

    /**
     * Constructor. By default, considers networks as undirected.
     *
     * @param graph     a fast graph representing the social network.
     * @param phi       the decay factor for the similarity.
     * @param orient    the orientation for selecting the adjacency matrix.
     */
    public GlobalLHNIndex(FastGraph<U> graph, double phi, EdgeOrientation orient)
    {
        super(graph);
        this.phi = phi;
        this.matrix = this.getMatrix();
        this.orient = orient;
    }

    @Override
    protected double[][] getJBLASMatrix()
    {
        double[][] adjAux = graph.getAdjacencyMatrix(orient);
        DoubleMatrix adj = new DoubleMatrix(adjAux);
        double edgeCount = adj.sum();
        DoubleMatrix eigen = Eigen.symmetricEigenvalues(adj);
        int[] largest = eigen.sortingPermutation();

        // The largest eigenvalue.
        double lambda = eigen.get(largest[largest.length-1]);

        DoubleMatrix sum = adj.rowSums();
        DoubleMatrix D = DoubleMatrix.zeros(numUsers());
        for(int i = 0; i < numUsers(); ++i)
        {
            D.put(i, i, 1.0/sum.get(i));
        }

        DoubleMatrix X = DoubleMatrix.eye(numUsers());
        adj.muli(phi/lambda);

        X.addi(adj.mul(-1.0));

        DoubleMatrix Z = Solve.solve(X, DoubleMatrix.eye(numUsers()));

        DoubleMatrix lhn = D.mmul(Z).mmul(D).mul(edgeCount*lambda);
        return lhn.toArray2();
    }

    @Override
    protected  double[][] getCOLTMatrix()
    {
        double[][] adjAux = graph.getAdjacencyMatrix(orient);
        DoubleMatrix2D adj = new SparseDoubleMatrix2D(adjAux);
        double edgeCount = adj.zSum();

        // First, we find the largest eigenvalue of the adjacency matrix.
        EigenvalueDecomposition eigen = new EigenvalueDecomposition(adj);
        DoubleMatrix1D eigenvalues = eigen.getRealEigenvalues();
        double lambda = Double.NEGATIVE_INFINITY;
        for(int i = 0; i < adj.columns(); ++i)
        {
            if(eigenvalues.getQuick(i) > lambda) lambda = eigenvalues.getQuick(i);
        }

        double defLambda = lambda;

        // Then...
        DoubleMatrix1D rowSums = new SparseDoubleMatrix1D(adj.rows());
        rowSums.assign(1.0);

        Algebra alg = new Algebra();
        rowSums = alg.mult(adj, rowSums);

        DoubleMatrix2D D = new SparseDoubleMatrix2D(adj.rows(), adj.rows());
        for(int i = 0; i < adj.rows(); ++i)
        {
            D.setQuick(i,i, 1.0/rowSums.get(i));
        }
        DoubleMatrix2D eye = DoubleFactory2D.sparse.identity(adj.rows());
        adj.assign(eye, (x,y) -> x*-phi/defLambda + y);


        LUDecompositionQuick lu = new LUDecompositionQuick();
        lu.decompose(adj);
        lu.solve(eye);

        // eye has the solution:
        eye = alg.mult(D, eye);
        eye = alg.mult(eye, D);
        eye.assign((x) -> x*edgeCount*defLambda);
        return eye.toArray();
    }


    @Override
    protected  double[][] getMTJMatrix()
    {
        double eigenvalue;
        double[][] adjAux = graph.getAdjacencyMatrix(orient);
        DenseMatrix matrix = new DenseMatrix(adjAux);

        Matrix degree = new DenseMatrix(this.numUsers(), this.numUsers());
        double edgeCount = 0;
        for(int i = 0; i < this.numUsers(); ++i)
        {
            double deg = this.graph.degree(this.uidx2user(i), orient);
            degree.set(i,i,1.0/deg);
            edgeCount += deg;
        }

        try
        {
            SVD svd = new SVD(matrix.numRows(), matrix.numColumns(), false);
            svd.factor(matrix);
            eigenvalue = svd.getS()[0];

        }
        catch(NotConvergedException notConv)
        {
            eigenvalue = 0.0;
        }

        if (eigenvalue != 0.0)
        {
            Matrix identity = Matrices.identity(this.numUsers());
            identity.add(-phi / eigenvalue, matrix);

            Matrix aux = Matrices.identity(this.numUsers());
            identity.solve(Matrices.identity(this.numUsers()), aux);
            aux = aux.scale(edgeCount*eigenvalue);
            
            Matrix def = Matrices.identity(this.numUsers());
            aux.mult(degree, def);
            degree.mult(def, aux);

            double[][] defMatrix = new double[numUsers()][numUsers()];
            for (int i = 0; i < aux.numRows(); ++i)
                for (int j = 0; j < aux.numColumns(); ++j)
                    defMatrix[i][j] = aux.get(i, j);
            return defMatrix;
        }


        double[][] defMatrix = new double[numUsers()][numUsers()];
        for(int i = 0; i < matrix.numRows(); ++i)
            defMatrix[i][i] = 1.0;
        return defMatrix;
    }
}
