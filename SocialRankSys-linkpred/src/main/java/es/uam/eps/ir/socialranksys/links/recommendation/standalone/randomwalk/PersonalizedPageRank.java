/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.standalone.randomwalk;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.Map;

/**
 * Recommends an user by his PageRank score.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class PersonalizedPageRank<U> extends UserFastRankingRecommender<U>
{

    /**
     * Teleport rate
     */
    private final double r;
    
    /**
     * Constructor.
     * @param graph Graph
     * @param r Teleport rate.
     */
    public PersonalizedPageRank(FastGraph<U> graph, double r)
    {
        super(graph);
        this.r = r;
    }

    @Override
    public Int2DoubleMap getScoresMap(int i) {
        U u = this.uIndex.uidx2user(i);
        Int2DoubleMap scores = new Int2DoubleOpenHashMap();
        PageRank<U> pageRank = new PageRank<>(this.r,u);
        Map<U, Double> pageRanks = pageRank.compute(this.getGraph());
        pageRanks.forEach((key, value) -> scores.put(uIndex.user2uidx(key), value.doubleValue()));
        return scores;
    }    
}
