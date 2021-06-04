/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.twitter;

import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.bipartite.AverageCosineSimilarity;

/**
 * Twitter average cosine: executes the average cosine over bipartite graphs from
 * the reduced graph.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class TwitterAverageCosineSimilarity<U> extends TwitterRecommender<U>
{
    /**
     * Constructor.
     * @param graph         original graph.
     * @param circlesize    size of the circles of trust.
     * @param r             teleport rate for the circles of trust.
     */
    public TwitterAverageCosineSimilarity(FastGraph<U> graph, int circlesize, double r)
    {
        super(graph, circlesize, r, AverageCosineSimilarity::new);
    }
}
