/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.twitter;

import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.randomwalk.PersonalizedHITS;

/**
 * Twitter Love algorithm. This algorithm applies a personalized HITS algorithm over a circle of trust between
 * users.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see PersonalizedHITS
 */
public class Love<U> extends TwitterRecommender<U>
{
    /**
     * Constructor.
     * @param graph         original graph.
     * @param circlesize    size of the circles of trust.
     * @param r             teleport rate for the circles of trust.
     * @param mode          true for recommending authorities, false for recommending hubs.
     * @param alpha         teleport rate for the personalized HITS algorithm.
     */
    public Love(FastGraph<U> graph, int circlesize, double r, boolean mode, double alpha)
    {
        super(graph, circlesize, r, circle -> new PersonalizedHITS<>(circle, mode, alpha));
    }
}
