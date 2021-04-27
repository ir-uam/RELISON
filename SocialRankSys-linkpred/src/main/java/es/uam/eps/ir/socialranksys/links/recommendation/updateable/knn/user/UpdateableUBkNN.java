/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.updateable.knn.user;

import es.uam.eps.ir.ranksys.nn.neighborhood.TopKNeighborhood;
import es.uam.eps.ir.socialranksys.links.data.updateable.preference.fast.updateable.FastUpdateablePreferenceData;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.fast.FastUpdateableRankingRecommender;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.knn.sim.UpdateableGraphSimilarity;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.jooq.lambda.tuple.Tuple3;

import java.util.stream.Stream;

/**
 * Updateable version of the user-based nearest-neighbors approach.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class UpdateableUBkNN<U> extends FastUpdateableRankingRecommender<U,U>
{
    /**
     * Updateable similarity between pairs of users.
     */
    private final UpdateableGraphSimilarity sim;
    /**
     * The number of neighbors to select.
     */
    private final int k;

    /**
     * Constructor.
     * @param prefData      preference data.
     * @param similarity    an updateable similarity between pairs of users.
     * @param k             the number of neighbors to select.
     */
    public UpdateableUBkNN(FastUpdateablePreferenceData<U,U> prefData, UpdateableGraphSimilarity similarity, int k)
    {
        super(prefData);
        this.k = k;
        this.sim = similarity;
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleOpenHashMap scores = new Int2DoubleOpenHashMap();
        scores.defaultReturnValue(0.0);
        TopKNeighborhood neighborhood = new TopKNeighborhood(this.sim, this.k);
        neighborhood.getNeighbors(uidx).forEach(neighbor ->
        {
            int widx = neighbor.v1;
            double sim = neighbor.v2;

            prefData.getUidxPreferences(widx).forEach(vW -> scores.addTo(vW.v1, vW.v2 * sim));
        });
        return scores;
    }


    @Override
    public void update(Stream<Tuple3<U, U, Double>> tuples)
    {
        tuples.forEach(t -> this.update(t.v1, t.v2, t.v3));
    }

    @Override
    public void updateDelete(Stream<Tuple3<U, U, Double>> tuples)
    {

    }

    @Override
    public void updateAddUser(U u)
    {
        this.sim.updateAddElement();
        this.prefData.updateAddUser(u);

    }

    @Override
    public void updateAddItem(U u)
    {
        this.updateAddUser(u);
    }

    @Override
    public void update(U u, U v, double val)
    {
        this.sim.updateAdd(this.user2uidx(u), this.user2uidx(v), val);
        this.prefData.update(u, v, val);
    }

    @Override
    public void updateDelete(U u, U u2, double val)
    {

    }
}
