/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.generator;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.relison.graph.tree.Tree;

import java.util.LinkedList;

/**
 * Class for cloning trees.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class TreeCloneGenerator<U> implements GraphGenerator<U>
{
    /**
     * Tree to clone
     */
    private Tree<U> tree;
    /**
     * Indicates if the tree has been configured
     */
    private boolean configured = false;

    /**
     * Configure the tree generator
     *
     * @param tree the tree to clone
     */
    public void configure(Tree<U> tree)
    {
        this.tree = tree;
        this.configured = true;
    }

    @Override
    public void configure(Object... configuration)
    {
        if (!(configuration == null) && configuration.length == 1)
        {
            Tree<U> auxGraph = (Tree<U>) configuration[0];
            this.configure(auxGraph);
        }
    }

    @Override
    public Graph<U> generate() throws GeneratorNotConfiguredException, GeneratorBadConfiguredException
    {
        if (!this.configured)
        {
            throw new GeneratorNotConfiguredException("Graph cloner: Generator was not configured");
        }
        else if (this.tree == null)
        {
            throw new GeneratorNotConfiguredException("Graph cloner: Generator was badly configured");
        }

        GraphGenerator<U> emptyTreeGen = new EmptyTreeGenerator<>();
        emptyTreeGen.configure(tree.isWeighted());
        Tree<U> newTree = (Tree<U>) emptyTreeGen.generate();

        LinkedList<U> currentLevelUsers = new LinkedList<>();
        LinkedList<U> nextLevelUsers = new LinkedList<>();
        currentLevelUsers.add(tree.getRoot());

        while (!currentLevelUsers.isEmpty())
        {
            U current = currentLevelUsers.pop();
            if (tree.isRoot(current))
            {
                newTree.addRoot(current);
            }
            else
            {
                newTree.addChild(tree.getParent(current), current, tree.getParentWeight(current));
            }

            tree.getChildren(current).forEach(nextLevelUsers::add);

            if (currentLevelUsers.isEmpty())
            {
                currentLevelUsers.addAll(nextLevelUsers);
                nextLevelUsers.clear();
            }
        }

        return newTree;
    }

}
