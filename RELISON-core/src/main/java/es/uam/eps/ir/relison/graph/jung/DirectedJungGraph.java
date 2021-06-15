/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.jung;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import es.uam.eps.ir.relison.graph.DirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.edges.EdgeWeight;
import es.uam.eps.ir.relison.index.Index;
import es.uam.eps.ir.relison.index.fast.FastIndex;

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
        Index<U> index = new FastIndex<>();
        this.graph.getVertices().stream().sorted().forEach(index::addObject);

        double[][] matrix = new double[graph.getVertexCount()][graph.getVertexCount()];
        this.graph.getVertices().stream().sorted().forEach(u ->
        {
            int uidx = index.object2idx(u);
            this.getNeighbourhood(u, direction).forEach(v ->
            {
                int vidx = index.object2idx(v);
                matrix[uidx][vidx] = EdgeWeight.getDefaultValue();
            });
        });

        return matrix;
    }

    @Override
    public boolean updateEdgeWeight(U nodeA, U nodeB, double newWeight)
    {
        return this.containsEdge(nodeA, nodeB);
    }


}
