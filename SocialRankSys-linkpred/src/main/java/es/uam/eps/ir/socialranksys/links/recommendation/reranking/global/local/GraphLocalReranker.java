/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.generator.GraphCloneGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.function.Supplier;

/**
 * Generalization of greedy local reranking strategies, for processing several recommendations at a time.
 *
 * These rerankers, given a set of recommendations, sequentially process them one by one. Those
 * recommendations which are processed later are aware of the previously processed recommendations.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class GraphLocalReranker<U> extends LocalLambdaReranker<U,U>
{
    /**
     * The graph.
     */
    protected final Graph<U> graph;

    /**
     * Constructor.
     * @param cutOff    maximum length of the definitive ranking.
     * @param lambda    trade-off between the original and novelty scores
     * @param norm      the normalization strategy.
     * @param graph     the original network.
     */
    public GraphLocalReranker(int cutOff, double lambda, Supplier<Normalizer<U>> norm, Graph<U> graph)
    {
        this(cutOff, lambda, norm, 0, graph);
    }

    /**
     * Constructor.
     * @param cutOff    maximum length of the definitive ranking.
     * @param lambda    trade-off between the original and novelty scores
     * @param norm      the normalization strategy.
     * @param seed      the random seed.
     * @param graph     the original network.
     */
    public GraphLocalReranker(int cutOff, double lambda, Supplier<Normalizer<U>> norm, int seed, Graph<U> graph)
    {
        super(cutOff, lambda, norm, seed);
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
    }

    @Override
    protected void update(U user, Tuple2od<U> bestItemValue)
    {
        this.innerUpdate(user, bestItemValue);
        U v = bestItemValue.v1;

        if(this.graph.isDirected())
        {
            this.graph.addEdge(user, v);
        }
        else
        {
            // If this element does not contain the corresponding value...
            if(!this.graph.containsEdge(v, user))
            {
                this.graph.addEdge(user, v);
            }
        }
    }

    /**
     * Updates the different parameters of the reranker, further than changing the
     * edges in the graph.
     * @param user      the target user.
     * @param updated   the new candidate user.
     */
    protected abstract void innerUpdate(U user, Tuple2od<U> updated);

}
