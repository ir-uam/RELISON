/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.complementary.edge;


import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.metrics.EdgeMetric;
import es.uam.eps.ir.socialranksys.metrics.exception.InexistentEdgeException;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes the embeddedness the edges of a graph.
 *
 * @param <V> Type of the users in the graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ComplementaryEmbededness<V> implements EdgeMetric<V>
{
    /**
     * Selection of neighbours of the origin node.
     */
    private final EdgeOrientation uSel;
    /**
     * Selection of neighbours of the destiny node.
     */
    private final EdgeOrientation vSel;

    /**
     * Constructor.
     *
     * @param uSel Selection of neighbours of the origin node.
     * @param vSel Selection of neighbours of the destiny node.
     */
    public ComplementaryEmbededness(EdgeOrientation uSel, EdgeOrientation vSel)
    {
        this.uSel = uSel;
        this.vSel = vSel;
    }

    /**
     * Default constructor. It selects the outgoing neighbourhood of the origin node,
     * and the incoming neighbours of the destiny node of the edge.
     */
    public ComplementaryEmbededness()
    {
        this(EdgeOrientation.OUT, EdgeOrientation.IN);
    }

    @Override
    public double compute(Graph<V> graph, V orig, V dest) throws InexistentEdgeException
    {
        if (graph.isMultigraph())
        {
            return Double.NaN;
        }

        if (graph.containsEdge(orig, dest))
        {
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

        throw new InexistentEdgeException("Edge between nodes " + orig + " and " + dest + " does not exist");
    }

    @Override
    public Map<Pair<V>, Double> compute(Graph<V> graph)
    {
        Map<Pair<V>, Double> values = new HashMap<>();
        if (!graph.isMultigraph())
        {
            graph.getAllNodes().forEach((orig) -> graph.getAdjacentNodes(orig).forEach(dest ->
            {
                try
                {
                    values.put(new Pair<>(orig, dest), this.compute(graph, orig, dest));
                }
                catch (InexistentEdgeException ex)
                {
                    ex.printStackTrace();
                }
            }));
        }
        return values;
    }

    @Override
    public Map<Pair<V>, Double> compute(Graph<V> graph, Stream<Pair<V>> edges)
    {
        Map<Pair<V>, Double> values = new ConcurrentHashMap<>();
        edges.forEach(edge ->
        {
            if (graph.containsEdge(edge.v1(), edge.v2()))
            {
                try
                {
                    values.put(edge, this.compute(graph, edge.v1(), edge.v2()));
                }
                catch (InexistentEdgeException e)
                {
                    values.put(edge, Double.NaN);
                }
            }
            else
            {
                values.put(edge, Double.NaN);
            }
        });
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
        double value = edges.mapToDouble(edge ->
        {
            try
            {
                return this.compute(graph, edge.v1(), edge.v2());
            }
            catch (InexistentEdgeException ex)
            {
                ex.printStackTrace();
                return 0.0;
            }
         }).sum();
        return value / (edgeCount + 0.0);
    }


}
