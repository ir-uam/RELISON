/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.algorithms.standalone.randomwalks;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.relison.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.randomwalk.CommuteTime;
import es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.randomwalk.PersonalizedPageRankHittingTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmIdentifiers.COMMUTEPERS;

/**
 * Grid search generator for PageRank algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see CommuteTime
 */
public class CommuteTimePersPageRankGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for the teleport parameter
     */
    private final static String R = "r";

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();

        List<Double> rs = grid.getDoubleValues(R);
        rs.forEach(r -> recs.put(COMMUTEPERS + "_" + r, (graph, prefData) -> new CommuteTime<>(graph, new PersonalizedPageRankHittingTime<>(graph, r))));
        return recs;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();

        List<Double> rs = grid.getDoubleValues(R);
        rs.forEach(r -> recs.put(COMMUTEPERS + "_" + r, () -> new CommuteTime<>(graph, new PersonalizedPageRankHittingTime<>(graph, r))));
        return recs;
    }

}
