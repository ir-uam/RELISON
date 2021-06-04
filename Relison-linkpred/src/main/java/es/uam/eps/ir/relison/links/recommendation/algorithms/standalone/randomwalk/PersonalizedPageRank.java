/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.randomwalk;

import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.recommendation.UserFastRankingRecommender;
import es.uam.eps.ir.relison.metrics.vertex.PageRank;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.Map;

/**
 * Recommends an user by her personalized PageRank score.
 *
 * <p><b>Reference: </b> S. White, P. Smyth. Algorithms for Estimating Relative Importance in Networks. 9th Annual ACM SIGKDD International Conference on Knowledge Discovery and Data Mining (KDD 2003)</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users
 */
public class PersonalizedPageRank<U> extends UserFastRankingRecommender<U>
{

    /**
     * Teleport rate.
     */
    private final double r;
    
    /**
     * Constructor.
     * @param graph     the training graph.
     * @param r         teleport rate.
     */
    public PersonalizedPageRank(FastGraph<U> graph, double r)
    {
        super(graph);
        this.r = r;
    }

    @Override
    public Int2DoubleMap getScoresMap(int i)
    {
        U u = this.uIndex.uidx2user(i);
        Int2DoubleMap scores = new Int2DoubleOpenHashMap();
        PageRank<U> pageRank = new PageRank<>(this.r, u);
        Map<U, Double> pageRanks = pageRank.compute(this.getGraph());
        pageRanks.forEach((key, value) -> scores.put(uIndex.user2uidx(key), value.doubleValue()));
        return scores;
    }    
}
