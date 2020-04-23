/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.complementary.pair;

import es.uam.eps.ir.socialnetwork.metrics.pair.AbstractPairMetric;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes the intersection between the neighborhoods of two nodes.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the nodes.
 */
public class ComplementaryNeighbourOverlap<U> extends AbstractPairMetric<U>
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
    public ComplementaryNeighbourOverlap()
    {
        this(EdgeOrientation.OUT, EdgeOrientation.IN);
    }
    
    /**
     * Constructor.
     * @param uSel Neighbour selection for the origin node.
     * @param vSel Neighbour selection for the destiny node.
     */
    public ComplementaryNeighbourOverlap(EdgeOrientation uSel, EdgeOrientation vSel)
    {
        this.uSel = uSel;
        this.vSel = vSel;
    }
    
    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {
        if(graph.isMultigraph())
            return Double.NaN;
        
        
        Set<U> firstNeighbours = graph.getNeighbourhood(orig,uSel).collect(Collectors.toCollection(HashSet::new));
        Set<U> secondNeighbours = graph.getNeighbourhood(dest,vSel).collect(Collectors.toCollection(HashSet::new));
        firstNeighbours.remove(dest);
        secondNeighbours.remove(orig);

        Set<U> intersection = new HashSet<>(firstNeighbours);
        intersection.retainAll(secondNeighbours);

        if(firstNeighbours.isEmpty() && secondNeighbours.isEmpty())
        {
            return 0.0;
        }
        else
        {
            return graph.getVertexCount() - firstNeighbours.size() - secondNeighbours.size() + intersection.size() + 0.0;
        }
        
    }
    
    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        Map<Pair<U>, Double> values = new HashMap<>();
        if(!graph.isMultigraph())
        {
            graph.getAllNodes().forEach((orig) ->
               graph.getAllNodes().forEach(dest -> 
               {
                   if(!orig.equals(dest))
                      values.put(new Pair<>(orig, dest), this.compute(graph, orig, dest));
               })
            );
        }
        return values;
    }

    @Override
    public double averageValue(Graph<U> graph) {
        double value = this.compute(graph).values().stream().reduce(0.0, Double::sum);
        return value/(graph.getVertexCount()*(graph.getVertexCount()-1));
    }
    
    @Override 
    public double averageValue(Graph<U> graph, Stream<Pair<U>> edges, int edgeCount)
    {
        double value = edges.mapToDouble(edge -> this.compute(graph, edge.v1(), edge.v2())).sum();
        return value / (edgeCount + 0.0);
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs) {
        Map<Pair<U>, Double> values = new ConcurrentHashMap<>();
        if(!graph.isMultigraph())
        {
            pairs.forEach(pair -> values.put(pair, this.compute(graph, pair.v1(), pair.v2())));
        }
        return values;
    }
}
