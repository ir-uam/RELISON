/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.updateable.mf;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import es.uam.eps.ir.ranksys.fast.FastRecommendation;
import es.uam.eps.ir.ranksys.fast.utils.topn.IntDoubleTopN;
import es.uam.eps.ir.sonalire.links.data.updateable.preference.fast.updateable.FastUpdateablePreferenceData;
import es.uam.eps.ir.sonalire.links.recommendation.updateable.fast.AbstractFastUpdateableRecommender;
import org.jooq.lambda.tuple.Tuple3;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.min;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static org.ranksys.core.util.tuples.Tuples.tuple;

/**
 * Matrix factorization recommender. Scores are calculated as the inner product of user and item vectors.
 * The recommendation algorithm can be updated over time.
 *
 * @author Saúl Vargas (saul.vargas@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the items
 */
public class MFUpdateableRecommender<U, I> extends AbstractFastUpdateableRecommender<U, I>
{
    /**
     * The current matrix factorization.
     */
    private final UpdateableFactorization<U, I> factorization;
    /**
     * The factorizer.
     */
    private final UpdateableFactorizer<U,I> factorizer;

    /**
     * Constructor.
     *
     * @param dataPref      preferences
     * @param factorization matrix factorization
     * @param factorizer    the factorizer.
     */
    public MFUpdateableRecommender(FastUpdateablePreferenceData<U,I> dataPref, UpdateableFactorization<U, I> factorization, UpdateableFactorizer<U,I> factorizer) {
        super(dataPref);
        this.factorization = factorization;
        this.factorizer = factorizer;
    }

    @Override
    public FastRecommendation getRecommendation(int uidx, int maxLength, IntPredicate filter) {
        DoubleMatrix1D pu;

        pu = factorization.getUserVector(uidx2user(uidx));
        if (pu == null) {
            return new FastRecommendation(uidx, new ArrayList<>());
        }

        IntDoubleTopN topN = new IntDoubleTopN(min(maxLength, factorization.numItems()));
        DoubleMatrix1D r = factorization.getItemMatrix().zMult(pu, null);
        for (int iidx = 0; iidx < r.size(); iidx++) {
            if (filter.test(iidx)) {
                topN.add(iidx, r.getQuick(iidx));
            }
        }

        topN.sort();

        List<Tuple2id> items = topN.reverseStream()
                .collect(toList());

        return new FastRecommendation(uidx, items);
    }

    @Override
    public FastRecommendation getRecommendation(int uidx, IntStream candidates) {
        DoubleMatrix1D pu;

        pu = factorization.getUserVector(uidx2user(uidx));
        if (pu == null) {
            return new FastRecommendation(uidx, new ArrayList<>());
        }

        DenseDoubleMatrix2D q = factorization.getItemMatrix();

        List<Tuple2id> items = candidates
                .mapToObj(iidx -> tuple(iidx, q.viewRow(iidx).zDotProduct(pu)))
                .sorted(comparingDouble(Tuple2id::v2).reversed())
                .collect(toList());

        return new FastRecommendation(uidx, items);
    }

    @Override
    public void updateAddUser(U u)
    {
        this.factorization.addUser(u);
    }
    
    @Override
    public void updateAddItem(I i)
    {
        this.factorization.addItem(i);
    }
    
    @Override
    public void update(Stream<Tuple3<U, I, Double>> tuples)
    {
        tuples.forEach(t ->
        {
            if(this.containsUser(t.v1) && this.containsItem(t.v2))
            {
                prefData.update(t.v1,t.v2,t.v3);
                this.factorizer.update(factorization, prefData,t.v1, t.v2, t.v3);
            }
        });
    }

    @Override
    public void update(U u, I i, double val)
    {
        if(this.containsUser(u) && this.containsItem(i))
        {
            prefData.update(u,i,val);
            this.factorizer.update(factorization, prefData,u,i,val);
        }
    }

    @Override
    public void updateDelete(Stream<Tuple3<U, I, Double>> tuples)
    {
        tuples.forEach(t ->
        {
            prefData.updateDelete(t.v1(),t.v2());
            this.factorizer.updateDelete(factorization, prefData,t.v1(),t.v2());
        });
    }

    @Override
    public void updateDelete(U u, I i, double val)
    {
        prefData.updateDelete(u,i);
        this.factorizer.updateDelete(factorization, prefData,u,i);
    }
}