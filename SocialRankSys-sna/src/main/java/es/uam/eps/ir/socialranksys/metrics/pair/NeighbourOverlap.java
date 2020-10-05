/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.pair;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes the intersection between the neighborhoods of two nodes.
 *
 * @param <U> type of the nodes.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class NeighbourOverlap<U> implements PairMetric<U>
{

    /**
     * Neighbour selection for the origin node.
     */
    private final EdgeOrientation uSel;
    /**
     * Neighbour selection for the destiny node.
     */
    private final EdgeOrientation vSel;

    /**
     * Default constructor. Uses the outgoing neighbourhood of the origin node,
     * and the incoming of the destiny one.
     */
    public NeighbourOverlap()
    {
        this(EdgeOrientation.OUT, EdgeOrientation.IN);
    }

    /**
     * Constructor.
     *
     * @param uSel Neighbour selection for the origin node.
     * @param vSel Neighbour selection for the destiny node.
     */
    public NeighbourOverlap(EdgeOrientation uSel, EdgeOrientation vSel)
    {
        this.uSel = uSel;
        this.vSel = vSel;
    }

    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {
        if (graph.isMultigraph())
        {
            return Double.NaN;
        }


        Set<U> firstNeighbours = graph.getNeighbourhood(orig, uSel).collect(Collectors.toCollection(HashSet::new));
        Set<U> secondNeighbours = graph.getNeighbourhood(dest, vSel).collect(Collectors.toCollection(HashSet::new));
        firstNeighbours.remove(dest);
        secondNeighbours.remove(orig);

        Set<U> intersection = new HashSet<>(firstNeighbours);
        intersection.retainAll(secondNeighbours);

        return intersection.size() + 0.0;
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        Map<Pair<U>, Double> values = new HashMap<>();
        if (!graph.isMultigraph())
        {
            graph.getAllNodes().forEach((orig) ->
                graph.getAllNodes().forEach(dest ->
                {
                    if (!orig.equals(dest))
                    {
                        values.put(new Pair<>(orig, dest), this.compute(graph, orig, dest));
                    }
                })
            );
        }
        return values;
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        double value = this.compute(graph).values().stream().reduce(0.0, Double::sum);
        return value / (graph.getVertexCount() * (graph.getVertexCount() - 1));
    }

    @Override
    public double averageValue(Graph<U> graph, Stream<Pair<U>> edges, int edgeCount)
    {
        double value = edges.mapToDouble(edge -> this.compute(graph, edge.v1(), edge.v2())).sum();
        return value / (edgeCount + 0.0);
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)
    {
        Map<U, Map<U, Double>> map = new HashMap<>();
        Map<Pair<U>, Double> values = new ConcurrentHashMap<>();

        if (graph.isMultigraph())
        {
            return values;
        }

        long numUsers = graph.getVertexCount();
        graph.getAllNodes().forEach(u ->
        {
            Object2DoubleOpenHashMap<U> aux = new Object2DoubleOpenHashMap<>();
            aux.defaultReturnValue(0.0);
            graph.getNeighbourhood(u, uSel).forEach(w ->
                graph.getNeighbourhood(w, vSel.invertSelection()).forEach(v -> aux.addTo(v, 1.0))
            );

            map.put(u, aux);
        });

        pairs.forEach(p -> values.put(p, map.get(p.v1()).getOrDefault(p.v2(), 0.0)));
        return values;
    }

    @Override
    public Function<U, Double> computeOrig(Graph<U> graph, U orig)
    {
        return this.computeIndividual(graph, orig, uSel, vSel);
    }

    @Override
    public Function<U, Double> computeDest(Graph<U> graph, U dest)
    {
        return this.computeIndividual(graph, dest, vSel, uSel);
    }

    /**
     * Computes the map of metrics for the user.
     *
     * @param graph the graph.
     * @param u     the user.
     * @param uSel  the neighborhood selection for the user.
     * @param vSel  the neighborhood selection for the other users
     *
     * @return the map of metrics for the user.
     */
    private Function<U, Double> computeIndividual(Graph<U> graph, U u, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        Object2DoubleOpenHashMap<U> map = new Object2DoubleOpenHashMap<>();
        map.defaultReturnValue(0.0);

        if (!graph.isMultigraph())
        {
            graph.getNeighbourhood(u, uSel).forEach(w ->
                graph.getNeighbourhood(w, vSel.invertSelection()).forEach(v -> map.addTo(v, 1.0))
            );
        }

        return v -> map.getOrDefault(v, map.defaultReturnValue());
    }
}
