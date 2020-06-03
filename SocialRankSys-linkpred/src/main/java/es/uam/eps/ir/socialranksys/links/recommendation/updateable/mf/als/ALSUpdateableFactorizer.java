/* 
 * Copyright (C) 2015 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.updateable.mf.als;

import cern.colt.function.DoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import es.uam.eps.ir.socialranksys.links.data.updateable.preference.fast.updateable.FastUpdateablePreferenceData;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.mf.UpdateableFactorization;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.mf.UpdateableFactorizer;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static java.lang.Math.sqrt;


/**
 * UpdateableFactorizer. Abstract class for matrix factorization algorithms.
 *
 * @author Saúl Vargas (saul.vargas@uam.es)
 * 
 * @param <U> type of the users
 * @param <I> type of the items
 */
public abstract class ALSUpdateableFactorizer<U, I> extends UpdateableFactorizer<U,I>
{
    /**
     * Number of iterations.
     */
    private final int numIter;
    
    /**
     * The log.
     */
    private static final Logger LOG = Logger.getLogger(ALSUpdateableFactorizer.class.getName());

    /**
     * Constructor.
     * @param numIter number of iterations. 
     */
    public ALSUpdateableFactorizer(int numIter)
    {
        this.numIter = numIter;
    }

    /**
     * Global loss of the factorization.
     *
     * @param factorization matrix factorization
     * @param data preference data
     * @return the global loss
     */
    @Override
    public double error(UpdateableFactorization<U, I> factorization, FastUpdateablePreferenceData<U, I> data)
    {
        DenseDoubleMatrix2D p = factorization.getUserMatrix();
        DenseDoubleMatrix2D q = factorization.getItemMatrix();

        return error(p,q,data);
    }

    /**
     * Creates and calculates a factorization.
     *
     * @param K size of the latent feature space.
     * @param data preference data
     * @return a matrix factorization
     */
    @Override
    public UpdateableFactorization<U, I> factorize(int K, FastUpdateablePreferenceData<U, I> data)
    {
        DoubleFunction init = x -> sqrt(1.0/K) * Math.random();
        UpdateableFactorization<U,I> factorization = new UpdateableFactorization<>(data, data, K, init);
        this.factorize(factorization, data);

        return factorization;
    }

    /**
     * Calculates the factorization by using a previously generate matrix
     * factorization.
     *
     * @param factorization matrix factorization
     * @param data preference data
     */
    @Override
    public void factorize(UpdateableFactorization<U, I> factorization, FastUpdateablePreferenceData<U, I> data)
    {
        DenseDoubleMatrix2D p = factorization.getUserMatrix();
        DenseDoubleMatrix2D q = factorization.getItemMatrix();

        IntSet uidxs = new IntOpenHashSet(data.getUidxWithPreferences().toArray());
        IntStream.range(0, p.rows()).filter(uidx -> !uidxs.contains(uidx)).forEach(uidx -> p.viewRow(uidx).assign(0.0));
        IntSet iidxs = new IntOpenHashSet(data.getIidxWithPreferences().toArray());
        IntStream.range(0, q.rows()).filter(iidx -> !iidxs.contains(iidx)).forEach(iidx -> q.viewRow(iidx).assign(0.0));

        for (int t = 1; t <= numIter; t++) {
            long time0 = System.nanoTime();

            set_minQ(q, p, data);
            set_minP(p, q, data);

            int iter = t;
            long time1 = System.nanoTime() - time0;

            LOG.log(Level.INFO, String.format("iteration n = %3d t = %.2fs", iter, time1 / 1_000_000_000.0));
            LOG.log(Level.FINE, () -> String.format("iteration n = %3d e = %.6f", iter, error(factorization, data)));
        }
    }
    
    /**
     * Updates a factorization, when a new rating is received.
     * @param factorization the factorization.
     * @param u updated user
     * @param i updated item
     * @param weight the weight
     * @param data the updated data.
     */
    @Override
    public void update(UpdateableFactorization<U,I> factorization, FastUpdateablePreferenceData<U,I> data, U u, I i, double weight)
    {
        if(!factorization.containsUser(u))
        {
            int uidx = factorization.addUser(u);
            DoubleMatrix1D vector = set_minP(u, factorization.getUserMatrix(), factorization.getItemMatrix(), data);
            factorization.getUserMatrix().viewRow(uidx).assign(vector);
        }
        else if(!factorization.containsItem(i))
        {
            int iidx = factorization.addItem(i);
            DoubleMatrix1D vector = set_minQ(i, factorization.getItemMatrix(), factorization.getUserMatrix(), data);
            factorization.getItemMatrix().viewRow(iidx).assign(vector);
        }
        else
        {
            int uidx = data.user2uidx(u);
            int iidx = data.item2iidx(i);
            DoubleMatrix1D userVector = set_minP(u, factorization.getUserMatrix(), factorization.getItemMatrix(), data);
            DoubleMatrix1D itemVector = set_minQ(i, factorization.getItemMatrix(), factorization.getUserMatrix(), data);


            factorization.getUserMatrix().viewRow(uidx).assign(userVector);
            factorization.getItemMatrix().viewRow(iidx).assign(itemVector);
        }
    }


    /**
     * Updates a factorization, when a new rating is removed.
     * @param factorization the factorization.
     * @param u updated user
     * @param i updated item
     * @param data the updated data.
     */
    @Override
    public void updateDelete(UpdateableFactorization<U,I> factorization, FastUpdateablePreferenceData<U,I> data, U u, I i)
    {
        if(data.containsUser(u) && data.containsItem(i))
        {
            int uidx = data.user2uidx(u);
            int iidx = data.item2iidx(i);
            DoubleMatrix1D userVector = set_minP(u, factorization.getUserMatrix(), factorization.getItemMatrix(), data);
            DoubleMatrix1D itemVector = set_minQ(i, factorization.getItemMatrix(), factorization.getUserMatrix(), data);

            factorization.getUserMatrix().viewRow(uidx).assign(userVector);
            factorization.getItemMatrix().viewRow(iidx).assign(itemVector);
        }
    }
    
    /**
     * Squared loss of two matrices.
     *
     * @param p user matrix
     * @param q item matrix
     * @param data preference data
     * @return squared loss
     */
    protected abstract double error(DenseDoubleMatrix2D p, DenseDoubleMatrix2D q, FastUpdateablePreferenceData<U, I> data);
    
    /**
     * User matrix least-squares step.
     *
     * @param p user matrix
     * @param q item matrix
     * @param data preference data
     */
    protected abstract void set_minP(DenseDoubleMatrix2D p, DenseDoubleMatrix2D q, FastUpdateablePreferenceData<U, I> data);

    /**
     * Item matrix least-squares step.
     *
     * @param q item matrix
     * @param p user matrix
     * @param data preference data
     */
    protected abstract void set_minQ(DenseDoubleMatrix2D q, DenseDoubleMatrix2D p, FastUpdateablePreferenceData<U, I> data);
    
    /**
     * User matrix least-squares step.
     *
     * @param u the user
     * @param p user matrix
     * @param q item matrix
     * @param data preference data
     * @return a vector containing the vector for user u
     */
    protected abstract DoubleMatrix1D set_minP(U u, DenseDoubleMatrix2D p, DenseDoubleMatrix2D q, FastUpdateablePreferenceData<U, I> data);

    /**
     * Item matrix least-squares step.
     *
     * @param i the item
     * @param q item matrix
     * @param p user matrix
     * @param data preference data
     * @return a vector containing the vector for item i
     */
    protected abstract DoubleMatrix1D set_minQ(I i, DenseDoubleMatrix2D q, DenseDoubleMatrix2D p, FastUpdateablePreferenceData<U, I> data);
    
    
    
}
