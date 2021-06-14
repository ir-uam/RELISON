/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.pair;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.sna.metrics.PairMetric;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
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
 * Expanded neighbor overlap. Finds the size of the intersection between
 * users at distance at most 2 of one user, with the neighborhood of another.
 * Users' weight depend on the number of times they appear.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ExpandedNeighborCountedOverlap<U> implements PairMetric<U>
{
    /**
     * True if we have to obtain the d2 neighbors of the origin user,
     * false if we have to do it for the destination.
     */
    private final boolean origin;
    /**
     * Orientation selection for the origin user.
     */
    private final EdgeOrientation uSel;
    /**
     * Orientation selection for the destination user.
     */
    private final EdgeOrientation vSel;

    /**
     * Constructor.
     *
     * @param origin true if we have to obtain the distance 2 neighbors of the origin user,
     *               false otherwise.
     * @param uSel   orientation selection for the origin user.
     * @param vSel   orientation selection for the destination user.
     */
    public ExpandedNeighborCountedOverlap(boolean origin, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        this.origin = origin;
        this.uSel = uSel;
        this.vSel = vSel;
    }

    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {
        Map<U, Double> distanceTwoUsers = this.explore(graph, (origin ? orig : dest), (origin ? uSel : vSel));
        Set<U> neigh = graph.getNeighbourhood((origin ? dest : orig), (origin ? vSel : uSel)).collect(Collectors.toCollection(HashSet::new));

        Set<U> aux = new HashSet<>(distanceTwoUsers.keySet());
        aux.retainAll(neigh);
        return aux.stream().mapToDouble(distanceTwoUsers::get).sum();
    }

    /**
     * Obtains the neighborhood at distance 2.
     *
     * @param graph  the graph.
     * @param node   the node.
     * @param orient the orientation.
     *
     * @return the neighborhood at distance 2.
     */
    public Map<U, Double> explore(Graph<U> graph, U node, EdgeOrientation orient)
    {
        Map<U, Double> exploration = new HashMap<>();
        graph.getNeighbourhood(node, orient).forEach(w ->
        {
            exploration.put(w, exploration.getOrDefault(w, 0.0) + 1.0);
            graph.getNeighbourhood(w, orient).forEach(x ->
                exploration.put(x, exploration.getOrDefault(x, 0.0) + 1.0)
            );
        });
        return exploration;
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        Map<Pair<U>, Double> values = new HashMap<>();
        Map<U, Set<U>> neighs = new HashMap<>();

        graph.getAllNodes().forEach(u ->
        {
            Map<U, Double> d2neigh = this.explore(graph, u, origin ? uSel : vSel);
            graph.getAllNodes().forEach(v ->
            {
                Set<U> vNeigh;
                if (neighs.containsKey(v))
                {
                    vNeigh = neighs.get(v);
                }
                else
                {
                    vNeigh = graph.getNeighbourhood(v, origin ? vSel : uSel).collect(Collectors.toCollection(HashSet::new));
                    neighs.put(v, vNeigh);
                }

                Set<U> aux = new HashSet<>(d2neigh.keySet());
                aux.retainAll(vNeigh);
                double value = aux.stream().mapToDouble(d2neigh::get).sum();
                if (origin)
                {
                    values.put(new Pair<>(u, v), value);
                }
                else
                {
                    values.put(new Pair<>(v, u), value);
                }
            });
        });

        return values;
    }

    @Override
    public Map<Pair<U>, Double> computeOnlyLinks(Graph<U> graph)
    {
        return null;
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)
    {
        Map<Pair<U>, Double> values = new ConcurrentHashMap<>();
        Map<U, Map<U, Double>> origins = new ConcurrentHashMap<>();
        Map<U, Set<U>> dests = new ConcurrentHashMap<>();

        EdgeOrientation neighO = origin ? this.vSel : this.uSel;
        EdgeOrientation exploreO = origin ? this.uSel : this.vSel;
        pairs.forEach(pair ->
        {
            U explored = origin ? pair.v2() : pair.v1();
            U other = origin ? pair.v1() : pair.v2();

            Map<U, Double> auxD2;
            Set<U> aux;
            Set<U> neigh;

            if (origins.containsKey(explored))
            {
                auxD2 = origins.get(explored);
            }
            else
            {
                auxD2 = this.explore(graph, explored, exploreO);
                origins.put(explored, auxD2);
            }
            aux = new HashSet<>(auxD2.keySet());
            if (dests.containsKey(other))
            {
                neigh = dests.get(other);
            }
            else
            {
                neigh = graph.getNeighbourhood(other, neighO).collect(Collectors.toCollection(HashSet::new));
                dests.put(other, neigh);
            }

            aux.retainAll(neigh);
            double value = aux.stream().mapToDouble(auxD2::get).sum();
            values.put(pair, value);
        });

        return values;
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        double value = this.compute(graph).values().stream().reduce(0.0, Double::sum);
        return value / (graph.getEdgeCount() + 0.0);
    }

    @Override
    public double averageValueOnlyLinks(Graph<U> graph)
    {
        return 0;
    }

    @Override
    public double averageValue(Graph<U> graph, Stream<Pair<U>> edges, int edgeCount)
    {
        double value = edges.mapToDouble(edge -> this.compute(graph, edge.v1(), edge.v2())).sum();
        return value / (edgeCount + 0.0);
    }

    @Override
    public Function<U, Double> computeOrig(Graph<U> graph, U orig)
    {
        if (origin)
        {
            return this.computeIndividualOrig(graph, orig, uSel, vSel);
        }
        else
        {
            return this.computeIndividualDest(graph, orig, uSel, vSel);
        }
    }

    @Override
    public Function<U, Double> computeDest(Graph<U> graph, U dest)
    {
        if (origin)
        {
            return this.computeIndividualDest(graph, dest, vSel, uSel);
        }
        else
        {
            return this.computeIndividualOrig(graph, dest, vSel, uSel);
        }
    }

    private Function<U, Double> computeIndividualOrig(Graph<U> graph, U u, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        Object2DoubleOpenHashMap<U> map = new Object2DoubleOpenHashMap<>();
        map.defaultReturnValue(0.0);

        graph.getNeighbourhood(u, uSel).forEach(w ->
        {
            graph.getNeighbourhood(w, vSel.invertSelection()).forEach(v -> map.addTo(v, 1.0));
            graph.getNeighbourhood(w, uSel).forEach(x -> graph.getNeighbourhood(x, vSel.invertSelection()).forEach(v -> map.addTo(v, 1.0)));
        });

        return v -> map.getOrDefault(v, map.defaultReturnValue());
    }

    private Function<U, Double> computeIndividualDest(Graph<U> graph, U u, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        Object2DoubleOpenHashMap<U> map = new Object2DoubleOpenHashMap<>();
        map.defaultReturnValue(0.0);

        graph.getNeighbourhood(u, uSel).forEach(w ->
            graph.getNeighbourhood(w, vSel.invertSelection()).forEach(x ->
            {
                map.addTo(x, 1.0);
                graph.getNeighbourhood(x, vSel.invertSelection()).forEach(v -> map.addTo(v, 1.0));
            })
        );

        return v -> map.getOrDefault(v, map.defaultReturnValue());
    }
}
