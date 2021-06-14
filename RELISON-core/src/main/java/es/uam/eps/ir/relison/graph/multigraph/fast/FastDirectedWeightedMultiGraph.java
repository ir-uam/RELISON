/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.multigraph.fast;


import es.uam.eps.ir.relison.graph.Weight;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.multigraph.DirectedWeightedMultiGraph;
import es.uam.eps.ir.relison.graph.multigraph.edges.fast.FastDirectedWeightedMultiEdges;
import es.uam.eps.ir.relison.index.fast.FastIndex;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fast implementation of a directed weighted graph. This implementation does not allow to remove nodes/edges
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastDirectedWeightedMultiGraph<U> extends AbstractFastMultiGraph<U> implements DirectedWeightedMultiGraph<U>
{
    /**
     * Constructor.
     */
    public FastDirectedWeightedMultiGraph()
    {
        super(new FastIndex<>(), new FastDirectedWeightedMultiEdges());
    }

    @Override
    public double[][] getAdjacencyMatrix(EdgeOrientation direction)
    {
        int numUsers = Long.valueOf(this.getVertexCount()).intValue();
        double[][] matrix = new double[numUsers][numUsers];

        this.getAllNodesIds().forEach(uidx -> this.getNeighborhood(uidx, direction).forEach(vidx ->
        {
            double weight = switch (direction)
                    {
                        case IN -> this.getEdgeWeight(vidx, uidx);
                        case OUT -> this.getEdgeWeight(uidx, vidx);
                        case UND, MUTUAL -> this.getEdgeWeight(uidx, vidx) + this.getEdgeWeight(vidx, uidx);
                    };
            matrix[uidx][vidx] = weight;
        }));

        return matrix;
    }

    // TODO: All below
    @Override
    public Stream<U> getMutualNodes(U node)
    {
        return this.getAdjacentNodes(node).filter(u -> this.containsEdge(node, u));
    }

    @Override
    public int getMutualEdgesCount(U node)
    {
        return this.getMutualNodes(node).mapToInt(u -> this.getNumEdges(node, u) + this.getNumEdges(u, node)).sum();
    }

    @Override
    public Stream<Weight<U, Double>> getAdjacentMutualNodesWeights(U node)
    {
        Set<U> mutuals = this.getMutualNodes(node).collect(Collectors.toSet());
        return this.getAdjacentNodesWeights(node).filter(w -> mutuals.contains(w.getIdx()));
    }

    @Override
    public Stream<Weight<U, Double>> getIncidentMutualNodesWeights(U node)
    {
        Set<U> mutuals = this.getMutualNodes(node).collect(Collectors.toSet());
        return this.getIncidentNodesWeights(node).filter(w -> mutuals.contains(w.getIdx()));
    }

    @Override
    public Stream<Weight<U, Double>> getMutualNodesWeights(U node)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<Weight<U, Integer>> getMutualNodesTypes(U node)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<Weight<U, Integer>> getAdjacentMutualNodesTypes(U node)
    {
        Set<U> mutuals = this.getMutualNodes(node).collect(Collectors.toSet());
        return this.getAdjacentNodesTypes(node).filter(w -> mutuals.contains(w.getIdx()));
    }

    @Override
    public Stream<Weight<U, Integer>> getIncidentMutualNodesTypes(U node)
    {
        Set<U> mutuals = this.getMutualNodes(node).collect(Collectors.toSet());
        return this.getIncidentNodesTypes(node).filter(w -> mutuals.contains(w.getIdx()));
    }

}
