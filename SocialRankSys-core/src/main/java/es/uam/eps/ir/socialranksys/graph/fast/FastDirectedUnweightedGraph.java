/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.fast;

import es.uam.eps.ir.socialranksys.graph.DirectedUnweightedGraph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeWeight;
import es.uam.eps.ir.socialranksys.graph.edges.fast.FastDirectedUnweightedEdges;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;

/**
 * Fast implementation for a directed unweighted graph. This implementation does not allow removing edges.
 *
 * @param <V> Type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastDirectedUnweightedGraph<V> extends AbstractFastGraph<V> implements DirectedUnweightedGraph<V>
{
    /**
     * Constructor.
     */
    public FastDirectedUnweightedGraph()
    {
        super(new FastIndex<>(), new FastDirectedUnweightedEdges());
    }

    @Override
    public double[][] getAdjacencyMatrix(EdgeOrientation direction)
    {
        int numUsers = Long.valueOf(this.getVertexCount()).intValue();
        double[][] matrix = new double[numUsers][numUsers];

        this.getAllNodesIds().forEach(uidx -> this.getNeighborhood(uidx, direction).forEach(vidx -> matrix[uidx][vidx] = EdgeWeight.getDefaultValue()));
        return matrix;
    }
}
