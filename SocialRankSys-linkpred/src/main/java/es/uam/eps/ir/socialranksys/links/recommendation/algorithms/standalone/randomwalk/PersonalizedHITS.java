/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.randomwalk;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.bipartite.BipartiteRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Personalized HITS Recommender.
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users
 */
public class PersonalizedHITS<U> extends BipartiteRecommender<U>
{
    /**
     * Teleport rate.
     */
    private final double alpha;
    /**
     * Convergence threshold.
     */
    private static final double THRESHOLD = 0.01;
    /**
     * Constructor
     * @param graph     original graph.
     * @param mode      true to recommend authorities, false to recommend hubs.
     * @param alpha     the teleport rate.
     */
    public PersonalizedHITS(FastGraph<U> graph, boolean mode, double alpha)
    {
        super(graph, mode);
        this.alpha = alpha;
    }

    @Override
    public Int2DoubleMap getScoresMap(int i) 
    {
        U u = uIndex.uidx2user(i);
        Int2DoubleMap scores = new Int2DoubleOpenHashMap();
        Object2DoubleMap<U> hubsMap = new Object2DoubleOpenHashMap<>();
        Object2DoubleMap<U> authMap = new Object2DoubleOpenHashMap<>();
        hubsMap.defaultReturnValue(0.0);
        authMap.defaultReturnValue(0.0);

        hubsMap.put(u, 1.0);
        this.authorities.forEach((key, value) -> authMap.put(value, 1.0 / (this.authorities.size())));

        double diff;
        do // Compute Personalized HITS
        {
            Map<U, Double> auxAuth = new HashMap<>();
            Map<U, Double> auxHubs = new HashMap<>();
            this.authorities.forEach((key, user) ->
            {
                long bIdx = key;

                assert this.bipartiteGraph != null;
                double newAuthScore = this.bipartiteGraph.getIncidentNodes(bIdx)
                        .mapToDouble(wIdx -> hubsMap.getDouble(this.hubs.get(wIdx)))
                        .sum();
                auxAuth.put(user, newAuthScore);
            });
            
            double sumAuth = Math.sqrt(auxAuth.values().stream().mapToDouble(score -> score*score).sum());
            
            diff = this.authorities.values().stream().mapToDouble(v ->
            {
                double old = authMap.getDouble(v);
                double newAux = auxAuth.get(v) / sumAuth;
                authMap.put(v, newAux);
                return Math.abs(old - newAux);
            }).sum();
            
            this.hubs.forEach((key, user) ->
            {
                long bIdx = key;

                assert this.bipartiteGraph != null;
                double newHubScore = this.bipartiteGraph.getAdjacentNodes(bIdx)
                        .mapToDouble(wIdx -> authMap.getDouble(this.authorities.get(wIdx)))
                        .sum();
                newHubScore *= (1 - this.alpha);

                if (user.equals(u))
                {
                    newHubScore += this.alpha;
                }

                auxHubs.put(user, newHubScore);

            });
            
            double sumHubs = Math.sqrt(auxHubs.values().stream().mapToDouble(score -> score*score).sum());
            
            diff += this.hubs.values().stream().mapToDouble(v ->
            {
                double old = hubsMap.getDouble(v);
                double newAux = auxHubs.get(v) / sumHubs;
                hubsMap.put(v, newAux);
                return Math.abs(old - newAux);
            }).sum();
        }
        while(diff > THRESHOLD);
        
        if(this.mode) //Authorities
        {
            this.uIndex.getAllUsers().forEach(v -> 
            {
                int vIdx = this.uIndex.user2uidx(v);
                if(authMap.containsKey(v))
                {
                    scores.put(vIdx, authMap.getDouble(v));
                }
                else
                {
                    scores.put(vIdx,0.0);
                }
            });
        }
        else // Hubs
        {
            this.uIndex.getAllUsers().forEach(v -> 
            {
                int vIdx = this.uIndex.user2uidx(v);
                if(hubsMap.containsKey(v))
                {
                    scores.put(vIdx, hubsMap.getDouble(v));
                }
                else
                {
                    scores.put(vIdx,0.0);
                }
            });
        }
            
            
        return scores;
    }

    
    
}
