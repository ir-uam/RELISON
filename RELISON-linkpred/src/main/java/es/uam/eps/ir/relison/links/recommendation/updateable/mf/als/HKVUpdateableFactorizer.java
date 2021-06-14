/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.updateable.mf.als;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.LUDecompositionQuick;
import es.uam.eps.ir.relison.links.data.updateable.preference.fast.updateable.FastUpdateablePreferenceData;
import es.uam.eps.ir.relison.links.data.updateable.preference.fast.updateable.TransposedUpdateablePreferenceData;

import java.util.function.DoubleUnaryOperator;

/**
 * Implicit matrix factorization of Hu, Koren and Volinsky.
 * <p>
 * <b>Reference: </b>Y. Hu, Y. Koren, C. Volinsky. Collaborative filtering for implicit feedback
 * datasets. 8th Annual IEEE International Conference on Data Mining (ICDM 2008), 263-272 (2008).
 * </p>
 *
 * @author Saúl Vargas (saul.vargas@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the items
 */
public class HKVUpdateableFactorizer<U, I> extends ALSUpdateableFactorizer<U, I>
{
    /**
     * An algebra.
     */
    private static final Algebra ALG = new Algebra();
    /**
     * Regularization factor for the user vectors.
     */
    private final double lambdaP;
    /**
     * Regularization factor for the item vectors.
     */
    private final double lambdaQ;
    /**
     * Confidence function.
     */
    private final DoubleUnaryOperator confidence;

    /**
     * Constructor. Same regularization factor for user and item matrices.
     *
     * @param lambda        regularization factor
     * @param confidence    confidence function
     * @param numIter       number of iterations
     */
    public HKVUpdateableFactorizer(double lambda, DoubleUnaryOperator confidence, int numIter) {
        this(lambda, lambda, confidence, numIter);
    }

    /**
     * Constructor. Different regularization factors for user and item matrices.
     *
     * @param lambdaP       regularization factor for user matrix
     * @param lambdaQ       regularization factor for item matrix
     * @param confidence    confidence function
     * @param numIter       number of iterations
     */
    public HKVUpdateableFactorizer(double lambdaP, double lambdaQ, DoubleUnaryOperator confidence, int numIter) {
        super(numIter);
        this.lambdaP = lambdaP;
        this.lambdaQ = lambdaQ;
        this.confidence = confidence;
    }

    @Override
    public double error(DenseDoubleMatrix2D p, DenseDoubleMatrix2D q, FastUpdateablePreferenceData<U, I> data) {
        // TODO: add regularization

        return data.getUidxWithPreferences().parallel().mapToDouble(uidx -> {
            DoubleMatrix1D pu = p.viewRow(uidx);
            DoubleMatrix1D su = q.zMult(pu, null);

            double err1 = data.getUidxPreferences(uidx).mapToDouble(iv -> {
                double rui = iv.v2;
                double sui = su.getQuick(iv.v1);
                double cui = confidence.applyAsDouble(rui);
                return cui * (rui - sui) * (rui - sui) - confidence.applyAsDouble(0) * sui * sui;
            }).sum();

            double err2 = confidence.applyAsDouble(0) * su.assign(x -> x * x).zSum();

            return (err1 + err2) / data.numItems();
        }).sum() / data.numUsers();

    }

    @Override
    public void set_minP(final DenseDoubleMatrix2D p, final DenseDoubleMatrix2D q, FastUpdateablePreferenceData<U, I> data) {
        set_min(p, q, confidence, lambdaP, data);
    }

    @Override
    public void set_minQ(final DenseDoubleMatrix2D q, final DenseDoubleMatrix2D p, FastUpdateablePreferenceData<U, I> data) {
        set_min(q, p, confidence, lambdaQ, new TransposedUpdateablePreferenceData<>(data));
    }

    private static <U, I, O> void set_min(final DenseDoubleMatrix2D p, final DenseDoubleMatrix2D q, DoubleUnaryOperator confidence, double lambda, FastUpdateablePreferenceData<U, I> data) {
        final int K = p.columns();

        DenseDoubleMatrix2D A1P = new DenseDoubleMatrix2D(K, K);
        q.zMult(q, A1P, 1.0, 0.0, true, false);
        for (int k = 0; k < K; k++) {
            A1P.setQuick(k, k, lambda + A1P.getQuick(k, k));
        }

        DenseDoubleMatrix2D[] A2P = new DenseDoubleMatrix2D[q.rows()];
        data.getIidxWithPreferences().parallel().forEach(iidx -> {
            A2P[iidx] = new DenseDoubleMatrix2D(K, K);
            DoubleMatrix1D qi = q.viewRow(iidx);
            ALG.multOuter(qi, qi, A2P[iidx]);
        });

        data.getUidxWithPreferences().parallel().forEach(uidx -> {
            DoubleMatrix2D A = new DenseDoubleMatrix2D(K, K);
            DoubleMatrix1D b = new DenseDoubleMatrix1D(K);
            A.assign(A1P);
            b.assign(0.0);

            data.getUidxPreferences(uidx).forEach(iv -> {
                int iidx = iv.v1;
                double rui = iv.v2;
                double cui = confidence.applyAsDouble(rui);

                DoubleMatrix1D qi = q.viewRow(iidx);

                A.assign(A2P[iidx], (x, y) -> x + y * (cui - 1.0));
                b.assign(qi, (x, y) -> x + y * rui * cui);
            });
            LUDecompositionQuick lu = new LUDecompositionQuick(0);
            lu.decompose(A);
            lu.solve(b);
            p.viewRow(uidx).assign(b);
        });
    }

    private static <U, I, O> DoubleMatrix1D set_min(int idx, final DenseDoubleMatrix2D p, final DenseDoubleMatrix2D q, DoubleUnaryOperator confidence, double lambda, FastUpdateablePreferenceData<U, I> data)
    {
        final int K = p.columns();

        DenseDoubleMatrix2D A1P = new DenseDoubleMatrix2D(K, K);
        q.zMult(q, A1P, 1.0, 0.0, true, false);
        for (int k = 0; k < K; k++) {
            A1P.setQuick(k, k, lambda + A1P.getQuick(k, k));
        }

        DenseDoubleMatrix2D[] A2P = new DenseDoubleMatrix2D[q.rows()];
        data.getIidxWithPreferences().parallel().forEach(iidx -> {
            A2P[iidx] = new DenseDoubleMatrix2D(K, K);
            DoubleMatrix1D qi = q.viewRow(iidx);
            ALG.multOuter(qi, qi, A2P[iidx]);
        });


        DoubleMatrix2D A = new DenseDoubleMatrix2D(K, K);
        DoubleMatrix1D b = new DenseDoubleMatrix1D(K);
        A.assign(A1P);
        b.assign(0.0);

        data.getUidxPreferences(idx).forEach(iv -> {
            int iidx = iv.v1;
            double rui = iv.v2;
            double cui = confidence.applyAsDouble(rui);

            DoubleMatrix1D qi = q.viewRow(iidx);

            A.assign(A2P[iidx], (x, y) -> x + y * (cui - 1.0));
            b.assign(qi, (x, y) -> x + y * rui * cui);
        });

        LUDecompositionQuick lu = new LUDecompositionQuick(0);
        lu.decompose(A);
        lu.solve(b);

        return b;

    }

    @Override
    protected DoubleMatrix1D set_minP(U u, DenseDoubleMatrix2D p, DenseDoubleMatrix2D q, FastUpdateablePreferenceData<U, I> data)
    {
        int uidx = data.user2uidx(u);
        return set_min(uidx, p, q, this.confidence, this.lambdaP, data);
    }




    @Override
    protected DoubleMatrix1D set_minQ(I i, DenseDoubleMatrix2D q, DenseDoubleMatrix2D p, FastUpdateablePreferenceData<U, I> data) {
        int iidx = data.item2iidx(i);
        return set_min(iidx, q, p, this.confidence, this.lambdaQ, new TransposedUpdateablePreferenceData<>(data));
    }


}