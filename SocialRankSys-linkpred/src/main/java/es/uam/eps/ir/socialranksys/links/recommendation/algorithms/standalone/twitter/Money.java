/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.twitter;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.bipartite.BipartiteRecommender;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.randomwalk.PersonalizedHITS;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.randomwalk.PersonalizedSALSA;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Twitter Money algorithm. This algorithm applies a personalized SALSA algorithm over a circle of trust between
 * users.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.randomwalk.PersonalizedHITS
 */
public class Money<U> extends TwitterRecommender<U>
{
    /**
     * Constructor.
     * @param graph         original graph.
     * @param circlesize    size of the circles of trust.
     * @param r             teleport rate for the circles of trust.
     * @param mode          true for recommending authorities, false for recommending hubs.
     * @param alpha         teleport rate for the personalized SALSA algorithm.
     */
    public Money(FastGraph<U> graph, int circlesize, double r, boolean mode, double alpha)
    {
        super(graph, circlesize, r, circle -> new PersonalizedSALSA<>(circle, mode, alpha));
    }
}
