/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.GraphCloneGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.GraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.socialranksys.index.Index;

import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * Methods for filtering the users and edges from a graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Adapters
{
    /**
     * Given a graph, obtains a version of it without autoloops.
     *
     * @param <U>   Type of the users.
     * @param graph the original graph.
     *
     * @return a graph without autoloops.
     */
    public static <U> Graph<U> removeAutoloops(Graph<U> graph)
    {
        try
        {
            GraphGenerator<U> ggen = new EmptyGraphGenerator<>();
            ggen.configure(graph.isDirected(), graph.isWeighted());
            Graph<U> auxGraph = ggen.generate();

            graph.getAllNodes().forEach(auxGraph::addNode);
            graph.getAllNodes().forEach(u ->
                graph.getAdjacentNodesWeights(u).filter(v -> !u.equals(v.getIdx())).forEach(v ->
                {
                    double weight = v.getValue();
                    int type = graph.getEdgeType(u, v.getIdx());
                    auxGraph.addEdge(u, v.getIdx(), weight, type);
                }));

            return graph;
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }

    }

    /**
     * Given a graph, obtains a version of the graph with all the possible autoloops.
     *
     * @param <U>   Type of the users
     * @param graph the original graph
     *
     * @return a graph with all the autoloops added.
     */
    public static <U> Graph<U> addAllAutoloops(Graph<U> graph)
    {
        try
        {
            GraphGenerator<U> ggen = new EmptyGraphGenerator<>();
            ggen.configure(graph.isDirected(), graph.isWeighted());
            Graph<U> auxGraph = ggen.generate();

            graph.getAllNodes().forEach(u ->
            {
                auxGraph.addNode(u);
                auxGraph.addEdge(u, u);
            });
            graph.getAllNodes().forEach(u ->
                graph.getAdjacentNodesWeights(u).forEach(v ->
                {
                    double weight = v.getValue();
                    int type = graph.getEdgeType(u, v.getIdx());
                    auxGraph.addEdge(u, v.getIdx(), weight, type);
                }));

            return graph;
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }

    }

    /**
     * Given two graphs (test and training graphs), generates a new graph containing only the users present in the
     * second one.
     *
     * @param graph         the test graph.
     * @param trainingGraph the training graph.
     * @param <U>           type of the users.
     *
     * @return the graph if everything went OK, null otherwise.
     */
    public static <U> Graph<U> onlyTrainUsers(Graph<U> graph, Graph<U> trainingGraph)
    {
        try
        {
            GraphGenerator<U> ggen = new EmptyGraphGenerator<>();
            ggen.configure(graph.isDirected(), graph.isWeighted());
            Graph<U> auxGraph = ggen.generate();

            trainingGraph.getAllNodes().forEach(auxGraph::addNode);

            graph.getAllNodes().forEach(u ->
            {
                if (auxGraph.containsVertex(u))
                {
                    graph.getAdjacentNodesWeights(u).forEach(v ->
                    {
                        if (auxGraph.containsVertex(v.getIdx()))
                        {
                            auxGraph.addEdge(u, v.getIdx(), v.getValue());
                        }
                    });
                }
            });

            return auxGraph;
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }
    }

    /**
     * Given a weighted network, returns the unweighted version.
     *
     * @param graph the original graph.
     * @param <U>   type of the users.
     *
     * @return the unweighted network if everything is OK, null otherwise.
     */
    public static <U> Graph<U> unweighted(Graph<U> graph)
    {
        try
        {
            GraphGenerator<U> ggen = new EmptyGraphGenerator<>();
            ggen.configure(graph.isDirected(), false);
            Graph<U> auxGraph = ggen.generate();

            graph.getAllNodes().forEach(auxGraph::addNode);
            graph.getAllNodes().forEach(u -> graph.getAdjacentNodes(u).forEach(v -> auxGraph.addEdge(u, v)));
            return auxGraph;
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }
    }

    /**
     * Given a directed network, returns the undirected version
     *
     * @param graph the original graph.
     * @param <U>   type of the users
     *
     * @return the graph.
     */
    public static <U> Graph<U> undirected(Graph<U> graph)
    {
        try
        {
            if (!graph.isDirected())
            {
                GraphGenerator<U> ggen = new GraphCloneGenerator<>();
                ggen.configure(graph);
                return ggen.generate();
            }
            else
            {
                GraphGenerator<U> ggen = new EmptyGraphGenerator<>();
                ggen.configure(false, graph.isWeighted());
                Graph<U> auxGraph = ggen.generate();

                graph.getAllNodes().forEach(auxGraph::addNode);
                graph.getAllNodes().forEach(u -> graph.getNeighbourhoodWeights(u, EdgeOrientation.UND).forEach(v -> auxGraph.addEdge(u, v.getIdx(), v.getValue())));
                return auxGraph;
            }
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }
    }

    /**
     * Given a filter, it applies it to the edges in the graph.
     * @param graph     the original network.
     * @param filter    the filter to apply.
     * @param <U>       the type of the users.
     * @return the filtered graph.
     */
    public static <U> Graph<U> filteredGraph(Graph<U> graph, Function<U, IntPredicate> filter)
    {
        try
        {
            GraphGenerator<U> ggen = new EmptyGraphGenerator<>();
            ggen.configure(graph.isDirected(), graph.isWeighted());
            Graph<U> auxGraph = ggen.generate();

            graph.getAllNodes().forEach(auxGraph::addNode);

            Index<U> index = graph.getAdjacencyMatrixMap();
            graph.getNodesWithAdjacentEdges().forEach(u ->
            {
                IntPredicate pred = filter.apply(u);
                graph.getAdjacentNodes(u).filter(v -> pred.test(index.object2idx(v))).forEach(v ->
                {
                    double weight = graph.getEdgeWeight(u,v);
                    int type = graph.getEdgeType(u, v);
                    auxGraph.addEdge(u,v,weight,type);
                });
            });

            return auxGraph;
        }
        catch(GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }
    }
}
