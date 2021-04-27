/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.updateable.fast;

import es.uam.eps.ir.ranksys.fast.FastRecommendation;
import es.uam.eps.ir.ranksys.fast.utils.topn.IntDoubleTopN;
import es.uam.eps.ir.socialranksys.links.data.updateable.preference.fast.updateable.FastUpdateablePreferenceData;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

import static java.lang.Integer.min;
import static java.util.stream.Collectors.toList;

/**
 * Fast updateable recommender that ranks the user. It can be updated over time.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class FastUpdateableRankingRecommender<U,I> extends AbstractFastUpdateableRecommender<U,I> 
{
    /**
     * Constructor.
     * @param prefData the updateable preference data.
     */
    public FastUpdateableRankingRecommender(FastUpdateablePreferenceData<U, I> prefData)
    {
        super(prefData);
    }

    @Override
    public FastRecommendation getRecommendation(int uidx, int maxLength, IntPredicate filter)
    {
        if (uidx == -1) {
            return new FastRecommendation(uidx, new ArrayList<>(0));
        }

        Int2DoubleMap scoresMap = getScoresMap(uidx);

        final IntDoubleTopN topN = new IntDoubleTopN(min(maxLength, scoresMap.size()));
        scoresMap.int2DoubleEntrySet().forEach(e -> {
            int iidx = e.getIntKey();
            double score = e.getDoubleValue();
            if (filter.test(iidx)) {
                topN.add(iidx, score);
            }
        });

        topN.sort();

        List<Tuple2id> items = topN.reverseStream()
                .collect(toList());

        return new FastRecommendation(uidx, items);
    }

    /**
     * Returns a map of item-score pairs.
     *
     * @param uidx index of the user whose scores are predicted
     * @return a map of item-score pairs
     */
    public abstract Int2DoubleMap getScoresMap(int uidx);
}
