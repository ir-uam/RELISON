/* 
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.randomwalk;

import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.Map;

/**
 * Recommends an user by her PageRank score.
 *
 * <p><b>Reference: </b> S. Brin, L. Page. The Anatomy of a Large-Scale Hypertextual Web Search Engine. 7th Annual International Conference on World Wide Web (WWW 1998), 107-117 (1998)</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class PageRank<U> extends UserFastRankingRecommender<U>
{

    /**
     * Teleport rate.
     */
    private final double r;

    /**
     * The PageRank scores.
     */
    private final Int2DoubleMap pr;

    /**
     * Constructor.
     * @param graph     the training graph.
     * @param r         teleport rate.
     */
    public PageRank(FastGraph<U> graph, double r)
    {
        super(graph);
        this.r = r;

        this.pr = new Int2DoubleOpenHashMap();
        es.uam.eps.ir.relison.sna.metrics.vertex.PageRank<U> pageRank = new es.uam.eps.ir.relison.sna.metrics.vertex.PageRank<>(this.r);
        Map<U, Double> pageRanks = pageRank.compute(this.getGraph());
        pageRanks.forEach((key, value) -> pr.put(uIndex.user2uidx(key), value.doubleValue()));
    }

    @Override
    public Int2DoubleMap getScoresMap(int i)
    {
        return pr;
    }    
}
