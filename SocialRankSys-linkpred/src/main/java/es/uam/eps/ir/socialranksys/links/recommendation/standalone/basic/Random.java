/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.standalone.basic;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Recommends users randomly.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Random<U> extends UserFastRankingRecommender<U>
{
    /**
     * Random number generator.
     */
    private final java.util.Random random;

    /**
     * Constructor.
     *
     * @param graph Graph
     */
    public Random(FastGraph<U> graph)
    {
        this(graph, System.currentTimeMillis());
    }

    /**
     * Constructor.
     *
     * @param graph The graph representing the social networks.
     * @param seed  The seed for the random number generator.
     */
    public Random(FastGraph<U> graph, long seed)
    {
        super(graph);
        this.random = new java.util.Random(seed);
    }

    @Override
    public Int2DoubleMap getScoresMap(int i)
    {
        Int2DoubleMap map = new Int2DoubleOpenHashMap();
        map.defaultReturnValue(Double.NEGATIVE_INFINITY);
        U u = uIndex.uidx2user(i);

        iIndex.getAllIidx().forEach(iidx -> map.put(iidx, random.nextDouble()));

        return map;
    }

}
