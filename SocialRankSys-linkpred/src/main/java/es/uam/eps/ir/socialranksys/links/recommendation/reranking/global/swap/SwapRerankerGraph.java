/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.graph.Graph;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * GReranker for optimizing a certain global property of the graph (e.g. Clustering Coefficient)
 * @author Javier Sanz-Cruzado Puig.
 * @param <U> Type of the users.
 */
public abstract class SwapRerankerGraph<U> extends SwapLambdaReranker<U,U> 
{

    /**
     * The graph.
     */
    protected final Graph<U> graph;
    /**
     * The recommendations
     */
    protected final Map<U, Set<U>> recs;
    
    /**qq
     * Constructor
     * @param cutOff Cutoff of the reranker.
     * @param lambda Trade-off between relevance and the global metric.
     * @param norm True if ratings have to be normalized.
     * @param rank True if the normalization is by ranking position, false if it is by score
     * @param graph The graph.
     */
    public SwapRerankerGraph(double lambda, int cutOff, boolean norm, boolean rank, Graph<U> graph)
    {
        super(lambda, cutOff, norm, rank);
        Cloner cloner = new Cloner();
        this.graph = cloner.deepClone(graph);
        this.recs = new HashMap<>();
    }

    @Override
    protected double nov(U u, Tuple2od<U> newValue, Tuple2od<U> oldValue)
    {
        
        U v = oldValue.v1;
        U w = newValue.v1;
        
        if(this.graph.isDirected())
        {
            return novAddDelete(u, newValue, oldValue);
        }
        else if(recs.get(v).contains(u) && recs.get(w).contains(u))
        {
            return this.globalvalue;
        }
        else if(recs.get(v).contains(u))
        {
            return novAdd(u, newValue, oldValue);
        }
        else if(recs.get(w).contains(u))
        {
            return novDelete(u, newValue, oldValue);
        }
        else
        {
            return novAddDelete(u, newValue, oldValue);
        }
    }
    
    /**
     * Computes the novelty if an edge is replaced by other
     * @param u User to be recommended.
     * @param newValue The new recommendation
     * @param oldValue The old recommendation
     * @return the novelty score.
     */
    protected abstract double novAddDelete(U u, Tuple2od<U> newValue, Tuple2od<U> oldValue);
    /**
     * Computes the novelty if an edge is added to the graph
     * @param u User to be recommended.
     * @param newValue The new recommendation
     * @param oldValue The old recommendation
     * @return the novelty score.
     */
    protected abstract double novAdd(U u, Tuple2od<U> newValue, Tuple2od<U> oldValue);
    /**
     * Computes the novelty if an edge is removed from the graph
     * @param u User to be recommended.
     * @param newValue The new recommendation
     * @param oldValue The old recommendation
     * @return the novelty score.
     */
    protected abstract double novDelete(U u, Tuple2od<U> newValue, Tuple2od<U> oldValue);

    @Override
    public Stream<Recommendation<U, U>> rerankRecommendations(Stream<Recommendation<U, U>> recommendation, int maxLength)
    {
        List<Recommendation<U,U>> recommendations = recommendation.collect(Collectors.toCollection(ArrayList::new));
        for(Recommendation<U,U> rec : recommendations)
        {
            U u = rec.getUser();
            this.recs.put(u, new HashSet<>());
            List<Tuple2od<U>> items = rec.getItems();
            for(int i = 0; i < Math.min(maxLength, cutOff);++i)
            {
                Tuple2od<U> val = items.get(i);
                U v = val.v1;
                this.graph.addEdge(u, v);
                
                this.recs.get(u).add(v);
            }
        }
        
        this.computeGlobalValue();
        return super.rerankRecommendations(recommendations.stream(), maxLength);
    }
    
    @Override
    protected void update(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        this.innerUpdate(user, updated, old);
        
        this.recs.get(user).remove(old.v1);
        this.recs.get(user).add(updated.v1);
        
        if(this.graph.isDirected())
        {
            this.graph.removeEdge(user, old.v1);
            this.graph.addEdge(user, updated.v1);
        }
        else 
        {
            if(!this.recs.get(old.v1).contains(user))
                this.graph.removeEdge(user, old.v1);
            if(!this.recs.get(updated.v1).contains(user))
                this.graph.addEdge(user, old.v1);
        }
        
    }

    /**
     * Computes the global value of the property we want to enhance.
     */
    protected abstract void computeGlobalValue();

    /**
     * Updates the different parameters of the reranker, further than changing the
     * edges in the graph.
     * @param user the target user.
     * @param updated the new candidate user.
     * @param old the old candidate user.
     */
    protected abstract void innerUpdate(U user, Tuple2od<U> updated, Tuple2od<U> old);

    
}
