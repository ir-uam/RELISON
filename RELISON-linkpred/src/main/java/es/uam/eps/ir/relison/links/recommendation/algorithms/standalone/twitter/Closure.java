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
import es.uam.eps.ir.relison.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Closure recommender. Recommends reciprocal edges according to the number of common neighbors between
 * the already existing edge endpoints.
 *
 * <p><b>Reference: </b> P. Gupta, A. Goel, J. Lin, A. Sharma, D. Wang, R. Zadeh. WTF: The Who to Follow Service at Twitter. 22nd Annual International Conference on World Wide Web (WWW 2013), 505-514 (2013).</p>
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Closure<U> extends UserFastRankingRecommender<U>
{
    
    /**
     * Constructor.
     * @param graph graph.
     */
    public Closure(FastGraph<U> graph)
    {
        super(graph);

    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        Int2DoubleMap scoresMap = new Int2DoubleOpenHashMap();
        
        U u = this.uidx2user(uidx);
        Set<U> uWs = this.getGraph().getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
        this.uIndex.getAllUidx().forEach(iidx -> 
        {
            U v = this.uidx2user(iidx);
            if(this.getGraph().containsEdge(v, u))
            {
                Set<U> vWs = this.getGraph().getIncidentNodes(v).collect(Collectors.toCollection(HashSet::new));
                if(!uWs.isEmpty() && !vWs.isEmpty())
                {
                    vWs.retainAll(uWs);
                    scoresMap.put(iidx, vWs.size() + 0.0);
                }
                else
                {
                    scoresMap.put(iidx, 0.0);
                }
            }
            else
            {
                scoresMap.put(iidx, 0.0);
            }
        });

        return scoresMap;
    }
}
