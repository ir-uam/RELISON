/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.distance.pair;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.metrics.PairMetric;
import es.uam.eps.ir.sonalire.metrics.distance.CompleteDistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.sonalire.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Computes the edge betweenness of the graph.
 *
 * <p>
 * <b>Reference:</b> M.E.J. Newman, M. Girvan. Finding and evaluating community structure in networks. Physical Review E 69(2), pp. 1-16 (2004)
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class EdgeBetweenness<U> implements PairMetric<U>
{

    /**
     * Distance calculator.
     */
    private final DistanceCalculator<U> dc;

    /**
     * Indicates whether the betweenness has to be normalized or not.
     */
    private final boolean normalize;

    /**
     * Constructor.
     *
     * @param dc distance calculator.
     */
    public EdgeBetweenness(DistanceCalculator<U> dc, boolean normalize)
    {
        this.dc = dc;
        this.normalize = normalize;
    }

    /**
     * Constructor.
     */
    public EdgeBetweenness(boolean normalize)
    {
        this.dc = new CompleteDistanceCalculator<>();
        this.normalize = normalize;
    }


    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {
        this.dc.computeDistances(graph);
        if (graph.containsEdge(orig, dest))
        {
            double betw = this.dc.getEdgeBetweenness(orig, dest);
            if(normalize) betw *= (graph.isDirected() ? 1.0 : 2.0)/(graph.getVertexCount()*(graph.getVertexCount()-1.0));
            return betw;
        }

        return 0.0;
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> edges)
    {
        Map<Pair<U>, Double> values = new ConcurrentHashMap<>();
        this.dc.computeDistances(graph);

        edges.forEach(edge ->
        {
            if (graph.containsEdge(edge.v1(), edge.v2()))
            {
                double betw = this.dc.getEdgeBetweenness(edge.v1(), edge.v2());
                if(normalize) betw *= (graph.isDirected() ? 1.0 : 2.0)/(graph.getVertexCount()*(graph.getVertexCount()-1.0));
                values.put(edge, betw);
            }
            else
            {
                values.put(edge, Double.NaN);
            }
        });
        return values;
    }

    @Override
    public Function<U, Double> computeOrig(Graph<U> graph, U orig)
    {
        Object2DoubleMap<U> map = new Object2DoubleOpenHashMap<>();
        graph.getAllNodes().forEach(dest -> map.put(dest, this.compute(graph, orig, dest)));

        return (map::getDouble);
    }

    @Override
    public Function<U, Double> computeDest(Graph<U> graph, U dest)
    {
        Object2DoubleMap<U> map = new Object2DoubleOpenHashMap<>();
        graph.getAllNodes().forEach(orig -> map.put(dest, this.compute(graph, orig, dest)));

        return (map::getDouble);
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        Map<Pair<U>, Double> values = new HashMap<>();
        this.dc.computeDistances(graph);
        Map<U, Map<U, Double>> betwenness = this.dc.getEdgeBetweenness();

        graph.getAllNodes().forEach(u -> graph.getAllNodes().forEach(v ->
        {
            if(graph.containsEdge(u, v))
            {
                double betw = betwenness.get(u).get(v);
                if (normalize)
                    betw *= (graph.isDirected() ? 1.0 : 2.0) / (graph.getVertexCount() * (graph.getVertexCount() - 1.0));
                values.put(new Pair<>(u, v), betw);
            }
            else
            {
                values.put(new Pair<>(u,v), 0.0);
            }
        }));

        return values;
    }

    @Override
    public Map<Pair<U>, Double> computeOnlyLinks(Graph<U> graph)
    {
        Map<Pair<U>, Double> values = new HashMap<>();
        this.dc.computeDistances(graph);
        Map<U, Map<U, Double>> betwenness = this.dc.getEdgeBetweenness();

        graph.getAllNodes().forEach(u -> graph.getAdjacentNodes(u).forEach(v ->
        {
             double betw = betwenness.get(u).get(v);
             if(normalize) betw *= (graph.isDirected() ? 1.0 : 2.0)/(graph.getVertexCount()*(graph.getVertexCount()-1.0));
             values.put(new Pair<>(u, v), betw);
        }));

        return values;
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        OptionalDouble optional = this.compute(graph).values().stream().mapToDouble(val -> val).average();
        return optional.isPresent() ? optional.getAsDouble() : 0.0;
    }

    @Override
    public double averageValueOnlyLinks(Graph<U> graph)
    {
        OptionalDouble optional = this.computeOnlyLinks(graph).values().stream().mapToDouble(val -> val).average();
        return optional.isPresent() ? optional.getAsDouble() : 0.0;
    }

    @Override
    public double averageValue(Graph<U> graph, Stream<Pair<U>> edges, int edgeCount)
    {
        if (graph.getEdgeCount() > 0L)
        {
            this.dc.computeDistances(graph);
            double value = edges.mapToDouble(edge ->
            {
                if (graph.containsEdge(edge.v1(), edge.v2()))
                {
                    double betw = this.dc.getEdgeBetweenness(edge.v1(), edge.v2());
                    if(normalize) betw *= (graph.isDirected() ? 1.0 : 2.0)/(graph.getVertexCount()*(graph.getVertexCount()-1.0));
                    return betw;
                }
                return 0.0;
            }).sum();
            return value / edgeCount;
        }
        return 0.0;
    }


}
