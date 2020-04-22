/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph.edges.fast;

import es.uam.eps.ir.socialnetwork.graph.edges.DirectedEdges;
import es.uam.eps.ir.socialnetwork.graph.edges.EdgeType;
import es.uam.eps.ir.socialnetwork.graph.edges.EdgeWeight;
import es.uam.eps.ir.socialnetwork.graph.edges.WeightedEdges;
import es.uam.eps.ir.socialnetwork.graph.index.IdxValue;
import es.uam.eps.ir.socialnetwork.graph.index.fast.FastWeightedAutoRelation;
import es.uam.eps.ir.socialnetwork.utils.listcombiner.OrderedListCombiner;
import es.uam.eps.ir.socialnetwork.utils.datatypes.Tuple2oo;
import es.uam.eps.ir.ranksys.fast.preference.IdxPref;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Fast implementation of directed weighted edges.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastDirectedWeightedEdges extends FastEdges implements DirectedEdges, WeightedEdges
{
    /**
     * Constructor.
     */
    public FastDirectedWeightedEdges()
    {
        super(new FastWeightedAutoRelation<>(), new FastWeightedAutoRelation<>());
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
        return this.types.getIdsFirst(node).map(weight -> new EdgeType(weight.getIdx(), weight.getValue()));
    }

    @Override
    public Stream<EdgeType> getAdjacentTypes(int node)
    {
        return this.types.getIdsSecond(node).map(weight -> new EdgeType(weight.getIdx(), weight.getValue()));
    }

    @Override
    public Stream<IdxPref> getIncidentWeights(int node)
    {
        return this.weights.getIdsFirst(node).map(weight -> new EdgeWeight(weight.getIdx(), weight.getValue()));
    }

    @Override
    public Stream<IdxPref> getAdjacentWeights(int node)
    {
        return this.weights.getIdsSecond(node).map(weight -> new EdgeWeight(weight.getIdx(), weight.getValue()));
    }

    @Override
    public boolean addEdge(int orig, int dest, double weight, int type)
    {
        if (this.weights.addRelation(orig, dest, weight) && this.types.addRelation(orig, dest, type))
        {
            numEdges++;
            return true;
        }

        return false;
    }

    @Override
    public boolean updateEdgeWeight(int orig, int dest, double weight)
    {
        return this.weights.updatePair(orig, dest, weight, false);
    }

    @Override
    public Stream<IdxPref> getNeighbourWeights(int node)
    {
        List<IdxPref> neighbors = new ArrayList<>();
        Comparator<Tuple2oo<IdxPref, Iterator<IdxPref>>> comparator = (Tuple2oo<IdxPref, Iterator<IdxPref>> x, Tuple2oo<IdxPref, Iterator<IdxPref>> y) ->
                (int) (x.v1().v1() - y.v1().v1());

        PriorityQueue<Tuple2oo<IdxPref, Iterator<IdxPref>>> queue = new PriorityQueue<>(2, comparator);

        Iterator<IdxPref> iteratorIncident = this.getIncidentWeights(node).iterator();
        Iterator<IdxPref> iteratorAdjacent = this.getAdjacentWeights(node).iterator();

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
            Tuple2oo<IdxPref, Iterator<IdxPref>> tuple = queue.poll();

            if (tuple.v1().v1() != currentNeigh)
            {
                if (currentNeigh > 0)
                {
                    neighbors.add(new EdgeWeight(currentNeigh, currentValue));
                    currentValue = 0.0;
                    currentNeigh = tuple.v1().v1();
                }
                else
                {
                    currentNeigh = tuple.v1().v1();
                }
            }

            currentValue += tuple.v1().v2();

            if (tuple.v2().hasNext())
            {
                queue.add(new Tuple2oo<>(tuple.v2().next(), tuple.v2()));
            }
        }

        if (currentNeigh != -1)
        {
            neighbors.add(new EdgeWeight(currentNeigh, currentValue));
        }

        return neighbors.stream();
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

        boolean weightRem = this.weights.remove(idx);
        boolean typesRem = this.types.remove(idx);
        if (weightRem && typesRem)
        {
            this.numEdges -= toDel;
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
