/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.jung;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import es.uam.eps.ir.socialranksys.graph.DirectedUnweightedGraph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

/**
 * Directed Graph Wrapper for <a href="http://jung.sourceforge.net/">JUNG</a>
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
    public double[][] getAdjacencyMatrix(EdgeOrientation direction)
    {
        throw new UnsupportedOperationException("Unavailable method");
    }

    @Override
    public boolean updateEdgeWeight(U nodeA, U nodeB, double newWeight)
    {
        return this.containsEdge(nodeA, nodeB);
    }
}
