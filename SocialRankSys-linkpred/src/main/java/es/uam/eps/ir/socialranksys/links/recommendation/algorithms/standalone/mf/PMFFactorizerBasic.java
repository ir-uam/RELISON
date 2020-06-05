/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.mf;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.TransposedPreferenceData;
import es.uam.eps.ir.ranksys.mf.als.ALSFactorizer;
import es.uam.eps.ir.socialranksys.utils.math.MathFunctions;

/**
 * Factorizes a matrix for the Probabilistic Matrix Factorization Algorithm
 * @author Javier Sanz-Cruzado
 * @param <U> Class of the users
 * @param <I> Class of the items
 */
public class PMFFactorizerBasic<U,I> extends ALSFactorizer<U,I>
{

    /**
     * Algebra
     */
    private static final Algebra ALG = new Algebra();
    /**
     * Regularization parameter for user matrix
     */
    private final double lambdaU;
    /**
     * Regularization parameter for item matrix
     */
    private final double lambdaV;
    /**
     * Learning rate
     */
    private final double learningRate;
 
    /**
     * Constructor.
     * @param lambda Regularization rate.
     * @param learningRate Learning rate for the gradient descent.
     */
    public PMFFactorizerBasic(double lambda, double learningRate)
    {
        this(lambda, lambda, learningRate);
    }
    
    
    /**
     * Constructor.
     * @param lambdaU Regularization rate for matrix U.
     * @param lambdaV Regularization rate for matrix V.
     * @param learningRate Learning rate for the gradient descent.
     */
    public PMFFactorizerBasic(double lambdaU, double lambdaV, double learningRate)
    {
        this(lambdaU, lambdaV, learningRate, 20);
    }
    
    /**
     * 
     * @param lambdaU Regularization rate for matrix U
     * @param lambdaV Regularization rate for matrix V
     * @param learningRate Learning rate for the gradient descent
     * @param numEpochs Number of epochs
     */
    public PMFFactorizerBasic(double lambdaU, double lambdaV, double learningRate, int numEpochs)
    {
        super(numEpochs);
        this.lambdaU = lambdaU;
        this.lambdaV = lambdaV;
        this.learningRate = learningRate;
    }

    @Override
    protected double error(DenseDoubleMatrix2D p, DenseDoubleMatrix2D q, FastPreferenceData<U, I> data)
    {
        return data.getUidxWithPreferences().parallel().mapToDouble(uidx -> {
            DoubleMatrix1D pu = p.viewRow(uidx);
            DoubleMatrix1D su = q.zMult(pu, null);
            
            double err1 = data.getUidxPreferences(uidx).mapToDouble(iv -> {
                double rui = iv.v2;
                double sui = su.getQuick(iv.v1);
                return (rui - sui) * (rui - sui);
            }).sum();
                       
            return err1 / data.numItems();
        }).sum() / data.numUsers();

    }

    @Override
    protected void set_minP(DenseDoubleMatrix2D p, DenseDoubleMatrix2D q, FastPreferenceData<U, I> data) 
    {
        set_min(p, q, this.lambdaU, data,learningRate);
    }

    @Override
    protected void set_minQ(DenseDoubleMatrix2D q, DenseDoubleMatrix2D p, FastPreferenceData<U, I> data) 
    {
        set_min(q, p, lambdaV, new TransposedPreferenceData<>(data),learningRate);
    }
    
    /**
     * Minimizes the parameters for one of the two matrices.
     * @param p Matrix to optimize.
     * @param q Fixed matrix.
     * @param lambda The regularization parameter for this matrix.
     * @param data Preference data.
     */
    protected static <U,I,O> void set_min(final DenseDoubleMatrix2D p, final DenseDoubleMatrix2D q, double lambda, FastPreferenceData<U,I> data, double learningRate)
    {
        final int K = p.columns();
        
        // Y^t Y
        DenseDoubleMatrix2D A1P = new DenseDoubleMatrix2D(K,K);
        
        
        //Y^t Y + \lambda I
        for(int k = 0; k < K; k++)
        {
            A1P.setQuick(k,k,lambda + A1P.getQuick(k, k));
        }
        
        DenseDoubleMatrix2D[] A2P = new DenseDoubleMatrix2D[q.rows()];
        data.getIidxWithPreferences().parallel().forEach(iidx-> {
            A2P[iidx] = new DenseDoubleMatrix2D(K,K);
            DoubleMatrix1D qi = q.viewRow(iidx);
            ALG.multOuter(qi, qi, A2P[iidx]);
        });
        
        data.getUidxWithPreferences().parallel().forEach(uidx ->{
            DoubleMatrix2D A = new DenseDoubleMatrix2D(K,K);
            DoubleMatrix1D b = new DenseDoubleMatrix1D(K);
            A.assign(A1P);
            b.assign(0.0);
            
            data.getUidxPreferences(uidx).forEach(iv -> {
                int iidx = iv.v1;
                double rui = iv.v2;
                
                DoubleMatrix1D qi = q.viewRow(iidx);
                
                A.assign(A2P[iidx], Double::sum);
                b.assign(qi, (x,y)-> x + y * rui);
            });
        });
    }
    
    /**
     * Finds an intermediate matrix used for factorization.
     * @param p User Matrix
     * @param q Item Matrix
     * @param data User preferences data
     * @return The intermediate matrix
     */
    private DenseDoubleMatrix2D findM(DenseDoubleMatrix2D p, DenseDoubleMatrix2D q, FastPreferenceData<U,I> data)
    {
        final int N = p.rows();
        final int M = q.rows();
        final int F = p.columns();
        
        //U_i^t V_j
        DenseDoubleMatrix2D aproxRatings = new DenseDoubleMatrix2D(N,M);
        DenseDoubleMatrix2D aproxDerivRatings = new DenseDoubleMatrix2D(N,M);
        p.zMult(q, aproxRatings, 1, 0, false, true);
        
        //sigma'(U_i^t V_j) * (\sigma(U_i^t V_j) - R_ij)
        DenseDoubleMatrix2D m = new DenseDoubleMatrix2D(N,M);
        data.getUidxWithPreferences().parallel().forEach(uidx ->
            data.getUidxPreferences(uidx).forEach(iv ->
            {
                int iidx = iv.v1;
                double rui = iv.v2;
                
                double sigmoidValue = MathFunctions.sigmoid.applyAsDouble(aproxRatings.getQuick(uidx, iidx));
                double derivValue = sigmoidValue*(1-sigmoidValue);
                double rating = (sigmoidValue-rui)*derivValue;
                m.setQuick(uidx, iidx, rating);
            })
        );
              
        return m;
    }
    
}
