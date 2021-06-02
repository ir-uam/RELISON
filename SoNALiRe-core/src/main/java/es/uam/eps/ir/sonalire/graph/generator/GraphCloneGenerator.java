/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.graph.generator;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.sonalire.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.sonalire.graph.multigraph.MultiGraph;

import java.util.List;

/**
 * Class for cloning graphs.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class GraphCloneGenerator<U> implements GraphGenerator<U>
{
    /**
     * Graph to clone
     */
    private Graph<U> graph;
    /**
     * Indicates if the graph has been generated
     */
    private boolean configured = false;

    /**
     * Configure the graph generator
     *
     * @param graph the graph to clone
     */
    public void configure(Graph<U> graph)
    {
        this.graph = graph;
        this.configured = true;
    }

    @Override
    public void configure(Object... configuration)
    {
        if (!(configuration == null) && configuration.length == 1)
        {
            Graph<U> auxGraph = (Graph<U>) configuration[0];
            this.configure(auxGraph);
        }
    }

    @Override
    public Graph<U> generate() throws GeneratorNotConfiguredException, GeneratorBadConfiguredException
    {
        if (!this.configured)
        {
            throw new GeneratorNotConfiguredException("Graph cloner: Generator was not configured");
        }
        else if (this.graph == null)
        {
            throw new GeneratorNotConfiguredException("Graph cloner: Generator was badly configured");
        }

        if (this.graph.isMultigraph()) // Clone a multigraph
        {
            GraphGenerator<U> emptyGraphGen = new EmptyMultiGraphGenerator<>();
            emptyGraphGen.configure(graph.isDirected(), graph.isWeighted());
            MultiGraph<U> newGraph = (MultiGraph<U>) emptyGraphGen.generate();
            MultiGraph<U> current = (MultiGraph<U>) graph;

            current.getAllNodes().forEach(newGraph::addNode);
            current.getAllNodes().forEach(u -> current.getAdjacentNodes(u).forEach(v ->
                                                                                   {
                                                                                       List<Double> weights = current.getEdgeWeights(u, v);
                                                                                       List<Integer> types = current.getEdgeTypes(u, v);

                                                                                       for (int i = 0; i < weights.size(); ++i)
                                                                                       {
                                                                                           newGraph.addEdge(u, v, weights.get(i), types.get(i));
                                                                                       }
                                                                                   }));

            return newGraph;
        }
        else // Clone a simple graph
        {
            GraphGenerator<U> emptyGraphGen = new EmptyGraphGenerator<>();
            emptyGraphGen.configure(graph.isDirected(), graph.isWeighted());
            Graph<U> newGraph = emptyGraphGen.generate();

            graph.getAllNodes().forEach(newGraph::addNode);
            graph.getAllNodes().forEach(u -> graph.getAdjacentNodes(u).forEach(v ->
                                                                               {
                                                                                   double weight = graph.getEdgeWeight(u, v);
                                                                                   int type = graph.getEdgeType(u, v);
                                                                                   newGraph.addEdge(u, v, weight, type);
                                                                               }));

            return newGraph;
        }
    }

}
