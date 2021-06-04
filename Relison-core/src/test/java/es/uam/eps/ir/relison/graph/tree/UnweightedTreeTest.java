/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.relison.graph.tree;


import es.uam.eps.ir.relison.graph.tree.fast.FastUnweightedTree;
import org.junit.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Class that tests the fast unweighted implementation of trees.
 *
 * @author Javier Sanz-Cruzado Puig
 */
public class UnweightedTreeTest
{
    /**
     * Constructor.
     */
    public UnweightedTreeTest()
    {

    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    /**
     * Tests the addition of the root to the graph.
     */
    @Test
    public void rootTest()
    {
        UnweightedTree<Integer> firstTree = new FastUnweightedTree<>();
        assertFalse(firstTree.addChild(0, 1));
        assertNull(firstTree.getRoot());
        assertTrue(firstTree.addRoot(0));
        assertEquals((Integer) 0, firstTree.getRoot());
        assertTrue(firstTree.isRoot(0));
        assertFalse(firstTree.isRoot(1));
    }

    /**
     * Tests the addition of children to the graph.
     */
    @Test
    public void childrenAddition()
    {
        UnweightedTree<Integer> tree = new FastUnweightedTree<>();
        assertTrue(tree.addRoot(0));
        assertFalse(tree.addChild(1, 0));
        assertTrue(tree.addChild(0, 1));
        assertTrue(tree.addChild(1, 2));
        assertFalse(tree.addChild(2, 0));

        // Check that the vertices and edges have been created
        assertTrue(tree.containsVertex(0));
        assertTrue(tree.containsVertex(1));
        assertTrue(tree.containsVertex(2));
        assertFalse(tree.containsEdge(2, 0));
        assertFalse(tree.containsEdge(2, 1));
        assertFalse(tree.containsEdge(1, 0));
        assertTrue(tree.containsEdge(1, 2));
        assertTrue(tree.containsEdge(0, 1));
        assertFalse(tree.containsEdge(0, 2));
    }

    /**
     * Tests the functions that check if a node is parent or child of other node.
     */
    @Test
    public void parentalCheck()
    {
        UnweightedTree<Integer> tree = new FastUnweightedTree<>();
        assertTrue(tree.addRoot(0));
        assertTrue(tree.addChild(0, 1));
        assertTrue(tree.addChild(1, 2));

        // Check the isParent function
        assertFalse(tree.isParent(2, 0));
        assertFalse(tree.isParent(2, 1));
        assertFalse(tree.isParent(1, 0));
        assertTrue(tree.isParent(1, 2));
        assertTrue(tree.isParent(0, 1));
        assertFalse(tree.isParent(0, 2));

        // Check the isChild function
        assertFalse(tree.isChild(0, 2));
        assertFalse(tree.isChild(1, 2));
        assertFalse(tree.isChild(0, 1));
        assertTrue(tree.isChild(2, 1));
        assertTrue(tree.isChild(1, 0));
        assertFalse(tree.isChild(2, 0));
    }

    /**
     * Test the functions that check if a node descends or ascends from other node.
     */
    @Test
    public void descendanceCheck()
    {
        UnweightedTree<Integer> tree = new FastUnweightedTree<>();
        assertTrue(tree.addRoot(0));
        assertTrue(tree.addChild(0, 1));
        assertTrue(tree.addChild(1, 2));

        // Check the isAscendant function
        assertFalse(tree.isAscendant(2, 0));
        assertFalse(tree.isAscendant(2, 1));
        assertFalse(tree.isAscendant(1, 0));
        assertTrue(tree.isAscendant(1, 2));
        assertTrue(tree.isAscendant(0, 1));
        assertTrue(tree.isAscendant(0, 2));

        // Check the isDescendant function
        assertFalse(tree.isDescendant(0, 2));
        assertFalse(tree.isDescendant(1, 2));
        assertFalse(tree.isDescendant(0, 1));
        assertTrue(tree.isDescendant(2, 1));
        assertTrue(tree.isDescendant(1, 0));
        assertTrue(tree.isDescendant(2, 0));
    }

    /**
     * Tests the functions for obtaining parents and children of nodes.
     */
    @Test
    public void obtainParentsAndChildren()
    {
        UnweightedTree<Integer> tree = new FastUnweightedTree<>();
        assertTrue(tree.addRoot(0));
        assertTrue(tree.addChild(0, 1));
        assertTrue(tree.addChild(1, 2));

        // Check the getParent function
        assertNull(tree.getParent(0));
        assertEquals((Integer) 0, tree.getParent(1));
        assertEquals((Integer) 1, tree.getParent(2));
        assertNull(tree.getParent(3));

        // Check the getChildren function
        assertTrue(tree.addChild(0, 3));
        Set<Integer> set = new HashSet<>();
        set.add(1);
        set.add(3);
        tree.getChildren(0).forEach(child -> assertTrue(set.contains(child)));
        assertEquals(2L, tree.getChildren(0).count());

        set.clear();
        set.add(2);
        tree.getChildren(1).forEach(child -> assertTrue(set.contains(child)));
        assertEquals(1L, tree.getChildren(1).count());

        assertEquals(0L, tree.getChildren(2).count());
        assertEquals(0L, tree.getChildren(3).count());
    }

    /**
     * Test the functions for obtaining the different levels of a graph.
     */
    @Test
    public void levels()
    {
        // Tree 1: Straight line
        UnweightedTree<Integer> tree = new FastUnweightedTree<>();
        assertTrue(tree.addRoot(0));
        assertTrue(tree.addChild(0, 1));
        assertTrue(tree.addChild(1, 2));
        assertTrue(tree.addChild(2, 3));
        assertTrue(tree.addChild(3, 4));

        Set<Integer> level0 = new HashSet<>();
        level0.add(0);
        Set<Integer> level1 = new HashSet<>();
        level1.add(1);
        Set<Integer> level2 = new HashSet<>();
        level2.add(2);
        Set<Integer> level3 = new HashSet<>();
        level3.add(3);
        Set<Integer> level4 = new HashSet<>();
        level4.add(4);

        // Individually find the levels
        assertEquals(level0.size(), tree.getLevel(0).count());
        tree.getLevel(0).forEach(u -> assertTrue(level0.contains(u)));
        assertEquals(level1.size(), tree.getLevel(1).count());
        tree.getLevel(1).forEach(u -> assertTrue(level1.contains(u)));
        assertEquals(level2.size(), tree.getLevel(2).count());
        tree.getLevel(2).forEach(u -> assertTrue(level2.contains(u)));
        assertEquals(level3.size(), tree.getLevel(3).count());
        tree.getLevel(3).forEach(u -> assertTrue(level3.contains(u)));
        assertEquals(level4.size(), tree.getLevel(4).count());
        tree.getLevel(4).forEach(u -> assertTrue(level4.contains(u)));

        // Find all the possible levels
        Map<Integer, Set<Integer>> levels = tree.getLevels();
        assertEquals(level0.size(), tree.getLevel(0).count());
        levels.get(0).forEach(u -> assertTrue(level0.contains(u)));
        assertEquals(level1.size(), tree.getLevel(1).count());
        levels.get(1).forEach(u -> assertTrue(level1.contains(u)));
        assertEquals(level2.size(), tree.getLevel(2).count());
        levels.get(2).forEach(u -> assertTrue(level2.contains(u)));
        assertEquals(level3.size(), tree.getLevel(3).count());
        levels.get(3).forEach(u -> assertTrue(level3.contains(u)));
        assertEquals(level4.size(), tree.getLevel(4).count());
        levels.get(4).forEach(u -> assertTrue(level4.contains(u)));

        // Tree 2: Binary tree
        tree = new FastUnweightedTree<>();
        assertTrue(tree.addRoot(0));
        assertTrue(tree.addChild(0, 1));
        assertTrue(tree.addChild(0, 2));
        assertTrue(tree.addChild(1, 3));
        assertTrue(tree.addChild(1, 4));
        assertTrue(tree.addChild(3, 5));
        assertTrue(tree.addChild(3, 6));

        level0.clear();
        level0.add(0);
        level1.clear();
        level1.add(1);
        level1.add(2);
        level2.clear();
        level2.add(3);
        level2.add(4);
        level3.clear();
        level3.add(5);
        level3.add(6);
        level4.clear();

        assertEquals(level0.size(), tree.getLevel(0).count());
        tree.getLevel(0).forEach(u -> assertTrue(level0.contains(u)));
        assertEquals(level1.size(), tree.getLevel(1).count());
        tree.getLevel(1).forEach(u -> assertTrue(level1.contains(u)));
        assertEquals(level2.size(), tree.getLevel(2).count());
        tree.getLevel(2).forEach(u -> assertTrue(level2.contains(u)));
        assertEquals(level3.size(), tree.getLevel(3).count());
        tree.getLevel(3).forEach(u -> assertTrue(level3.contains(u)));
        assertEquals(level4.size(), tree.getLevel(4).count());
        tree.getLevel(4).forEach(u -> assertTrue(level4.contains(u)));

        levels = tree.getLevels();
        assertEquals(level0.size(), tree.getLevel(0).count());
        levels.get(0).forEach(u -> assertTrue(level0.contains(u)));
        assertEquals(level1.size(), tree.getLevel(1).count());
        levels.get(1).forEach(u -> assertTrue(level1.contains(u)));
        assertEquals(level2.size(), tree.getLevel(2).count());
        levels.get(2).forEach(u -> assertTrue(level2.contains(u)));
        assertEquals(level3.size(), tree.getLevel(3).count());
        levels.get(3).forEach(u -> assertTrue(level3.contains(u)));
        assertEquals(level4.size(), tree.getLevel(4).count());
        levels.get(4).forEach(u -> assertTrue(level4.contains(u)));
    }

    /**
     * Tests the method for identifying if a root is a leaf or not.
     */
    @Test
    public void checkLeaves()
    {
        // Tree 1: Straight line
        UnweightedTree<Integer> tree = new FastUnweightedTree<>();
        assertTrue(tree.addRoot(0));
        assertTrue(tree.addChild(0, 1));
        assertTrue(tree.addChild(1, 2));
        assertTrue(tree.addChild(2, 3));
        assertTrue(tree.addChild(3, 4));

        assertFalse(tree.isLeaf(0));
        assertFalse(tree.isLeaf(1));
        assertFalse(tree.isLeaf(2));
        assertFalse(tree.isLeaf(3));
        assertTrue(tree.isLeaf(4));
        assertFalse(tree.isLeaf(5));


        // Tree 2: Binary tree
        tree = new FastUnweightedTree<>();
        assertTrue(tree.addRoot(0));
        assertTrue(tree.addChild(0, 1));
        assertTrue(tree.addChild(0, 2));
        assertTrue(tree.addChild(1, 3));
        assertTrue(tree.addChild(1, 4));
        assertTrue(tree.addChild(3, 5));
        assertTrue(tree.addChild(3, 6));

        assertFalse(tree.isLeaf(0));
        assertFalse(tree.isLeaf(1));
        assertTrue(tree.isLeaf(2));
        assertFalse(tree.isLeaf(3));
        assertTrue(tree.isLeaf(4));
        assertTrue(tree.isLeaf(5));
        assertTrue(tree.isLeaf(6));
    }

    /**
     * Tests the methods for obtaining the leaves of the tree.
     */
    @Test
    public void obtainLeaves()
    {
        // Tree 1: Straight line
        UnweightedTree<Integer> tree = new FastUnweightedTree<>();
        assertTrue(tree.addRoot(0));
        assertTrue(tree.addChild(0, 1));
        assertTrue(tree.addChild(1, 2));
        assertTrue(tree.addChild(2, 3));
        assertTrue(tree.addChild(3, 4));

        Set<Integer> leafs0 = new HashSet<>();
        leafs0.add(4);
        Set<Integer> leafs1 = new HashSet<>();
        leafs1.add(4);
        Set<Integer> leafs2 = new HashSet<>();
        leafs2.add(4);
        Set<Integer> leafs3 = new HashSet<>();
        leafs3.add(4);
        Set<Integer> leafs4 = new HashSet<>();
        Set<Integer> leaves = new HashSet<>();
        leaves.add(4);

        // Individually find the levels
        assertEquals(leafs0.size(), tree.getLeaves(0).count());
        tree.getLeaves(0).forEach(u -> assertTrue(leafs0.contains(u)));
        assertEquals(leafs1.size(), tree.getLeaves(1).count());
        tree.getLeaves(1).forEach(u -> assertTrue(leafs1.contains(u)));
        assertEquals(leafs2.size(), tree.getLeaves(2).count());
        tree.getLeaves(2).forEach(u -> assertTrue(leafs2.contains(u)));
        assertEquals(leafs3.size(), tree.getLeaves(3).count());
        tree.getLeaves(3).forEach(u -> assertTrue(leafs3.contains(u)));
        assertEquals(leafs4.size(), tree.getLeaves(4).count());
        tree.getLeaves(4).forEach(u -> fail());
        assertEquals(leaves.size(), tree.getLeaves().count());
        tree.getLeaves().forEach(u -> assertTrue(leaves.contains(u)));


        // Tree 2: Binary tree
        tree = new FastUnweightedTree<>();
        assertTrue(tree.addRoot(0));
        assertTrue(tree.addChild(0, 1));
        assertTrue(tree.addChild(0, 2));
        assertTrue(tree.addChild(1, 3));
        assertTrue(tree.addChild(1, 4));
        assertTrue(tree.addChild(3, 5));
        assertTrue(tree.addChild(3, 6));

        leafs0.clear();
        leafs0.add(2);
        leafs0.add(4);
        leafs0.add(5);
        leafs0.add(6);
        leafs1.clear();
        leafs1.add(4);
        leafs1.add(5);
        leafs1.add(6);
        leafs2.clear();
        leafs3.clear();
        leafs3.add(5);
        leafs3.add(6);
        Set<Integer> leafs5 = new HashSet<>();
        Set<Integer> leafs6 = new HashSet<>();
        leaves.clear();
        leaves.add(2);
        leaves.add(4);
        leaves.add(5);
        leaves.add(6);

        // Individually find the levels
        assertEquals(leafs0.size(), tree.getLeaves(0).count());
        tree.getLeaves(0).forEach(u -> assertTrue(leafs0.contains(u)));
        assertEquals(leafs1.size(), tree.getLeaves(1).count());
        tree.getLeaves(1).forEach(u -> assertTrue(leafs1.contains(u)));
        assertEquals(leafs2.size(), tree.getLeaves(2).count());
        tree.getLeaves(2).forEach(u -> assertTrue(leafs2.contains(u)));
        assertEquals(leafs3.size(), tree.getLeaves(3).count());
        tree.getLeaves(3).forEach(u -> assertTrue(leafs3.contains(u)));
        assertEquals(leafs4.size(), tree.getLeaves(4).count());
        tree.getLeaves(4).forEach(u -> fail());
        assertEquals(leafs5.size(), tree.getLeaves(5).count());
        tree.getLeaves(5).forEach(u -> fail());
        assertEquals(leafs6.size(), tree.getLeaves(6).count());
        tree.getLeaves(6).forEach(u -> fail());
        assertEquals(leaves.size(), tree.getLeaves().count());
        tree.getLeaves().forEach(u -> assertTrue(leaves.contains(u)));

        // Tree 3: only root
        tree = new FastUnweightedTree<>();
        assertTrue(tree.addRoot(0));

        leafs0.clear();
        leaves.clear();
        leaves.add(0);
        assertEquals(leafs0.size(), tree.getLeaves(0).count());
        tree.getLeaves(0).forEach(u -> assertTrue(leafs0.contains(u)));
        assertEquals(leaves.size(), tree.getLeaves().count());
        tree.getLeaves().forEach(u -> assertTrue(leaves.contains(u)));

        // Tree 4: empty graph
        tree = new FastUnweightedTree<>();
        assertEquals(0, tree.getLeaves().count());
        assertNull(tree.getLeaves(0));
    }
}
