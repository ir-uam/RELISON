/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph.fast;


import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import es.uam.eps.ir.socialranksys.graph.Weight;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.multigraph.UndirectedWeightedMultiGraph;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.fast.FastUndirectedWeightedMultiEdges;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.LinkedSparseMatrix;

import java.util.stream.Stream;

/**
 * Fast implementation for an Undirected Weighted multigraph.
 *
 * @param <U> type of the nodes
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastUndirectedWeightedMultiGraph<U> extends FastMultiGraph<U> implements UndirectedWeightedMultiGraph<U>
{

    /**
     * Constructor for an empty graph
     */
    public FastUndirectedWeightedMultiGraph()
    {
        super(new FastIndex<>(), new FastUndirectedWeightedMultiEdges());
    }

    @Override
    public DoubleMatrix2D getAdjacencyMatrix(EdgeOrientation direction)
    {
        DoubleMatrix2D matrix = new SparseDoubleMatrix2D(Long.valueOf(this.getVertexCount()).intValue(), Long.valueOf(this.getVertexCount()).intValue());
        // Creation of the adjacency matrix
        for (int row = 0; row < matrix.rows(); ++row)
        {
            for (int col = 0; col < matrix.rows(); ++col)
            {

                if (this.containsEdge(this.vertices.idx2object(col), this.vertices.idx2object(row)) ||
                        this.containsEdge(this.vertices.idx2object(row), this.vertices.idx2object(col)))
                {
                    matrix.setQuick(row, col, this.edges.getNumEdges(row, col));
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
                                                  this.getNeighbourNodes(u).forEach(v ->
                                                                                    {
                                                                                        int vIdx = this.vertices.object2idx(v);
                                                                                        matrix.set(uIdx, vIdx, this.edges.getNumEdges(vIdx, uIdx));
                                                                                    });
                                              });

        return matrix;
    }


    // TODO: All Below
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

}
