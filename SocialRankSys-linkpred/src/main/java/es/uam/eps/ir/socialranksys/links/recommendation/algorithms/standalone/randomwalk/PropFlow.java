/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.randomwalk;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommender which uses the PropFlow algorithm.
 * 
 * Lichtenwalter, R., Lussier, J., Chawla, N. New perspectives and methods in link prediction.
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class PropFlow<U> extends UserFastRankingRecommender<U>
{

    /**
     * Maximum distance from the target node (maximum length of the random walk).
     */
    private final int maxLength;
    /**
     * Orientation of the edges.
     */
    private final EdgeOrientation orientation;

    /**
     * Constructor.
     * @param graph the original graph.
     * @param maxLength maximum distance from the target node (maximum length of the random walk).
     * @param orientation the orientation of the edges.
     */
    public PropFlow(FastGraph<U> graph, int maxLength, EdgeOrientation orientation)
    {
        super(graph);
        this.maxLength = maxLength;
        this.orientation = orientation;
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        
        U u = uidx2user(uidx);
        Set<Integer> found = new HashSet<>();
        LinkedList<Integer> newSearch = new LinkedList<>();
        Int2DoubleMap propFlow = new Int2DoubleOpenHashMap();
        this.iIndex.getAllIidx().forEach(iidx -> propFlow.put(iidx, 0.0));

        
        found.add(uidx);
        newSearch.push(uidx);
        propFlow.put(uidx, 1.0);
        for(int j = 0; j < this.maxLength; ++j)
        {
            LinkedList<Integer> oldSearch = (LinkedList<Integer>) newSearch.clone();
            newSearch.clear();
            
            while(!oldSearch.isEmpty())
            {
                int userId = oldSearch.pop();
                U user = this.uidx2user(userId);
                double nodeInput = propFlow.get(userId);
                
                List<U> neighbourhood = this.getGraph().getNeighbourhood(user, orientation).collect(Collectors.toCollection(ArrayList::new));
                double sumOutput = neighbourhood.stream().mapToDouble(neigh -> 
                {
                    return switch (orientation)
                    {
                        case IN -> this.getGraph().getEdgeWeight(neigh, user);
                        case OUT -> this.getGraph().getEdgeWeight(user, neigh);
                        default -> Math.max(this.getGraph().getEdgeWeight(neigh, user), this.getGraph().getEdgeWeight(user, neigh));
                    };
                }).sum();
                
                
                double flow;
                for(U neigh : neighbourhood)
                {
                    int nIdx = this.user2uidx(neigh);
                    double weight = switch (orientation)
                    {
                        case IN -> this.getGraph().getEdgeWeight(neigh, user);
                        case OUT -> this.getGraph().getEdgeWeight(user, neigh);
                        default -> Math.max(this.getGraph().getEdgeWeight(neigh, user), this.getGraph().getEdgeWeight(user, neigh));
                    };
                    flow = nodeInput*weight/sumOutput;
                    
                    if(propFlow.containsKey(nIdx))
                    {
                        propFlow.put(nIdx, propFlow.get(nIdx)+flow);
                    }
                    else
                    {
                        propFlow.put(nIdx, flow);
                    }
                    
                    if(!found.contains(nIdx))
                    {
                        found.add(nIdx);
                        newSearch.push(nIdx);
                    }
                }              
            }            
        }
        return propFlow;
    }  
}
