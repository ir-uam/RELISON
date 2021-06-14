/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.swap;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.generator.GraphCloneGenerator;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract implementation of the swap reranking strategy for the contact recommendation context.
 * The properties we try to optimize here are global properties of social networks (e.g. clustering coefficient).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public abstract class GraphSwapReranker<U> extends SwapLambdaReranker<U,U>
{
    /**
     * The graph.
     */
    protected final Graph<U> graph;
    /**
     * The recommendations
     */
    protected final Map<U, Set<U>> recs;
    
    /**
     * Constructor
     * @param cutOff    length of the definitive recommendation rankings.
     * @param lambda    trade-off between relevance and the global metric.
     * @param norm      the normalization strategy.
     * @param graph     the original graph.
     */
    public GraphSwapReranker(double lambda, int cutOff, Supplier<Normalizer<U>> norm, Graph<U> graph)
    {
        super(lambda, cutOff, norm);
        Graph<U> graph1;
        GraphCloneGenerator<U> cloner = new GraphCloneGenerator<>();
        cloner.configure(graph);
        try
        {
            graph1 = cloner.generate();
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException e)
        {
            e.printStackTrace();
            graph1 = null;
        }

        this.graph = graph1;
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
     * @param u         user to be recommended.
     * @param newValue  the new recommendation
     * @param oldValue  the old recommendation
     * @return the novelty score.
     */
    protected abstract double novAddDelete(U u, Tuple2od<U> newValue, Tuple2od<U> oldValue);
    /**
     * Computes the novelty if an edge is added to the graph
     * @param u         user to be recommended.
     * @param newValue  the new recommendation
     * @param oldValue  the old recommendation
     * @return the novelty score.
     */
    protected abstract double novAdd(U u, Tuple2od<U> newValue, Tuple2od<U> oldValue);
    /**
     * Computes the novelty if an edge is removed from the graph
     * @param u         user to be recommended.
     * @param newValue  the new recommendation
     * @param oldValue  the old recommendation
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

            int length = items.size();
            length = Math.min(maxLength, length);


            for(int i = 0; i < Math.min(length, cutOff);++i)
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
     * @param user      the target user.
     * @param updated   the new candidate user.
     * @param old       the old candidate user.
     */
    protected abstract void innerUpdate(U user, Tuple2od<U> updated, Tuple2od<U> old);

    
}
