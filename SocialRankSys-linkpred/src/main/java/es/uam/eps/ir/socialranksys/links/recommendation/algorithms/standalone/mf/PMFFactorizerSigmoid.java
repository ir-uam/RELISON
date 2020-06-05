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
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.TransposedPreferenceData;
import es.uam.eps.ir.ranksys.mf.als.ALSFactorizer;

/**
 * Factorizes a matrix for the Probabilistic Matrix Factorization Algorithm
 * @author Javier Sanz-Cruzado
 * @param <U> Class of the users
 * @param <I> Class of the items
 */
public class PMFFactorizerSigmoid<U,I> extends ALSFactorizer<U,I>
{

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
    public PMFFactorizerSigmoid(double lambda, double learningRate)
    {
        this(lambda, lambda, learningRate);
    }
    
    
    /**
     * Constructor.
     * @param lambdaU Regularization rate for matrix U.
     * @param lambdaV Regularization rate for matrix V.
     * @param learningRate Learning rate for the gradient descent.
     */
    public PMFFactorizerSigmoid(double lambdaU, double lambdaV, double learningRate)
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
    public PMFFactorizerSigmoid(double lambdaU, double lambdaV, double learningRate, int numEpochs)
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
        set_min(p,q,lambdaU, data, this.learningRate);
    }

    @Override
    protected void set_minQ(DenseDoubleMatrix2D p, DenseDoubleMatrix2D q, FastPreferenceData<U,I> data)
    {
        set_min(q,p,lambdaV, new TransposedPreferenceData<>(data), this.learningRate);
    }
    
    /**
     * Minimizes the parameters for one of the two matrices.
     * @param p Matrix to optimize.
     * @param q Fixed matrix.
     * @param lambda The regularization parameter for this matrix.
     * @param data Preference data.
     */
    protected static <U,I,O> void set_min(DenseDoubleMatrix2D p, final DenseDoubleMatrix2D q, double lambda, FastPreferenceData<U,I> data, double learningRate)
    {
        final int K = q.columns();
        // Applying gradient descent for optimizing the P matrix
        /*
         * Number of iterations for the gradient descent
         */
        int numItersGradient = 10;
        for(int i = 0; i < numItersGradient; ++i)
        {
            // Calculate the approximated scores for the matrix
            DenseDoubleMatrix2D s = new DenseDoubleMatrix2D(p.rows(), q.rows());        
            p.zMult(q, s, 1.0, 0.0, false, true);
            s.assign(s, (x,y)-> 1.0/(1+Math.exp(-x)));
            
            
            // Calculate the new latent matrix
            data.getUidxWithPreferences().parallel().forEach(uidx ->{
                // Calculate the new latent matrix for each object (it might be user or item)
                DoubleMatrix1D sumI = p.viewRow(uidx);
                data.getUidxPreferences(uidx).forEach((iv)->{
                    int iidx = iv.v1;
                    double rui = iv.v2;
                    double sui = s.getQuick(uidx, iidx);
                    DoubleMatrix1D column = q.viewRow(iidx);
                   
                    sumI.assign(column, (x,y) -> x + learningRate*y*(rui-sui)*sui*(1-sui));
                           
                });
                
                for(int j = 0; j < K; ++j)
                {
                    sumI.setQuick(j, lambda);
                }
                
                p.viewRow(uidx).assign(sumI);
            });

        }
    } 
}
