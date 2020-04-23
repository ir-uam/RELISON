/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.pair;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.Weight;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
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
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the nodes.
 */
public class WeightedNeighborOverlap<U> extends AbstractPairMetric<U>
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
    public WeightedNeighborOverlap()
    {
        this(EdgeOrientation.OUT, EdgeOrientation.IN);
    }
    
    /**
     * Constructor.
     * @param uSel Neighbour selection for the origin node.
     * @param vSel Neighbour selection for the destiny node.
     */
    public WeightedNeighborOverlap(EdgeOrientation uSel, EdgeOrientation vSel)
    {
        this.uSel = uSel;
        this.vSel = vSel;
    }
    
    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {
        if(graph.isMultigraph())
            return Double.NaN;

        Set<U> firstNeighbours = graph.getNeighbourhood(orig, uSel).collect(Collectors.toCollection(HashSet::new));
        Set<Weight<U, Double>> secondNeighbours = graph.getNeighbourhoodWeights(dest, vSel).collect(Collectors.toCollection(HashSet::new));
        firstNeighbours.remove(dest);

        return secondNeighbours.stream().filter(x -> firstNeighbours.contains(x.getIdx())).mapToDouble(Weight::getValue).sum();

    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)
    {
        Map<U, Map<U,Double>> map = new HashMap<>();
        Map<Pair<U>, Double> values = new ConcurrentHashMap<>();

        if(graph.isMultigraph()) return values;

        graph.getAllNodes().forEach(u ->
        {
            Object2DoubleOpenHashMap<U> aux = new Object2DoubleOpenHashMap<>();
            aux.defaultReturnValue(0.0);
            graph.getNeighbourhood(u, uSel).forEach(w ->
                graph.getNeighbourhoodWeights(w, vSel.invertSelection()).forEach(v -> aux.addTo(v.getIdx(), v.getValue()))
            );

            map.put(u, aux);
        });

        pairs.forEach(p -> values.put(p, map.get(p.v1()).getOrDefault(p.v2(), 0.0)));
        return values;
    }

    @Override
    public Function<U,Double> computeOrig(Graph<U> graph, U orig)
    {
        return this.computeIndividual(graph, orig, uSel, vSel);
    }

    @Override
    public Function<U,Double> computeDest(Graph<U> graph, U dest)
    {
        return this.computeIndividual(graph, dest, vSel, uSel);
    }

    /**
     * Computes the map of metrics for the user.
     * @param graph the graph.
     * @param u the user.
     * @param uSel the neighborhood selection for the user.
     * @param vSel the neighborhood selection for the other users
     * @return the map of metrics for the user.
     */
    private Function<U,Double> computeIndividual(Graph<U> graph, U u, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        Object2DoubleOpenHashMap<U> map = new Object2DoubleOpenHashMap<>();
        map.defaultReturnValue(0.0);

        if(!graph.isMultigraph())
        {
            graph.getNeighbourhood(u, uSel).forEach(w ->
                graph.getNeighbourhoodWeights(w, vSel.invertSelection()).forEach(v -> map.addTo(v.getIdx(), v.getValue()))
            );
        }

        return v -> map.getOrDefault(v, map.defaultReturnValue());

    }
}
