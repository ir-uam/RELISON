/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.tree.fast;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeWeight;
import es.uam.eps.ir.socialranksys.graph.edges.fast.FastDirectedUnweightedEdges;
import es.uam.eps.ir.socialranksys.graph.tree.Tree;
import es.uam.eps.ir.socialranksys.graph.tree.UnweightedTree;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.LinkedSparseMatrix;
import org.jblas.DoubleMatrix;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fast implementation of an unweighted tree
 *
 * @param <U> Type of the nodes.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastUnweightedTree<U> extends FastTree<U> implements UnweightedTree<U>
{

    /**
     * Constructor.
     */
    public FastUnweightedTree()
    {
        super(new FastDirectedUnweightedEdges());
    }

    @Override
    public Tree<U> getDescendants(U parent)
    {
        if (this.containsVertex(parent))
        {
            Tree<U> tree = new FastUnweightedTree<>();
            tree.addRoot(parent);

            // Perform a breadth first search, with fixed depth.
            LinkedList<U> currentLevelUsers = new LinkedList<>();
            LinkedList<U> nextLevelUsers = new LinkedList<>();
            currentLevelUsers.add(parent);
            while (!currentLevelUsers.isEmpty())
            {
                U current = currentLevelUsers.pop();
                List<U> children = this.getChildren(current).collect(Collectors.toCollection(ArrayList::new));
                for (U child : children)
                {
                    tree.addChild(current, child, EdgeWeight.getDefaultValue(), this.getEdgeType(current, child));
                    nextLevelUsers.add(child);
                }

                if (currentLevelUsers.isEmpty())
                {
                    currentLevelUsers.addAll(nextLevelUsers);
                    nextLevelUsers.clear();
                }
            }

            return tree;
        }
        else
        {
            return null;
        }
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
    public DoubleMatrix getJBLASAdjacencyMatrix(EdgeOrientation orientation)
    {
        return null;
    }

    public Matrix getAdjacencyMatrixMTJ(EdgeOrientation direction)
    {
        Matrix matrix = new LinkedSparseMatrix(Long.valueOf(this.getVertexCount()).intValue(), Long.valueOf(this.getVertexCount()).intValue());
        this.vertices.getAllObjects().forEach(u ->
        {
            int uIdx = this.vertices.object2idx(u);
            this.getNeighbourhood(u, direction).forEach(v ->
            {
                int vIdx = this.vertices.object2idx(v);
                matrix.set(uIdx, vIdx, EdgeWeight.getDefaultValue());
            });
        });

        return matrix;
    }

}
