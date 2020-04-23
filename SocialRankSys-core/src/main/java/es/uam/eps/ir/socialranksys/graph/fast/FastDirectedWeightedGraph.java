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

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import es.uam.eps.ir.socialranksys.graph.DirectedWeightedGraph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.edges.fast.FastDirectedWeightedEdges;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.LinkedSparseMatrix;

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
public class FastDirectedWeightedGraph<V> extends FastGraph<V> implements DirectedWeightedGraph<V>
{
    /**
     * Constructor.
     */
    public FastDirectedWeightedGraph()
    {
        super(new FastIndex<>(), new FastDirectedWeightedEdges());
    }

    @Override
    public DoubleMatrix2D getAdjacencyMatrix(EdgeOrientation direction)
    {
        DoubleMatrix2D matrix = new SparseDoubleMatrix2D(Long.valueOf(this.getVertexCount()).intValue(), Long.valueOf(this.getVertexCount()).intValue());

        // Creation of the adjacency matrix.
        for (int row = 0; row < matrix.rows(); ++row)
        {
            for (int col = 0; col < matrix.rows(); ++col)
            {
                switch (direction)
                {
                    case IN:
                        if (this.containsEdge(this.vertices.idx2object(col), this.vertices.idx2object(row)))
                        {
                            matrix.setQuick(row, col, 1.0);
                        }
                        break;
                    case OUT:
                        if (this.containsEdge(this.vertices.idx2object(row), this.vertices.idx2object(col)))
                        {
                            matrix.setQuick(row, col, 1.0);
                        }
                        break;
                    default: //case UND
                        if (this.containsEdge(this.vertices.idx2object(col), this.vertices.idx2object(row)) ||
                                this.containsEdge(this.vertices.idx2object(row), this.vertices.idx2object(col)))
                        {
                            matrix.setQuick(row, col, 1.0);
                        }
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
                switch (direction)
                {
                    case IN:
                        matrix.set(uIdx, vIdx, 1.0);
                        break;
                    case OUT:
                        matrix.set(uIdx, vIdx, 1.0);
                        break;
                    default:
                        matrix.set(uIdx, vIdx, 1.0);
                }
            });
        });

        return matrix;
    }
}
