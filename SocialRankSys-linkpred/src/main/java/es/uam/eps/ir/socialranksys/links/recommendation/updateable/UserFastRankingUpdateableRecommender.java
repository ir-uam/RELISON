/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.updateable;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import org.jooq.lambda.tuple.Tuple3;

import java.util.stream.Stream;

/**
 * Abstract class for user recommendation in social networks. The recommendation algorithm can be updated over time.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public abstract class UserFastRankingUpdateableRecommender<U> extends UserFastRankingRecommender<U> implements UpdateableRecommender<U,U>
{
    /**
     * Constructor.
     * @param graph a fast graph representing the social network.
     */
    public UserFastRankingUpdateableRecommender(FastGraph<U> graph)
    {
        super(graph);
    }
        
    /**
     * Re-trains the recommender after receiving a set of preferences
     * @param newPrefs the new preferences
     */
    @Override
    public void update(Stream<Tuple3<U,U,Double>> newPrefs)
    {
        newPrefs.forEach(triplet -> 
        {
            U u = triplet.v1;
            U v = triplet.v2;
            double w = triplet.v3;
            
            if(!this.graph.containsVertex(u))
            {
                this.graph.addNode(u);
            }
            if(!this.graph.containsVertex(v))
            {
                this.graph.addNode(v);
            }
            if(this.graph.containsEdge(u, v))
            {
                this.graph.updateEdgeWeight(u, v, w);
            }
            else
            {
                this.graph.addEdge(u, v, w);
            }
        });
    }
    
    /**
     * Re-trains the recommender after removing some preferences
     * @param oldPrefs the preferences to remove
     */
    @Override
    public void updateDelete(Stream<Tuple3<U,U, Double>> oldPrefs)
    {
        oldPrefs.forEach(tuple -> 
        {
            U u = tuple.v1();
            U v = tuple.v2();
            
            if(this.graph.containsEdge(u, v))
            {
                this.graph.removeEdge(u, v);
            }
        });
    }
}
