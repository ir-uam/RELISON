/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.graph.fast;

import es.uam.eps.ir.sonalire.graph.DirectedWeightedGraph;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.edges.fast.FastDirectedWeightedEdges;
import es.uam.eps.ir.sonalire.index.fast.FastIndex;

/**
 * Fast implementation of a directed weighted graph. This implementation does not allow to remove nodes/edges.
 *
 * @param <V> Type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastDirectedWeightedGraph<V> extends AbstractFastGraph<V> implements DirectedWeightedGraph<V>
{
    /**
     * Constructor.
     */
    public FastDirectedWeightedGraph()
    {
        super(new FastIndex<>(), new FastDirectedWeightedEdges());
    }

    @Override
    public double[][] getAdjacencyMatrix(EdgeOrientation direction)
    {
        int numUsers = Long.valueOf(this.getVertexCount()).intValue();
        double[][] matrix = new double[numUsers][numUsers];

        this.getAllNodesIds().forEach(uidx -> this.getNeighborhoodWeights(uidx, direction).forEach(vidx -> matrix[uidx][vidx.v1] = vidx.v2));
        return matrix;
    }
}
