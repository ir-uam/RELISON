/* 
 *  Copyright (C) 2015 Information Retrieval Group at Universidad AutÃ³noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph.multigraph.fast;


import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import es.uam.eps.ir.socialnetwork.graph.Weight;
import es.uam.eps.ir.socialnetwork.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialnetwork.graph.index.fast.FastIndex;
import es.uam.eps.ir.socialnetwork.graph.multigraph.DirectedWeightedMultiGraph;
import es.uam.eps.ir.socialnetwork.graph.multigraph.edges.fast.FastDirectedWeightedMultiEdges;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.LinkedSparseMatrix;

import java.util.stream.Stream;

/**
 * Fast implementation of a directed weighted graph. This implementation does not allow to remove nodes/edges
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class FastDirectedWeightedMultiGraph<U> extends FastMultiGraph<U> implements DirectedWeightedMultiGraph<U>
{

    /**
     * Constructor. 
     */
    public FastDirectedWeightedMultiGraph()
    {
        super(new FastIndex<>(), new FastDirectedWeightedMultiEdges());
    }

    
    @Override
    public DoubleMatrix2D getAdjacencyMatrix(EdgeOrientation direction)
    {
        DoubleMatrix2D matrix = new SparseDoubleMatrix2D(Long.valueOf(this.getVertexCount()).intValue(), Long.valueOf(this.getVertexCount()).intValue());
        // Creation of the adjacency matrix
        for(int row = 0; row < matrix.rows(); ++row)
        {   
            for(int col = 0; col < matrix.rows(); ++col)
            {
                switch(direction)
                {
                    case IN:
                        if(this.containsEdge(this.vertices.idx2object(col), this.vertices.idx2object(row)))
                            matrix.setQuick(row, col, this.edges.getNumEdges(row, col));
                        break;
                    case OUT:
                        if(this.containsEdge(this.vertices.idx2object(row), this.vertices.idx2object(col)))
                            matrix.setQuick(row, col, this.edges.getNumEdges(row, col));
                        break;
                    default: //case UND
                        if(this.containsEdge(this.vertices.idx2object(col), this.vertices.idx2object(row)) ||
                            this.containsEdge(this.vertices.idx2object(row), this.vertices.idx2object(col)))
                            matrix.setQuick(row, col, this.edges.getNumEdges(row, col) + this.edges.getNumEdges(col, row));
                }
            }
        }
        
        return matrix;
    }

   
    @Override
    public Matrix getAdjacencyMatrixMTJ(EdgeOrientation direction)
    {
        Matrix matrix = new LinkedSparseMatrix(Long.valueOf(this.getVertexCount()).intValue(), Long.valueOf(this.getVertexCount()).intValue());
        this.vertices.getAllObjects().forEach(u -> 
        {
            int uIdx = this.vertices.object2idx(u);
            this.getNeighbourhood(u, direction).forEach(v -> 
            {
                int vIdx = this.vertices.object2idx(v);
                switch(direction)
                {
                    case IN:
                        matrix.set(uIdx, vIdx, this.edges.getNumEdges(vIdx,uIdx));
                        break;
                    case OUT:
                        matrix.set(uIdx, vIdx, this.edges.getNumEdges(uIdx,vIdx));
                        break;
                    default:
                        matrix.set(uIdx, vIdx, this.edges.getNumEdges(vIdx,uIdx)+this.edges.getNumEdges(uIdx,vIdx));
                }
            });
        });

        return matrix;
    }
  
    
    
    
    
    
    
    
    
    
    
    
    // TODO: All below
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
    public Stream<Weight<U, Double>> getAdjacentMutualNodesWeights(U node)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<Weight<U, Double>> getIncidentMutualNodesWeights(U node)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<Weight<U, Double>> getMutualNodesWeights(U node)
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
