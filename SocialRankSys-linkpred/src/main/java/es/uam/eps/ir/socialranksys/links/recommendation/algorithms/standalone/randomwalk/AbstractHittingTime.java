/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.randomwalk;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.MatrixBasedRecommender;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.MatrixLibrary;
import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.sparse.LinkedSparseMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;
import org.jblas.ranges.IntervalRange;
import org.jblas.ranges.Range;

import java.util.stream.IntStream;

/**
 * Abstract version of the hitting time algorithm.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class AbstractHittingTime<U> extends MatrixBasedRecommender<U>
{
    /**
     * Constructor.
     *
     * @param graph A fast graph representing the social network.
     */
    public AbstractHittingTime(FastGraph<U> graph)
    {
        super(graph);
    }

    /**
     * Constructor.
     * @param graph a fast graph representing the social network.
     * @param library the matrix library to use.
     */
    public AbstractHittingTime(FastGraph<U> graph, MatrixLibrary library)
    {
        super(graph, library);
    }

    /**
     * Obtains the matrix for each user,
     * @param uidx the identifier of the target user.
     * @return the hitting time matrix.
     */
    protected double[][] getJBLASMatrix(int uidx)
    {
        int nUsers = Long.valueOf(graph.getVertexCount()).intValue();
        // First, we find the transition matrix:

        DoubleMatrix T = this.getJBLASTransitionMatrix(uidx);
        DoubleMatrix A = DoubleMatrix.eye(nUsers);
        A.addi(T.mul(-1.0));

        Range range = new IntervalRange(0, nUsers-1);

        DoubleMatrix U = A.get(range, range);
        DoubleMatrix D = A.get(nUsers-1, range);

        DoubleMatrix invU = Solve.solve(U, DoubleMatrix.eye(nUsers-1));
        DoubleMatrix h = Solve.solve(U, D);
        double beta = h.sum();

        DoubleMatrix W = DoubleMatrix.zeros(nUsers, nUsers);
        for(int i = 0; i < nUsers; ++i)
        {
            W.put(new IntervalRange(i,i+1), range, h.mul(-1.0/beta));
            W.put(i, nUsers-1, 1.0/beta);
        }

        DoubleMatrix aux = DoubleMatrix.zeros(nUsers, nUsers);
        aux.put(range, range, invU);
        DoubleMatrix AA = DoubleMatrix.eye(nUsers);
        AA.addi(W.mul(-1.0));

        DoubleMatrix AHash = AA.mmul(aux).mmul(AA);
        DoubleMatrix AHashDiag = AHash.diag().diag();

        DoubleMatrix J = DoubleMatrix.ones(nUsers, nUsers);
        aux = DoubleMatrix.eye(nUsers).add(AHash.mul(-1.0)).add(J.mmul(AHashDiag));
        DoubleMatrix Pi = DoubleMatrix.zeros(nUsers, nUsers);
        for(int i = 0; i < nUsers; ++i)
        {
            Pi.put(i,i, W.get(i,i));
        }

        return aux.mmuli(Pi).toArray2();
    }

    /**
     * Generates the first passage time matrix using the COLT library.
     * @param uidx the identifier of the user.
     * @return the matrix using the COLT library.
     */
    protected double[][] getCOLTMatrix(int uidx)
    {
        // First, we find the transition matrix:
        DoubleMatrix2D T = this.getCOLTTransitionMatrix(uidx);

        Algebra alg = new Algebra();
        // Compute A = I - T
        DoubleMatrix2D AMatrix = DoubleFactory2D.sparse.identity(this.numUsers());
        AMatrix.assign(T, Functions.minus);

        // Compute matrix U and vector d
        DoubleMatrix2D U = AMatrix.viewPart(0, 0, AMatrix.rows()-1, AMatrix.columns()-1);
        DoubleMatrix1D d = AMatrix.viewRow(AMatrix.rows()-1).viewPart(0, AMatrix.columns()-1);

        // Compute the inverse of the matrix U
        DoubleMatrix2D invU = alg.inverse(U);

        // Compute the vector h
        DoubleMatrix1D h = alg.mult(alg.transpose(invU), d);

        // Compute the beta constant
        double beta = 1 - h.zSum();

        // Compute the stationary distribution
        DoubleMatrix1D weight = new SparseDoubleMatrix1D(AMatrix.columns());
        int i;
        for(i = 0; i < weight.size()-1; ++i)
        {
            weight.setQuick(i, -h.getQuick(i)/beta);
        }
        weight.setQuick(i, 1/beta);

        DoubleMatrix2D auxWeight = DoubleFactory2D.sparse.make(weight.toArray(), 1);
        // Compute the product of matrices AA#
        DoubleMatrix2D W = DoubleFactory2D.sparse.repeat(auxWeight,AMatrix.rows(),1);

        DoubleMatrix2D AA = DoubleFactory2D.sparse.identity(AMatrix.rows());
        AA.assign(W, Functions.minus);

        // Compute the pseudoinverse of matrix A
        DoubleMatrix2D AHash = new SparseDoubleMatrix2D(AMatrix.rows(), AMatrix.columns());
        for(i = 0; i < U.rows(); ++i)
        {
            for(int j = 0; j < invU.columns(); ++j)
            {
                AHash.setQuick(i, j, invU.getQuick(i, j));
            }
        }
        AHash = alg.mult(AA, AHash);
        AHash = alg.mult(AHash, AA);

        DoubleMatrix2D AHashDg = DoubleFactory2D.sparse.diagonal(DoubleFactory2D.sparse.diagonal(AHash));

        DoubleMatrix2D J = DoubleFactory2D.sparse.make(AHash.rows(), AHash.columns(), 1);
        J = alg.mult(J, AHashDg);
        J.assign(AHash, Functions.minus);
        J.assign(DoubleFactory2D.sparse.identity(AHash.rows()), Functions.plus);


        DoubleMatrix2D Pi = new SparseDoubleMatrix2D(AHash.rows(), AHash.columns());
        for(i = 0; i < AHash.rows(); ++i)
        {
            Pi.setQuick(i, i, 1.0/weight.getQuick(i));
        }
        return alg.mult(J,Pi).toArray();
    }


    protected double[][] getMTJMatrix(int uidx)
    {
        Matrix transition = this.getMTJTransitionMatrix(uidx);
        // Once we have the transition matrix, it is time to compute the necessary matrices for computing the mean first
        // passage time matrix.

        // Compute A = I - T
        Matrix AMatrix = Matrices.identity(this.numUsers());
        AMatrix.add(-1,transition);


        // Compute matrix U and vector d
        int[] selection = IntStream.range(0, this.numUsers()-1).toArray();
        Matrix U = new DenseMatrix(AMatrix.numRows()-1, AMatrix.numColumns()-1);
        for(int i = 0; i < AMatrix.numRows()-1;++i)
        {
            for(int j=0; j < AMatrix.numColumns()-1;++j)
                U.set(i,j,AMatrix.get(i,j));
        }
        Matrix aux = Matrices.identity(AMatrix.numRows());
        Vector d = Matrices.getSubVector(Matrices.getColumn(AMatrix.transpose(aux), this.numUsers()-1), selection);

        // Compute the inverse of the matrix U
        Matrix invU = U.solve(Matrices.identity(this.numUsers()-1), Matrices.identity(this.numUsers()-1));

        // Compute the vector h
        Vector h = new DenseVector(this.uIndex.numUsers()-1);
        invU.transMult(d, h);

        // Compute the beta constant
        double beta = 1.0;
        for(int i = 0; i < h.size(); ++i)
        {
            beta -= h.get(i);
        }

        // Compute the stationary distribution
        Vector weight = new DenseVector(AMatrix.numRows());
        for(int i = 0; i < weight.size()-1; ++i)
        {
            weight.set(i, -h.get(i)/beta);
        }
        weight.set(weight.size()-1, 1/beta);

        Matrix W = new DenseMatrix(AMatrix.numRows(),AMatrix.numRows());
        for(int j = 0; j < AMatrix.numRows(); ++j)
        {
            for(int k = 0; k < AMatrix.numRows(); ++k)
            {
                AMatrix.set(j,k,weight.get(k));
            }
        }

        Matrix AA = Matrices.identity(AMatrix.numRows());
        AA.add(-1, W);

        // Compute the pseudoinverse of matrix A
        Matrix AHash = new LinkedSparseMatrix(AMatrix.numRows(), AMatrix.numColumns());
        for(int i = 0; i < U.numRows(); ++i)
        {
            for(int j = 0; j < invU.numColumns(); ++j)
            {
                AHash.set(i, j, invU.get(i, j));
            }
        }
        AA.mult(AHash, aux);
        aux.mult(AA, AHash);

        Matrix AHashDg = new LinkedSparseMatrix(AMatrix.numRows(), AMatrix.numColumns());
        for(int i = 0; i < AHash.numRows(); ++i)
        {
            AHashDg.set(i,i,AHash.get(i, i));
        }

        Matrix J = new DenseMatrix(AMatrix.numRows(), AMatrix.numColumns());
        for(int i = 0; i < J.numRows(); ++i)
        {
            for(int j = 0; j < J.numColumns(); ++j)
            {
                J.set(i,j,1.0);
            }
        }


        J.mult(AHashDg, aux);
        J = aux;
        J.add(-1.0, AHash);
        J.add(Matrices.identity(J.numRows()));


        Matrix Pi = new DenseMatrix(AHash.numRows(), AHash.numColumns());
        for(int i = 0; i < AHash.numRows(); ++i)
        {
            Pi.set(i, i, 1.0/weight.get(i));
        }
        Matrix hittingTime = Matrices.identity(this.numUsers());
        J.mult(Pi, hittingTime);

        double[][] defMatrix = new double[numUsers()][numUsers()];
        for(int i = 0; i < hittingTime.numRows(); ++i)
            for(int j = 0; j < hittingTime.numColumns(); ++j)
                defMatrix[i][j] = hittingTime.get(i,j);
        return defMatrix;
    }

    protected abstract DoubleMatrix getJBLASTransitionMatrix(int uidx);
    protected abstract DoubleMatrix2D getCOLTTransitionMatrix(int uidx);
    protected abstract DenseMatrix getMTJTransitionMatrix(int uidx);
}