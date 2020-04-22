/*
 *  Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph.tree;

import es.uam.eps.ir.socialnetwork.graph.DirectedGraph;
import es.uam.eps.ir.socialnetwork.graph.Weight;
import es.uam.eps.ir.socialnetwork.graph.edges.EdgeType;
import es.uam.eps.ir.socialnetwork.graph.edges.EdgeWeight;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Interface for managing and creating tree graphs.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the nodes.
 */
public interface Tree<U> extends DirectedGraph<U>
{
    /**
     * Obtains the root of the graph.
     * @return the root of the graph.
     */
    U getRoot();
    /**
     * Obtain the leaves of the tree.
     * @return a stream containing the leaves of the tree.
     */
    Stream<U> getLeaves();
    /**
     * Obtains the leaves of a tree that come from a certain node
     * @param u the leaves of the tree.
     * @return a stream containing the selected leaves of the tree.
     */
    Stream<U> getLeaves(U u);
    /**
     * Obtains all the nodes in a given level of the tree. Root equal to 0.
     * @param level the level to retrieve.
     * @return the nodes in the corresponding level.
     */
    Stream<U> getLevel(int level);
    /**
     * Obtains all the nodes on each level.
     * @return all the nodes on each level.
     */
    Map<Integer, Set<U>> getLevels();
    
    /**
     * Obtains the direct children of a node.
     * @param u the parent node
     * @return A stream containing the children of a node. If it is a leaf, an
     * empty stream is returned. If the node does not exist, null.
     */
    Stream<U> getChildren(U u);
    
    /**
     * Gets the number of children of a node.
     * @param u the node.
     * @return the number of children, -1 if the node does not exist.
     */
    long getChildrenCount(U u);

    /**
     * Obtains the weights of the children of a node.
     * @param u The parent node.
     * @return A stream containing the children of the node if it exists, null if not.
     */
    Stream<Weight<U,Double>> getChildrenWeights(U u);
    
    /**
     * Obtains the weight of the edge between a node and its parent
     * @param u The child node
     * @return The weight if the node exists and it is not the root, NaN otherwise.
     */
    double getParentWeight(U u);
    
    /**
     * Obtains the parent of a node.
     * @param u the child.
     * @return the parent of the node, null if the
     * node does not exist, or the node is the root.
     */
    U getParent(U u);
    /**
     * Indicates if the node is a leaf or not.
     * @param u the user 
     * @return true if the node is a leaf, false if the node is not a leaf or it does not exist.
     */
    boolean isLeaf(U u);
    /**
     * Indicates if the node is the root node or not.
     * @param u the user.
     * @return true if the node is the root, false if the node is not the root or it does not exist.
     */
    boolean isRoot(U u);
    /**
     * Adds a root to the tree.
     * @param root the new root of the tree.
     * @return true if the root is correctly added, false if there is already a root
     * in the graph.
     */
    boolean addRoot(U root);
    /**
     * Adds a child to a given node.
     * @param parent the parent node.
     * @param child the child node to add.
     * @return true if the child is correctly added, false if not.
     */
    default boolean addChild(U parent, U child)
    {
        return this.addChild(parent, child, EdgeWeight.getDefaultValue());
    }
    /**
     * Adds a child to a given node
     * @param parent the parent node.
     * @param child the child node to add.
     * @param weight the weight of the edge
     * @return true if the child is correctly added, false if not.
     */
    default boolean addChild(U parent, U child, double weight)
    {
        return this.addChild(parent, child, weight, EdgeType.getDefaultValue());
    }
    
    /**
     * Adds a child to a given node.
     * @param parent the parent node.
     * @param child the child node to add.
     * @param weight the weight of the edge.
     * @param type the type of the edge.
     * @return true if the child is correcly added, false if not.
     */
    boolean addChild(U parent, U child, double weight, int type);
            
    /**
     * Checks if a node is the parent of another one.
     * @param parent the parent node.
     * @param child the node we want to check.
     * @return true if the child
     */
    boolean isParent(U parent, U child);
    
    /**
     * Checks if a node is the parent of another one.
     * @param child the node we want to check.
     * @param parent the parent node.
     * @return true if the child
     */
    default boolean isChild(U child, U parent)
    {
        return this.isParent(parent, child);
    }
    
    /**
     * Obtains the subtree formed by the user and its descendants.
     * @param parent the parent node.
     * @return a tree rooted in the parent if the node exists, null if not
     */
    Tree<U> getDescendants(U parent);
    
    /**
     * Checks if a node is ascendant of another one
     * @param parent the first node (the possible ascendant)
     * @param child the second node (the possible descendant)
     * @return true if the second node descends from the first, false if not.
     */
    boolean isAscendant(U parent, U child);
    
    /**
     * Checks if a node is descendant of another one
     * @param child the first node (the possible descendant)
     * @param parent the second node (the possible ascendant)
     * @return true if the second node descends from the first, false if not.
     */
    default boolean isDescendant(U child, U parent)
    {
        return this.isAscendant(parent, child);
    }

    @Override
    default boolean addNode(U node)
    {
        throw new UnsupportedOperationException("Trees do not allow adding nodes this way");
    }
    
    @Override
    default boolean addEdge(U nodeA, U nodeB, double weight, int type, boolean insertNodes)
    {
        throw new UnsupportedOperationException("Trees do not allow adding nodes this way");
    }
}
