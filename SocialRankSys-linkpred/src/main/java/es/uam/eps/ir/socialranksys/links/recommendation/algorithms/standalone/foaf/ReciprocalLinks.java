/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.foaf;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.Random;

/**
 * Recommends reciprocal links (only useful for directed contact recommendation).
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class ReciprocalLinks<U> extends UserFastRankingRecommender<U>
{
    /**
     * Random number generator for solving draws.
     */
    private final Random random;

    /**
     * Constructor.
     * @param graph The original graph.
     */
    public ReciprocalLinks(FastGraph<U> graph) {
        this(graph, 0);
    }
    
    /**
     * Constructor.
     * @param graph The original graph.
     * @param randomseed A random seed for the RNG which solves draws
     */
    public ReciprocalLinks(FastGraph<U> graph, int randomseed)
    {
        super(graph);
        this.random = new Random(randomseed);
    }
            

    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        Int2DoubleMap scores = new Int2DoubleOpenHashMap();
        U u = this.uidx2user(uidx);
        this.iIndex.getAllIidx().forEach(iidx -> {
            if(this.getGraph().containsEdge(this.iidx2item(iidx), u))
            {
                scores.put(iidx, 1.0 + random.nextDouble());
            }
            else
            {
                scores.put(iidx, random.nextDouble());
            }
        });
        
        return scores;
    }   
}
