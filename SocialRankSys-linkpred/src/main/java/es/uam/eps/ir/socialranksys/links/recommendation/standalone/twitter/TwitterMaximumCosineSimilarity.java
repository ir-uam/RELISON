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
import es.uam.eps.ir.socialranksys.links.recommendation.standalone.bipartite.MaximumCosineSimilarity;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Twitter Money algorithm.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class TwitterMaximumCosineSimilarity<U> extends TwitterRecommender<U>
{

    /**
     * Constructor.
     * @param graph Original graph.
     * @param circlesize Size of the circles of trust.
     * @param r Teleport rate for the circles of trust.
     */
    public TwitterMaximumCosineSimilarity(FastGraph<U> graph, int circlesize, double r) {
        super(graph, circlesize, r);
    }

    @Override
    public Int2DoubleMap getScoresMap(int uIdx) 
    {
        Int2DoubleMap output = new Int2DoubleOpenHashMap();
        U u = uIndex.uidx2user(uIdx);
        
        FastGraph<U> graph = this.circles.get(u);
        BipartiteRecommender<U> rec = new MaximumCosineSimilarity<>(graph);
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
