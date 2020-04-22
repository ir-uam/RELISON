/* 
 *  Copyright (C) 2015 Information Retrieval Group at Universidad AutÃ³noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph.jung;

import cern.colt.matrix.DoubleMatrix2D;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import es.uam.eps.ir.socialnetwork.graph.UndirectedUnweightedGraph;
import es.uam.eps.ir.socialnetwork.graph.edges.EdgeOrientation;
import no.uib.cipr.matrix.Matrix;

/**
 * Undirected Graph Wrapper for JUNG
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
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
