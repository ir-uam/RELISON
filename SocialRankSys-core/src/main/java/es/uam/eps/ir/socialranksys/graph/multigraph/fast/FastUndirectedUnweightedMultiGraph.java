/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph.fast;


import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.multigraph.UndirectedUnweightedMultiGraph;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.fast.FastUndirectedUnweightedMultiEdges;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;

/**
 * Fast implementation of an undirected unweighted multi-graph
 *
 * @param <U> Type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastUndirectedUnweightedMultiGraph<U> extends AbstractFastMultiGraph<U> implements UndirectedUnweightedMultiGraph<U>
{
    /**
     * Constructor.
     */
    public FastUndirectedUnweightedMultiGraph()
    {
        super(new FastIndex<>(), new FastUndirectedUnweightedMultiEdges());
    }

    @Override
    public double[][] getAdjacencyMatrix(EdgeOrientation direction)
    {
        int numUsers = Long.valueOf(this.getVertexCount()).intValue();
        double[][] matrix = new double[numUsers][numUsers];

        this.getAllNodesIds().forEach(uidx -> this.getNeighborhood(uidx, direction).forEach(vidx ->
               matrix[uidx][vidx] = this.getNumEdges(uidx, vidx)
        ));

        return matrix;
    }
}
