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
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import es.uam.eps.ir.socialranksys.graph.UndirectedUnweightedGraph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import no.uib.cipr.matrix.Matrix;

/**
 * Undirected Graph Wrapper for JUNG
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class UndirectedJungGraph<U> extends JungGraph<U> implements UndirectedUnweightedGraph<U>
{
    /**
     * Constructor.
     */
    public UndirectedJungGraph()
    {
        super(new UndirectedSparseGraph<>());
    }

    @Override
    public boolean updateEdgeWeight(U orig, U dest, double weight)
    {
        return this.containsEdge(orig, dest);
    }

    @Override
    public DoubleMatrix2D getAdjacencyMatrix(EdgeOrientation direction)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Matrix getAdjacencyMatrixMTJ(EdgeOrientation direction)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
