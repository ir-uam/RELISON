/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad AutÃ³noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.jung;

import cern.colt.matrix.DoubleMatrix2D;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import es.uam.eps.ir.socialranksys.graph.DirectedUnweightedGraph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import no.uib.cipr.matrix.Matrix;

/**
 * Directed Graph Wrapper for JUNG
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DirectedJungGraph<U> extends JungGraph<U> implements DirectedUnweightedGraph<U>
{
    /**
     * Constructor.
     */
    public DirectedJungGraph()
    {
        super(new DirectedSparseGraph<>());
    }

    @Override
    public DoubleMatrix2D getAdjacencyMatrix(EdgeOrientation direction)
    {
        throw new UnsupportedOperationException("Unavailable method");
    }

    @Override
    public Matrix getAdjacencyMatrixMTJ(EdgeOrientation direction)
    {
        throw new UnsupportedOperationException("Unavailable method");
    }

    @Override
    public boolean updateEdgeWeight(U nodeA, U nodeB, double newWeight)
    {
        return this.containsEdge(nodeA, nodeB);
    }
}
