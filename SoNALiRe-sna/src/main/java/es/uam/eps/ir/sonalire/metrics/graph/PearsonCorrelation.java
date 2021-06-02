/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.graph;

import es.uam.eps.ir.sonalire.graph.DirectedGraph;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.UndirectedGraph;
import es.uam.eps.ir.sonalire.metrics.GraphMetric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;

/**
 * Class for computing the Pearson correlation of scalar values for a graph.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class PearsonCorrelation<U> implements GraphMetric<U>
{
    /**
     * Function for computing the values for each origin node in the link.
     */
    private final ToDoubleBiFunction<U, Graph<U>> originFunction;
    /**
     * Function for computing the values for each destiny node in the link.
     */
    private final ToDoubleBiFunction<U, Graph<U>> destinyFunction;

    /**
     * Constructor
     *
     * @param originFunction  Function for computing the values for each origin node in the link.
     * @param destinyFunction Function for computing the values for each destiny node in the link.
     */
    public PearsonCorrelation(ToDoubleBiFunction<U, Graph<U>> originFunction, ToDoubleBiFunction<U, Graph<U>> destinyFunction)
    {
        this.originFunction = originFunction;
        this.destinyFunction = destinyFunction;
    }

    @Override
    public double compute(Graph<U> graph)
    {
        Map<U, Double> originRes = new HashMap<>();
        Map<U, Double> destinyRes = new HashMap<>();


        graph.getAllNodes().forEach(u ->
        {
            originRes.put(u, originFunction.applyAsDouble(u, graph));
            destinyRes.put(u, destinyFunction.applyAsDouble(u, graph));
        });

        if (graph.isDirected())
        {
            return this.computeDirected((DirectedGraph<U>) graph, originRes, destinyRes);
        }
        else
        {
            return this.computeUndirected((UndirectedGraph<U>) graph, originRes, destinyRes);
        }
    }

    /**
     * Computes the assortativity for directed graphs.
     *
     * @param graph      the graph.
     * @param originRes  the values for the origin node.
     * @param destinyRes the values for the destination node.
     *
     * @return the Pearson correlation of the directed graph.
     */
    private double computeDirected(DirectedGraph<U> graph, Map<U, Double> originRes, Map<U, Double> destinyRes)
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
            outSum += originRes.get(u) * outDegree;
            outSumSquare += originRes.get(u) * originRes.get(u) * outDegree;
            inSum += destinyRes.get(u) * inDegree;
            inSumSquare += destinyRes.get(u) * destinyRes.get(u) * inDegree;
        }

        double sum = graph.getAllNodes().mapToDouble(u ->
        {
            double out = originRes.get(u);
            return graph.getAdjacentNodes(u).mapToDouble(v -> out * destinyRes.get(v)).sum();
        }).sum();

        double numEdges = graph.getEdgeCount() + 0.0;
        double numerator = sum - outSum * inSum / numEdges;
        double denominator = Math.sqrt(outSumSquare - outSum * outSum / numEdges) * Math.sqrt(inSumSquare - inSum * inSum / numEdges);
        return numerator / denominator;
    }

    /**
     * Computes the assortativity for directed graphs.
     *
     * @param graph      the graph.
     * @param originRes  the values for the origin node.
     * @param destinyRes the values for the destination node.
     *
     * @return the Pearson correlation of the undirected graph.
     */
    private double computeUndirected(UndirectedGraph<U> graph, Map<U, Double> originRes, Map<U, Double> destinyRes)
    {
        List<U> nodes = graph.getAllNodes().collect(Collectors.toCollection(ArrayList::new));

        double originSum = 0.0;
        double originSumSquare = 0.0;
        double destinySum = 0.0;
        double destinySumSquare = 0.0;
        for (U u : nodes)
        {
            double degree = graph.degree(u);
            originSum += degree * originRes.get(u);
            originSumSquare += degree * originRes.get(u) * originRes.get(u);
            destinySum += degree * destinyRes.get(u);
            destinySumSquare += degree * destinyRes.get(u) * destinyRes.get(u);
        }

        double numSum = graph.getAllNodes().mapToDouble(u -> graph.getAdjacentNodes(u).mapToDouble(v -> originRes.get(u) * destinyRes.get(v)).sum()).sum();

        double numEdges = 2.0 * graph.getEdgeCount() + 0.0;
        double numerator = numSum - originSum * destinySum / numEdges;
        double denominator = Math.sqrt(originSumSquare - originSum * originSum / numEdges) * Math.sqrt(destinySumSquare - destinySum * destinySum / numEdges);
        return numerator / denominator;
    }

}
