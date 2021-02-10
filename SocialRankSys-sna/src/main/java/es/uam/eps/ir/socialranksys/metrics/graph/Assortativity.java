/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.graph;


import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.UndirectedGraph;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;

/**
 * Class for computing the assortativity of scalar values for a graph.
 *
 * <p>
 * <b>Reference: </b> M.E.J. Newman. Assortative mixing in networks. Physical Review Letters 89(20), 208701 (2002)
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Assortativity<U> implements GraphMetric<U>
{
    /**
     * Function for computing the values for each node.
     */
    private final ToDoubleBiFunction<U, Graph<U>> function;

    /**
     * Constructor.
     *
     * @param function function to apply to the users.
     */
    public Assortativity(ToDoubleBiFunction<U, Graph<U>> function)
    {
        this.function = function;
    }

    @Override
    public double compute(Graph<U> graph)
    {
        Map<U, Double> res = new HashMap<>();


        graph.getAllNodes().forEach(u -> res.put(u, function.applyAsDouble(u, graph)));

        if (graph.isDirected())
        {
            return this.computeDirected((DirectedGraph<U>) graph, res);
        }
        else
        {
            return this.computeUndirected((UndirectedGraph<U>) graph, res);
        }
    }

    /**
     * Computes the assortativity for directed graphs.
     *
     * @param graph the graph
     * @param res   the values for each node
     *
     * @return the assortativity of the directed graph.
     */
    private double computeDirected(DirectedGraph<U> graph, Map<U, Double> res)
    {
        List<U> nodes = graph.getAllNodes().collect(Collectors.toCollection(ArrayList::new));

        double outSum = 0.0;
        double inSum = 0.0;
        double outSumSquare = 0.0;
        double inSumSquare = 0.0;

        for (U u : nodes)
        {
            double outDegree = graph.outDegree(u);
            double inDegree = graph.inDegree(u);
            outSum += res.get(u) * outDegree;
            outSumSquare += res.get(u) * res.get(u) * outDegree;
            inSum += res.get(u) * inDegree;
            inSumSquare += res.get(u) * res.get(u) * inDegree;
        }

        double sum = graph.getAllNodes().mapToDouble(u ->
        {
            double out = res.get(u);
            return graph.getAdjacentNodes(u).mapToDouble(v -> out * res.get(v)).sum();
        }).sum();

        double numEdges = graph.getEdgeCount() + 0.0;
        double numerator = sum - outSum * inSum / numEdges;
        double denominator = Math.sqrt(outSumSquare - outSum * outSum / numEdges) * Math.sqrt(inSumSquare - inSum * inSum / numEdges);
        return numerator / denominator;
    }

    /**
     * Computes the assortativity for undirected graphs.
     *
     * @param graph the graph
     * @param res   the values for each node
     *
     * @return the assortativity of the undirected graph.
     */
    private double computeUndirected(UndirectedGraph<U> graph, Map<U, Double> res)
    {
        List<U> nodes = graph.getAllNodes().collect(Collectors.toCollection(ArrayList::new));

        double sum = 0.0;
        double sumSquare = 0.0;
        for (U u : nodes)
        {
            double degree = graph.degree(u);
            sum += degree * res.get(u);
            sumSquare += degree * res.get(u) * res.get(u);
        }

        double numSum = graph.getAllNodes().mapToDouble(u -> graph.getAdjacentNodes(u).mapToDouble(v -> res.get(u) * res.get(v)).sum()).sum();

        double numEdges = 2.0 * graph.getEdgeCount() + 0.0;
        double numerator = numSum - sum * sum / numEdges;
        double denominator = sumSquare - sum * sum / numEdges;
        return numerator / denominator;
    }

}
