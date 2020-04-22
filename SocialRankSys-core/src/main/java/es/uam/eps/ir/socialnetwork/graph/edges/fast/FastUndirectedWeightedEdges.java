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

import es.uam.eps.ir.socialnetwork.graph.edges.EdgeType;
import es.uam.eps.ir.socialnetwork.graph.edges.EdgeWeight;
import es.uam.eps.ir.socialnetwork.graph.edges.UndirectedEdges;
import es.uam.eps.ir.socialnetwork.graph.edges.WeightedEdges;
import es.uam.eps.ir.socialnetwork.graph.index.IdxValue;
import es.uam.eps.ir.socialnetwork.graph.index.fast.FastWeightedAutoRelation;
import es.uam.eps.ir.ranksys.fast.preference.IdxPref;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Fast implementation of undirected weighted edges.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastUndirectedWeightedEdges extends FastEdges implements UndirectedEdges, WeightedEdges
{

    /**
     * Constructor.
     */
    public FastUndirectedWeightedEdges()
    {
        super(new FastWeightedAutoRelation<>(), new FastWeightedAutoRelation<>());
    }

    @Override
    public Stream<Integer> getNeighbourNodes(int node)
    {
        return this.weights.getIdsFirst(node).map(IdxValue::getIdx);
    }

    @Override
    public Stream<EdgeType> getNeighbourTypes(int node)
    {
        return this.types.getIdsFirst(node).map(type -> new EdgeType(type.getIdx(), type.getValue()));
    }

    @Override
    public Stream<IdxPref> getIncidentWeights(int node)
    {
        return this.getNeighbourWeights(node);
    }

    @Override
    public Stream<IdxPref> getAdjacentWeights(int node)
    {
        return this.getNeighbourWeights(node);
    }

    @Override
    public Stream<IdxPref> getNeighbourWeights(int node)
    {
        return this.weights.getIdsFirst(node).map(weight -> new EdgeWeight(weight.getIdx(), weight.getValue()));
    }

    @Override
    public boolean addEdge(int orig, int dest, double weight, int type)
    {
        if (orig != dest)
        {
            if (this.weights.addRelation(orig, dest, weight) &&
                    this.weights.addRelation(dest, orig, weight) &&
                    this.types.addRelation(orig, dest, type) &&
                    this.types.addRelation(dest, orig, type))
            {
                this.numEdges++;
                return true;
            }
        }
        else
        {
            if (this.weights.addRelation(orig, dest, weight) && this.types.addRelation(orig, dest, type))
            {
                this.numEdges++;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean updateEdgeWeight(int orig, int dest, double weight)
    {
        if (orig != dest)
        {
            return this.weights.updatePair(orig, dest, weight, false) && this.weights.updatePair(dest, orig, weight, false);
        }
        else
        {
            return this.weights.updatePair(orig, dest, weight, false);
        }
    }

    @Override
    public boolean removeNode(int idx)
    {
        long toDel = this.getAdjacentCount(idx);
        if (this.weights.remove(idx) && this.types.remove(idx))
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
        return this.weights.firstsWithSeconds();
    }

    @Override
    public IntStream getNodesWithMutualEdges()
    {
        return this.weights.firstsWithSeconds();
    }
}
