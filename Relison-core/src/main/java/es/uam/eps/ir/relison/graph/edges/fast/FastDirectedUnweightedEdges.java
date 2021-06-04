/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.edges.fast;

import es.uam.eps.ir.ranksys.fast.preference.IdxPref;
import es.uam.eps.ir.relison.graph.edges.DirectedEdges;
import es.uam.eps.ir.relison.graph.edges.EdgeType;
import es.uam.eps.ir.relison.graph.edges.EdgeWeight;
import es.uam.eps.ir.relison.graph.edges.UnweightedEdges;
import es.uam.eps.ir.relison.index.IdxValue;
import es.uam.eps.ir.relison.index.fast.FastUnweightedAutoRelation;
import es.uam.eps.ir.relison.index.fast.FastWeightedAutoRelation;
import es.uam.eps.ir.relison.utils.datatypes.Tuple2oo;
import es.uam.eps.ir.relison.utils.listcombiner.OrderedListCombiner;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Fast implementation of directed unweighted edges.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastDirectedUnweightedEdges extends FastEdges implements DirectedEdges, UnweightedEdges
{
    /**
     * Constructor.
     */
    public FastDirectedUnweightedEdges()
    {
        super(new FastUnweightedAutoRelation<>(), new FastWeightedAutoRelation<>());
    }

    @Override
    public Stream<Integer> getIncidentNodes(int node)
    {
        return this.weights.getIdsFirst(node).map(IdxValue::getIdx);
    }

    @Override
    public Stream<Integer> getAdjacentNodes(int node)
    {
        return this.weights.getIdsSecond(node).map(IdxValue::getIdx);
    }

    @Override
    public Stream<EdgeType> getIncidentTypes(int node)
    {
        return this.types.getIdsFirst(node).map(type -> new EdgeType(type.getIdx(), type.getValue()));
    }

    @Override
    public Stream<EdgeType> getAdjacentTypes(int node)
    {
        return this.types.getIdsSecond(node).map(type -> new EdgeType(type.getIdx(), type.getValue()));
    }

    @Override
    public Stream<IdxPref> getNeighbourWeights(int node)
    {
        List<IdxPref> neighbors = new ArrayList<>();
        Comparator<Tuple2oo<Integer, Iterator<Integer>>> comparator = Comparator.comparingInt(Tuple2oo::v1);

        PriorityQueue<Tuple2oo<Integer, Iterator<Integer>>> queue = new PriorityQueue<>(2, comparator);

        Iterator<Integer> iteratorIncident = this.getIncidentNodes(node).iterator();
        Iterator<Integer> iteratorAdjacent = this.getAdjacentNodes(node).iterator();

        if (iteratorIncident.hasNext())
        {
            queue.add(new Tuple2oo<>(iteratorIncident.next(), iteratorIncident));
        }
        if (iteratorAdjacent.hasNext())
        {
            queue.add(new Tuple2oo<>(iteratorAdjacent.next(), iteratorAdjacent));
        }

        double currentValue = 0.0;
        int currentNeigh = -1;
        while (!queue.isEmpty())
        {
            Tuple2oo<Integer, Iterator<Integer>> tuple = queue.poll();

            if (currentNeigh != tuple.v1())
            {
                if (currentNeigh > 0)
                {
                    neighbors.add(new IdxPref(currentNeigh, currentValue));
                    currentValue = 0.0;
                }
                currentNeigh = tuple.v1();
            }

            currentValue += EdgeWeight.getDefaultValue();

            if (tuple.v2().hasNext())
            {
                queue.add(new Tuple2oo<>(tuple.v2().next(), tuple.v2()));
            }
        }

        if (currentNeigh != -1)
        {
            neighbors.add(new IdxPref(currentNeigh, currentValue));
        }

        return neighbors.stream();
    }

    @Override
    public boolean addEdge(int orig, int dest, double weight, int type)
    {
        if (this.weights.addRelation(orig, dest, weight) && this.types.addRelation(orig, dest, type))
        {
            this.numEdges++;
            return true;
        }
        return false;
    }

    @Override
    public boolean updateEdgeWeight(int orig, int dest, double weight)
    {
        return this.containsEdge(orig, dest);
    }

    @Override
    public boolean removeNode(int idx)
    {
        int toDel = 0;
        if (this.weights.containsPair(idx, idx))
        {
            toDel--;
        }
        toDel += this.getAdjacentCount(idx) + this.getIncidentCount(idx);
        if (this.weights.remove(idx) && this.types.remove(idx))
        {
            this.numEdges -= toDel;
            return true;
        }
        return false;
    }

    @Override
    public boolean removeEdge(int orig, int dest)
    {
        if (this.weights.removePair(orig, dest) && this.types.removePair(orig, dest))
        {
            this.numEdges--;
            return true;
        }
        return false;
    }

    @Override
    public IntStream getNodesWithIncidentEdges()
    {
        return this.weights.secondsWithFirsts();
    }

    @Override
    public IntStream getNodesWithAdjacentEdges()
    {
        return this.weights.firstsWithSeconds();
    }

    @Override
    public IntStream getNodesWithEdges()
    {
        Iterator<Integer> iteratorIncident = this.getNodesWithIncidentEdges().iterator();
        Iterator<Integer> iteratorAdjacent = this.getNodesWithAdjacentEdges().iterator();

        List<Integer> users = OrderedListCombiner.mergeLists(iteratorAdjacent, iteratorIncident, Comparator.naturalOrder(), (x, y) -> x);
        return users.stream().mapToInt(x -> x);
    }

    @Override
    public IntStream getNodesWithMutualEdges()
    {
        List<Integer> users = new ArrayList<>();

        Iterator<Integer> iteratorIncident = this.getNodesWithIncidentEdges().iterator();
        while (iteratorIncident.hasNext())
        {
            int idx = iteratorIncident.next();
            Stream<Integer> incident = this.getIncidentNodes(idx);
            Stream<Integer> adjacent = this.getAdjacentNodes(idx);

            boolean value = OrderedListCombiner.intersectionHaslements(incident, adjacent, Comparator.naturalOrder());
            if (value)
            {
                users.add(idx);
            }
        }

        return users.stream().mapToInt(x -> x);
    }
}
