/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.pair;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Computes the embeddedness the edges of a graph.
 *
 * <p>
 * <b>Reference: </b> D. Easley, J.M. Kleinberg. Networks, crowds and markets (2010)
 * </p>
 *
 * @param <U> Type of the users in the graph
 *
 * @author Javier Sanz-Cruzado Puig
 */
public class Embededness<U> extends AbstractPairMetric<U>
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
    public Embededness(EdgeOrientation uSel, EdgeOrientation vSel)
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

        if (firstNeighbours.isEmpty() && secondNeighbours.isEmpty() && graph.containsEdge(orig, dest))
        {
            return 1.0;
        }
        else
        {
            return (intersection.size() + 0.0) / (firstNeighbours.size() + secondNeighbours.size() - intersection.size() + 0.0);
        }
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

        double uSize;

        if (!graph.isMultigraph())
        {
            uSize = graph.getNeighbourhood(u, uSel).mapToDouble(w ->
            {
                graph.getNeighbourhood(w, vSel.invertSelection()).filter(v -> !v.equals(w)).forEach(v -> map.addTo(v, 1.0));
                return 1.0;
            }).sum();
        }
        else
        {
            uSize = 0.0;
        }

        return v ->
        {
            double inter = map.getOrDefault(v, map.defaultReturnValue());
            if (inter > 0.0)
            {
                return inter / (uSize + graph.getNeighbourhoodSize(v, vSel) - inter);
            }
            return 0.0;
        };
    }
}
