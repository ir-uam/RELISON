/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.tree.fast;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.edges.EdgeWeight;
import es.uam.eps.ir.relison.graph.edges.fast.FastDirectedUnweightedEdges;
import es.uam.eps.ir.relison.graph.tree.Tree;
import es.uam.eps.ir.relison.graph.tree.UnweightedTree;

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
    public double[][] getAdjacencyMatrix(EdgeOrientation direction)
    {
        int numUsers = Long.valueOf(this.getVertexCount()).intValue();
        double[][] matrix = new double[numUsers][numUsers];
        this.getAllNodesIds().forEach(uidx ->
            this.getNeighborhood(uidx, direction).forEach(vidx ->
                matrix[uidx][vidx] = EdgeWeight.getDefaultValue()
            )
        );
        return matrix;
    }
}
