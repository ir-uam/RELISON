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
import org.jblas.DoubleMatrix;

import java.util.stream.Stream;

/**
 * Fast implementation for an Undirected Weighted multigraph.
 *
 * @param <U> type of the nodes
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastUndirectedWeightedMultiGraph<U> extends AbstractFastMultiGraph<U> implements UndirectedWeightedMultiGraph<U>
{

    @Override
    public DoubleMatrix getJBLASAdjacencyMatrix(EdgeOrientation orientation)
    {
        int numUsers = Long.valueOf(this.getVertexCount()).intValue();
        DoubleMatrix matrix = DoubleMatrix.zeros(numUsers, numUsers);

        this.getNodesIdsWithEdges(EdgeOrientation.UND).forEach(uidx ->
            this.getNeighborhood(uidx, EdgeOrientation.UND).forEach(vidx ->
                matrix.put(uidx, vidx, this.getEdgeWeight(uidx, vidx)))
        );
        return matrix;
    }

    @Override
    public DoubleMatrix2D getAdjacencyMatrix(EdgeOrientation direction)
    {
        int numUsers = Long.valueOf(this.getVertexCount()).intValue();
        DoubleMatrix2D matrix = new SparseDoubleMatrix2D(numUsers, numUsers);

        this.getNodesIdsWithEdges(EdgeOrientation.UND).forEach(uidx ->
            this.getNeighborhood(uidx, EdgeOrientation.UND).forEach(vidx ->
                matrix.setQuick(uidx, vidx, this.getEdgeWeight(uidx, vidx)))
        );
        return matrix;
    }

    /**
     * Constructor for an empty graph
     */
    public FastUndirectedWeightedMultiGraph()
    {
        super(new FastIndex<>(), new FastUndirectedWeightedMultiEdges());
    }

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
