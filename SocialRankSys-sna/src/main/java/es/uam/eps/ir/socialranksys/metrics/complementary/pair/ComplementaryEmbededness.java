/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.complementary.pair;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.metrics.pair.AbstractPairMetric;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes the embeddedness the edges in the complementary of a graph.
 *
 * @param <V> Type of the users in the graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ComplementaryEmbededness<V> extends AbstractPairMetric<V>
{
    /**
     * Selection of the neighbours of the first node.
     */
    private final EdgeOrientation uSel;
    /**
     * Selection of the neighbour of the second node.
     */
    private final EdgeOrientation vSel;

    /**
     * Constructor.
     *
     * @param uSel Selection of the neighbours of the first node.
     * @param vSel Selection of the neighbours of the second node.
     */
    public ComplementaryEmbededness(EdgeOrientation uSel, EdgeOrientation vSel)
    {
        this.uSel = uSel;
        this.vSel = vSel;
    }

    @Override
    public double compute(Graph<V> graph, V orig, V dest)
    {
        if (graph.isMultigraph())
        {
            return Double.NaN;
        }


        Set<V> firstNeighbours = graph.getNeighbourhood(orig, uSel).collect(Collectors.toCollection(HashSet::new));
        Set<V> secondNeighbours = graph.getNeighbourhood(dest, vSel).collect(Collectors.toCollection(HashSet::new));
        firstNeighbours.remove(dest);
        secondNeighbours.remove(orig);

        Set<V> intersection = new HashSet<>(firstNeighbours);
        intersection.retainAll(secondNeighbours);

        if (firstNeighbours.isEmpty() && secondNeighbours.isEmpty())
        {
            return 0.0;
        }
        else
        {
            return (graph.getVertexCount() - firstNeighbours.size() - secondNeighbours.size() + intersection.size() + 0.0) / (graph.getVertexCount() - intersection.size() + 0.0);
        }
    }

    @Override
    public Map<Pair<V>, Double> compute(Graph<V> graph)
    {
        Map<Pair<V>, Double> values = new HashMap<>();
        if (!graph.isMultigraph())
        {
            graph.getAllNodes().forEach((orig) -> graph.getAllNodes().forEach(dest -> values.put(new Pair<>(orig, dest), this.compute(graph, orig, dest))));
        }
        return values;
    }

    @Override
    public Map<Pair<V>, Double> compute(Graph<V> graph, Stream<Pair<V>> pairs)
    {
        Map<Pair<V>, Double> values = new ConcurrentHashMap<>();
        if (!graph.isMultigraph())
        {
            pairs.forEach(pair -> values.put(pair, this.compute(graph, pair.v1(), pair.v2())));
        }
        return values;
    }

    @Override
    public double averageValue(Graph<V> graph)
    {
        double value = this.compute(graph).values().stream().reduce(0.0, Double::sum);
        return value / (graph.getEdgeCount() + 0.0);
    }

    @Override
    public double averageValue(Graph<V> graph, Stream<Pair<V>> edges, int edgeCount)
    {
        double value = edges.mapToDouble(edge -> this.compute(graph, edge.v1(), edge.v2())).sum();
        return value / (edgeCount + 0.0);
    }


}
