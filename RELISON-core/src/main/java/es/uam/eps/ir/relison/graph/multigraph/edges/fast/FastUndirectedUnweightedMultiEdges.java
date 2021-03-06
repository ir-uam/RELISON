/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.multigraph.edges.fast;

import es.uam.eps.ir.relison.graph.multigraph.edges.MultiEdgeTypes;
import es.uam.eps.ir.relison.graph.multigraph.edges.MultiEdgeWeights;
import es.uam.eps.ir.relison.graph.multigraph.edges.UndirectedMultiEdges;
import es.uam.eps.ir.relison.graph.multigraph.edges.UnweightedMultiEdges;
import es.uam.eps.ir.relison.index.IdxValue;
import es.uam.eps.ir.relison.index.fast.FastWeightedAutoRelation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Fast implementation of undirected unweighted edges for multigraphs.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastUndirectedUnweightedMultiEdges extends FastMultiEdges implements UndirectedMultiEdges, UnweightedMultiEdges
{
    /**
     * Constructor.
     */
    public FastUndirectedUnweightedMultiEdges()
    {
        super(new FastWeightedAutoRelation<>(), new FastWeightedAutoRelation<>());
    }

    @Override
    public Stream<Integer> getNeighbourNodes(int node)
    {
        return this.weights.getIdsFirst(node).map(IdxValue::getIdx);
    }

    @Override
    public Stream<MultiEdgeTypes> getNeighbourTypes(int node)
    {
        return this.types.getIdsFirst(node).map(type -> new MultiEdgeTypes(type.getIdx(), type.getValue()));
    }

    @Override
    public Stream<MultiEdgeWeights> getNeighbourWeights(int node)
    {
        return this.weights.getIdsFirst(node).map(weight -> new MultiEdgeWeights(weight.getIdx(), MultiEdgeWeights.getDefaultValue(this.getNumEdges(node, weight.getIdx()))));
    }

    @Override
    public boolean addEdge(int orig, int dest, double weight, int type)
    {
        boolean failed;
        if (this.weights.containsPair(orig, dest))
        {
            List<Double> weightList = this.getEdgeWeights(orig, dest);
            weightList.add(MultiEdgeWeights.getDefaultValue());

            List<Integer> typeList = this.types.getValue(orig, dest);
            typeList.add(type);

            if(orig != dest)
                failed = this.weights.updatePair(orig, dest, weightList) && this.types.updatePair(orig, dest, typeList)
                    && this.weights.updatePair(dest, orig, weightList) && this.types.updatePair(dest, orig, typeList);
            else
                failed = this.weights.updatePair(orig, dest, weightList) && this.types.updatePair(orig, dest, typeList);
        }
        else
        {
            List<Double> weightList = new ArrayList<>();
            weightList.add(MultiEdgeWeights.getDefaultValue());

            List<Integer> typeList = new ArrayList<>();
            typeList.add(type);

            if(orig != dest)
                failed = this.weights.addRelation(orig, dest, weightList) && this.types.addRelation(orig, dest, typeList)
                    && this.weights.addRelation(dest, orig, weightList) && this.types.addRelation(dest, orig, typeList);
            else
                failed = this.weights.addRelation(orig, dest, weightList) && this.types.addRelation(orig, dest, typeList);
        }

        if (failed)
        {
            this.numEdges++;
        }
        return failed;
    }

    @Override
    public IntStream getNodesWithEdges()
    {
        return this.weights.firstsWithSeconds();
    }


    @Override
    public boolean removeEdge(int orig, int dest, int idx)
    {
        if(this.weights.containsPair(orig, dest) && this.types.containsPair(orig, dest))
        {
            if (orig == dest)
            {

                List<Double> weightList = this.weights.getValue(orig, dest);
                List<Integer> typeList = this.types.getValue(orig, dest);

                if (idx < 0 || idx >= weightList.size()) return false;
                weightList.remove(idx);
                typeList.remove(idx);
                this.numEdges--;

                if (weightList.isEmpty())
                    return this.weights.removePair(orig, dest) && this.types.removePair(orig, dest);
                else return true;
            }
            else
            {
                List<Double> weightListA = this.weights.getValue(orig, dest);
                List<Double> weightListB = this.weights.getValue(dest, orig);
                List<Integer> typeListA = this.types.getValue(orig, dest);
                List<Integer> typeListB = this.types.getValue(dest, orig);

                if (idx < 0 || idx >= weightListA.size()) return false;
                weightListA.remove(idx);
                weightListB.remove(idx);
                typeListA.remove(idx);
                typeListB.remove(idx);
                this.numEdges--;
                if (weightListA.isEmpty())
                {
                    return this.weights.removePair(orig, dest) && this.weights.removePair(dest, orig) && this.types.removePair(orig, dest) && this.types.removePair(dest, orig);
                }
                else return true;
            }
        }

        return false;
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
    public boolean removeEdges(int orig, int dest)
    {
        int numRemoved = this.getNumEdges(orig, dest);
        if (orig == dest)
        {
            if (this.weights.removePair(orig, dest) && this.types.removePair(dest, orig))
            {
                this.numEdges -= numRemoved;
                return true;
            }
            return false;
        }
        else if (this.weights.removePair(orig, dest) && this.weights.removePair(dest, orig) && this.types.removePair(orig, dest) && this.types.removePair(dest, orig))
        {
            this.numEdges -= numRemoved;
            return true;
        }
        return false;
    }

    @Override
    public boolean updateEdgeWeight(int orig, int dest, double weight, int idx)
    {
        return this.containsEdge(orig, dest) && idx >= 0 && idx < this.getNumEdges(orig, dest);
    }

    @Override
    public boolean updateEdgeType(int orig, int dest, int type, int idx)
    {
        if(this.containsEdge(orig, dest))
        {
            if(orig == dest)
            {
                List<Integer> weights = this.getEdgeTypes(orig, dest);
                if (idx < 0 || idx >= weights.size()) return false;
                weights.set(idx, type);
                return true;
            }
            else
            {
                List<Integer> weightsA = this.getEdgeTypes(orig, dest);
                List<Integer> weightsB = this.getEdgeTypes(dest, orig);
                if (idx < 0 || idx >= weightsA.size()) return false;
                weightsA.set(idx, type); weightsB.set(idx, type);
                return true;
            }
        }
        return false;
    }
}
