/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.standalone.twitter;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.standalone.bipartite.BipartiteRecommender;
import es.uam.eps.ir.socialranksys.links.recommendation.standalone.randomwalk.PersonalizedSALSA;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Twitter Money algorithm.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 */
public class Money<U> extends TwitterRecommender<U>
{
    /**
     * True for authorities, false for hubs
     */
    private final boolean mode;
    /**
     * Teleport rate for the Money algorithm.
     */
    private final double alpha;
    /**
     * Constructor.
     * @param graph Original graph.
     * @param circlesize Size of the circles of trust.
     * @param r Teleport rate for the circles of trust.
     * @param mode true for recommending authorities, false for recommending hubs.
     * @param alpha teleport rate for the Money algorithm.
     */
    public Money(FastGraph<U> graph, int circlesize, double r, boolean mode, double alpha) {
        super(graph, circlesize, r);
        
        this.mode = mode;
        this.alpha = alpha;
    }

    @Override
    public Int2DoubleMap getScoresMap(int uIdx) 
    {
        Int2DoubleMap output = new Int2DoubleOpenHashMap();
        U u = uIndex.uidx2user(uIdx);
        
        FastGraph<U> graph = this.circles.get(u);
        BipartiteRecommender<U> rec = new PersonalizedSALSA<>(graph, mode, alpha);
        Int2DoubleMap scores = rec.getScoresMap(rec.user2uidx(u));
        
        iIndex.getAllIidx().forEach(iIdx -> {
            if(scores.containsKey(iIdx))
            {
                output.put(iIdx, scores.get(iIdx));
            }
            else
            {
                output.put(iIdx, 0.0);
            }
        });
        
        return output;
    }
}
