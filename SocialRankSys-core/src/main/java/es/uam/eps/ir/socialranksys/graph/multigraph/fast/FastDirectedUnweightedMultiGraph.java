/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph.fast;


import es.uam.eps.ir.socialranksys.graph.Weight;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.multigraph.DirectedUnweightedMultiGraph;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.fast.FastDirectedUnweightedMultiEdges;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;

import java.util.stream.Stream;

/**
 * Fast implementation for a directed unweighted multi-graph. This implementation does not allow removing edges.
 *
 * @param <U> Type of the nodes
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastDirectedUnweightedMultiGraph<U> extends AbstractFastMultiGraph<U> implements DirectedUnweightedMultiGraph<U>
{
    /**
     * Constructor.
     */
    public FastDirectedUnweightedMultiGraph()
    {
        super(new FastIndex<>(), new FastDirectedUnweightedMultiEdges());
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
                        case IN -> this.getNumEdges(vidx, uidx);
                        case OUT -> this.getNumEdges(uidx, vidx);
                        case UND, MUTUAL -> this.getNumEdges(uidx, vidx) + this.getNumEdges(vidx, uidx);
                    };
            matrix[uidx][vidx] = weight;
        }));

        return matrix;
    }

    //TODO: All below
    @Override
    public Stream<U> getMutualNodes(U node)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getMutualEdgesCount(U node)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<Weight<U, Integer>> getAdjacentMutualNodesTypes(U node)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<Weight<U, Integer>> getIncidentMutualNodesTypes(U node)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
