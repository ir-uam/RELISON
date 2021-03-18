/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.twitter;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.bipartite.BipartiteRecommender;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.bipartite.MaximumCosineSimilarity;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Twitter maximum cosine: executes the maximum cosine over bipartite graphs from
 * the reduced graph.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class TwitterMaximumCosineSimilarity<U> extends TwitterRecommender<U>
{

    /**
     * Constructor.
     * @param graph         original graph.
     * @param circlesize    size of the circles of trust.
     * @param r             teleport rate for the circles of trust.
     */
    public TwitterMaximumCosineSimilarity(FastGraph<U> graph, int circlesize, double r)
    {
        super(graph, circlesize, r, MaximumCosineSimilarity::new);
    }
}
