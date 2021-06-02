/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.randomwalk;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.utils.matrix.MatrixLibrary;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import org.jblas.DoubleMatrix;

/**
 * Implementation of the Hitting time (using the not-personalized
 * PageRank transition matrix)
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class PageRankHittingTime<U> extends AbstractHittingTime<U>
{
    /**
     * The hitting time matrix.
     */
    private double[][] matrix;
    /**
     * Teleport probability.
     */
    private final double r;

    /**
     * Constructor.
     *
     * @param graph a fast graph representing the social network.
     * @param r     teleport probability.
     */
    public PageRankHittingTime(FastGraph<U> graph, double r)
    {
        super(graph);
        this.r = r;
        matrix = this.getMatrix(0);
    }

    /**
     * Constructor.
     *
     * @param graph     a fast graph representing the social network.
     * @param library   the matrix library to use.
     * @param r         teleport probability.
     */
    public PageRankHittingTime(FastGraph<U> graph, MatrixLibrary library, double r)
    {
        super(graph, library);
        this.r = r;
        matrix = this.getMatrix(0);
    }

    @Override
    protected DoubleMatrix getJBLASTransitionMatrix(int uidx)
    {
        double[][] adj = graph.getAdjacencyMatrix(EdgeOrientation.OUT);
        DoubleMatrix adjacency = new DoubleMatrix(adj);
        DoubleMatrix matrix = DoubleMatrix.ones(adjacency.rows);
        DoubleMatrix vector = matrix.rowSums();
        for(int i = 0; i < adjacency.rows; ++i)
        {
            if(vector.get(i) >= 0)
            {
                for(int j = 0; j < adjacency.columns; ++j)
                {
                    adjacency.put(i,j,adjacency.get(i,j)/vector.get(i));
                }
            }
        }
        matrix.muli(r/ adjacency.rows);
        matrix.addi(adjacency.mul(1.0-r));
        return matrix;
    }

    @Override
    protected DoubleMatrix2D getCOLTTransitionMatrix(int uidx)
    {
        double[][] adj = graph.getAdjacencyMatrix(EdgeOrientation.OUT);
        DoubleMatrix2D adjacency = new SparseDoubleMatrix2D(adj);
        DoubleMatrix2D matrix = new DenseDoubleMatrix2D(adjacency.rows(), adjacency.columns());
        matrix.assign(1.0);

        DoubleMatrix1D vector = new DenseDoubleMatrix1D(adjacency.rows());
        vector.assign(1.0);
        Algebra alg = new Algebra();
        vector = alg.mult(adjacency, vector);

        for(int i = 0; i < adjacency.rows(); ++i)
        {
            if(vector.get(i) >= 0)
            {
                for(int j = 0; j < adjacency.columns(); ++j)
                {
                    adjacency.setQuick(i,j,adjacency.get(i,j)/vector.get(i));
                }
            }
        }

        adjacency.assign((x) -> (1-r)*x);
        matrix.assign((x) -> r/(adjacency.rows()+0.0)*x);
        matrix.assign(adjacency, Double::sum);
        return matrix;
    }

    @Override
    protected DenseMatrix getMTJTransitionMatrix(int uidx)
    {
        double[][] adj = graph.getAdjacencyMatrix(EdgeOrientation.OUT);
        Matrix adjacency = new DenseMatrix(adj);
        DenseMatrix matrix = new DenseMatrix(adjacency.numRows(), adjacency.numColumns());
        Vector aux = new DenseVector(adjacency.numColumns());

        for(int i = 0; i < adjacency.numRows(); ++i)
        {
            aux.set(i, 1.0);
            for (int j = 0; j < adjacency.numColumns(); ++j)
                matrix.set(i, j, r / (adjacency.numRows() + 0.0));
        }
        Vector vector = new DenseVector(adjacency.numColumns());
        matrix.mult(aux, vector);

        for(int i = 0; i < adjacency.numRows(); ++i)
        {
            if(vector.get(i) >= 0)
            {
                for(int j = 0; j < adjacency.numColumns(); ++j)
                {
                    adjacency.set(i,j,(1.0-r)*adjacency.get(i,j)/vector.get(i));
                }
            }
        }

        matrix.add(adjacency);
        return matrix;
    }

    @Override
    public double[][] getMatrix(int uidx)
    {
        if(matrix == null) matrix = super.getMatrix(uidx);
        return matrix;
    }
}
