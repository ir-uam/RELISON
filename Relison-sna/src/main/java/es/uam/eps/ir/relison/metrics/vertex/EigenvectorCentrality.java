/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics.vertex;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.EigenvalueDecomposition;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.utils.matrix.MatrixLibrary;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.EVD;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.NotConvergedException;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;

/**
 * Finds the eigenvector centrality of the network, which measures the importance of a node
 * based on the importance of its neighbors. It is the solution to equation Ax = kx, where
 * k is the largest eigenvalue of the adjacency matrix A.
 *
 * <p>
 * <b>Reference: </b> Bonacich, P.F. Power and centrality: A family of measures. American Journal of Sociology 92 (5), pp. 1170-1182 (1987)
 * </p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class EigenvectorCentrality<U> extends MatrixBasedVertexMetric<U>
{
    /**
     * The orientation of the paths.
     */
    private final EdgeOrientation orient;

    /**
     * Constructor.
     * @param library   the matrix library to use.
     * @param orient    the orientation to choose for the adjacency matrix.
     */
    public EigenvectorCentrality(MatrixLibrary library, EdgeOrientation orient)
    {
        super(library);
        this.orient = orient;
    }

    /**
     * Constructor. It takes the paths leading to the users (right eigenvector - Orientation OUT).
     * @param library   the matrix library to use.
     */
    public EigenvectorCentrality(MatrixLibrary library)
    {
        this(library, EdgeOrientation.OUT);
    }

    /**
     * Constructor. It uses the default matrix libraries.
     * @param orient    the orientation to choose for the adjacency matrix.
     */
    public EigenvectorCentrality(EdgeOrientation orient)
    {
        super();
        this.orient = orient;
    }

    /**
     * Constructor. It uses the default matrix libraries and considers the paths leading to the users (right
     * eigenvector - Orientation OUT).
     */
    public EigenvectorCentrality()
    {
        this(EdgeOrientation.OUT);
    }

    @Override
    protected double[] getJBLASScores(Graph<U> graph)
    {
        double[][] adj = graph.getAdjacencyMatrix(orient);
        DoubleMatrix matrix = new DoubleMatrix(adj);

        if(graph.isDirected() && orient != EdgeOrientation.UND && orient != EdgeOrientation.MUTUAL)
        {
            ComplexDoubleMatrix[] eigenVectors = Eigen.eigenvectors(matrix);
            ComplexDoubleMatrix eigenvalues = eigenVectors[1];
            double min = Double.NEGATIVE_INFINITY;
            int index = -1;
            for(int i = 0; i < eigenvalues.columns; ++i)
            {
                ComplexDouble complex = eigenvalues.get(i, i);
                if(complex.isReal() && complex.real() > min)
                {
                    index = i;
                    min = complex.real();
                }
            }

            // If the maximum eigenvalue is zero, then, the graph is acyclic, and all the centralities
            // should be zero
            if(min == 0.0 || min == Double.NEGATIVE_INFINITY) return new double[matrix.columns];
            else return eigenVectors[0].real().getColumn(index).toArray();
        }
        else // the matrix is symmetric
        {
            DoubleMatrix[] eigenVectors = Eigen.symmetricEigenvectors(matrix);
            double[] eigenvalues = eigenVectors[1].diag().data;
            double min = Double.NEGATIVE_INFINITY;
            int index = -1;
            for(int i = 0; i < eigenvalues.length; ++i)
            {
                if(eigenvalues[i] > min)
                {
                    index = i;
                    min = eigenvalues[i];
                }
            }
            return eigenVectors[0].getColumn(index).data;
        }
    }

    @Override
    protected double[] getMTJScores(Graph<U> graph)
    {
        int numUsers = graph.getAdjacencyMatrixMap().numObjects();
        double[][] adj = graph.getAdjacencyMatrix(orient);
        DenseMatrix aux = new DenseMatrix(adj);

        EVD eig = new EVD(aux.numColumns(), false, true);
        try
        {
            eig.factor(aux);
        }
        catch (NotConvergedException e)
        {
            return new double[numUsers];
        }

        if(graph.isDirected() && orient != EdgeOrientation.UND && orient != EdgeOrientation.MUTUAL)
        {
            double[] realEigs = eig.getRealEigenvalues();
            double[] imgEigs = eig.getImaginaryEigenvalues();
            double min = Double.NEGATIVE_INFINITY;
            int index = -1;
            for(int i = 0; i < realEigs.length; ++i)
            {
                if(imgEigs[i] == 0.0 && realEigs[i] > min)
                {
                    index = i;
                    min = realEigs[i];
                }
            }

            // If the maximum eigenvalue is zero, then, the graph is acyclic, and all the centralities
            // should be zero
            if(min == 0.0 || min == Double.NEGATIVE_INFINITY) return new double[aux.numColumns()];
            else
            {
                Matrix matrix = eig.getRightEigenvectors();
                double[] scores = new double[matrix.numRows()];
                for(int i = 0; i < scores.length; ++i)
                {
                    scores[i] = matrix.get(i, index);
                }
                return scores;
            }
        }
        else
        {
            double[] eigenvalues = eig.getRealEigenvalues();
            double min = Double.NEGATIVE_INFINITY;
            int index = -1;
            for(int i = 0; i < eigenvalues.length; ++i)
            {
                if(eigenvalues[i] > min)
                {
                    index = i;
                    min = eigenvalues[i];
                }
            }
            DenseMatrix matrix = eig.getRightEigenvectors();
            double[] scores = new double[matrix.numRows()];
            for(int i = 0; i < scores.length; ++i)
            {
                scores[i] = matrix.get(i, index);
            }
            return scores;
        }
    }

    @Override
    protected double[] getCOLTScores(Graph<U> graph)
    {
        double[][] adj = graph.getAdjacencyMatrix(orient);
        DoubleMatrix2D matrix = new SparseDoubleMatrix2D(adj);

        EigenvalueDecomposition eig = new EigenvalueDecomposition(matrix);
        if(graph.isDirected() && orient != EdgeOrientation.UND && orient != EdgeOrientation.MUTUAL)
        {
            DoubleMatrix1D realEig = eig.getRealEigenvalues();
            DoubleMatrix1D imagEig = eig.getImagEigenvalues();

            double min = Double.NEGATIVE_INFINITY;
            int index = -1;
            for(int i = 0; i < realEig.size(); ++i)
            {
                if(imagEig.getQuick(i) == 0 && realEig.getQuick(i) > min) // Real eigenvalue;
                {
                    index = i;
                    min = realEig.getQuick(i);
                }
            }

            // If the maximum eigenvalue is zero, then, the graph is acyclic, and all the centralities
            // should be zero
            if(min == 0.0 || min == Double.NEGATIVE_INFINITY) return new double[matrix.columns()];
            else return eig.getV().viewColumn(index).toArray();
        }
        else // the matrix is symmetric
        {
            DoubleMatrix1D eigenvalues = eig.getRealEigenvalues();
            double min = Double.NEGATIVE_INFINITY;
            int index = -1;
            for(int i = 0; i < eigenvalues.size(); ++i)
            {
                if(eigenvalues.getQuick(i) > min)
                {
                    index = i;
                    min = eigenvalues.getQuick(i);
                }
            }
            return eig.getV().viewColumn(index).toArray();
        }
    }
}