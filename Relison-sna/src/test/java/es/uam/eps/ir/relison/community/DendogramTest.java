/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.community;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.tree.Tree;
import es.uam.eps.ir.relison.index.fast.FastIndex;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.Dendogram;
import org.jooq.lambda.tuple.Tuple3;
import org.junit.*;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Automated unit tests for the dendogram class.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DendogramTest
{
    /**
     * The dendogram.
     */
    Dendogram<Long> dendogram;

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
        Graph<Long> graph = new FastUndirectedUnweightedGraph<>();
        List<Tuple3<Integer, Integer, Integer>> triplets = new ArrayList<>();
        FastIndex<Long> index = new FastIndex<>();

        index.addObject(100L);
        graph.addNode(100L);
        index.addObject(101L);
        graph.addNode(101L);
        index.addObject(102L);
        graph.addNode(102L);
        index.addObject(103L);
        graph.addNode(103L);
        index.addObject(104L);
        graph.addNode(104L);
        index.addObject(105L);
        graph.addNode(105L);
        index.addObject(106L);
        graph.addNode(106L);
        index.addObject(107L);
        graph.addNode(107L);
        index.addObject(108L);
        graph.addNode(108L);
        index.addObject(109L);
        graph.addNode(109L);

        Tuple3<Integer, Integer, Integer> triplet = new Tuple3<>(10, 17, 18);
        triplets.add(triplet);
        triplet = new Tuple3<>(12, 16, 17);
        triplets.add(triplet);
        triplet = new Tuple3<>(13, 15, 16);
        triplets.add(triplet);
        triplet = new Tuple3<>(9, 14, 15);
        triplets.add(triplet);
        triplet = new Tuple3<>(7, 8, 14);
        triplets.add(triplet);
        triplet = new Tuple3<>(5, 6, 13);
        triplets.add(triplet);
        triplet = new Tuple3<>(4, 11, 12);
        triplets.add(triplet);
        triplet = new Tuple3<>(2, 3, 11);
        triplets.add(triplet);
        triplet = new Tuple3<>(0, 1, 10);
        triplets.add(triplet);

        this.dendogram = new Dendogram<>(index, graph, triplets.stream());
    }

    @After
    public void tearDown()
    {

    }

    /**
     * Tests the construction of the tree.
     */
    @Test
    public void constructionTest()
    {
        // First, we should check the creation of the tree.
        Tree<Integer> tree = this.dendogram.getTree();

        // Check the leaf nodes.
        Assert.assertEquals(19, tree.getVertexCount());
        Set<Integer> set = new HashSet<>();
        set.add(0);
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        set.add(6);
        set.add(7);
        set.add(8);
        set.add(9);

        tree.getLeaves().forEach(leaf -> Assert.assertTrue(set.contains(leaf)));

        // Check the weights.
        Assert.assertEquals(1.0, tree.getEdgeWeight(10, 0), 1e-8);
        Assert.assertEquals(1.0, tree.getEdgeWeight(10, 1), 1e-8);
        Assert.assertEquals(1.0, tree.getEdgeWeight(11, 2), 1e-8);
        Assert.assertEquals(1.0, tree.getEdgeWeight(11, 3), 1e-8);
        Assert.assertEquals(1.0, tree.getEdgeWeight(12, 4), 1e-8);
        Assert.assertEquals(1.0, tree.getEdgeWeight(13, 5), 1e-8);
        Assert.assertEquals(1.0, tree.getEdgeWeight(13, 6), 1e-8);
        Assert.assertEquals(1.0, tree.getEdgeWeight(14, 7), 1e-8);
        Assert.assertEquals(1.0, tree.getEdgeWeight(14, 8), 1e-8);
        Assert.assertEquals(1.0, tree.getEdgeWeight(15, 9), 1e-8);
        Assert.assertEquals(2.0, tree.getEdgeWeight(18, 10), 1e-8);
        Assert.assertEquals(2.0, tree.getEdgeWeight(12, 11), 1e-8);
        Assert.assertEquals(2.0, tree.getEdgeWeight(16, 13), 1e-8);
        Assert.assertEquals(2.0, tree.getEdgeWeight(15, 14), 1e-8);
        Assert.assertEquals(3.0, tree.getEdgeWeight(17, 12), 1e-8);
        Assert.assertEquals(3.0, tree.getEdgeWeight(16, 15), 1e-8);
        Assert.assertEquals(5.0, tree.getEdgeWeight(17, 16), 1e-8);
        Assert.assertEquals(8.0, tree.getEdgeWeight(18, 17), 1e-8);
    }

    /**
     * Checks the method that gets all possible community divisions attending to the
     * number of communities.
     */
    @Test
    public void getCommunitiesByNumberTest()
    {
        Map<Integer, Communities<Long>> commMap = this.dendogram.getCommunitiesByNumber();
        Set<Long> set = new HashSet<>();
        IntStream.rangeClosed(1, 10).forEach(i ->
        {
            Communities<Long> commAux = commMap.get(i);

            switch (i)
            {
                case 1 ->
                {
                    Assert.assertEquals(1, commAux.getNumCommunities());
                    LongStream.rangeClosed(100, 109).forEach(set::add);
                    commAux.getUsers(0).forEach(u -> Assert.assertTrue(set.contains(u)));
                }
                case 2 ->
                {
                    Assert.assertEquals(2, commAux.getNumCommunities());
                    Assert.assertEquals(2L, commAux.getUsers(commAux.getCommunity(100L)).count());
                    commAux.getUsers(commAux.getCommunity(100L)).forEach(set::add);
                    LongStream.rangeClosed(100, 101).forEach(u -> Assert.assertTrue(set.contains(u)));
                    Assert.assertEquals(8L, commAux.getUsers(commAux.getCommunity(102L)).count());
                    set.clear();
                    commAux.getUsers(commAux.getCommunity(102L)).forEach(set::add);
                    LongStream.rangeClosed(102, 109).forEach(u -> Assert.assertEquals(8L, commAux.getUsers(commAux.getCommunity(u)).count()));
                }
                case 5 ->
                {
                    Assert.assertEquals(5, commAux.getNumCommunities());
                    Assert.assertEquals(2L, commAux.getUsers(commAux.getCommunity(100L)).count());
                    commAux.getUsers(commAux.getCommunity(100L)).forEach(set::add);
                    LongStream.rangeClosed(100, 101).forEach(u -> Assert.assertTrue(set.contains(u)));
                    Assert.assertEquals(3L, commAux.getUsers(commAux.getCommunity(102L)).count());
                    commAux.getUsers(commAux.getCommunity(102L)).forEach(set::add);
                    LongStream.rangeClosed(102, 104).forEach(u -> Assert.assertTrue(set.contains(u)));
                    Assert.assertEquals(2L, commAux.getUsers(commAux.getCommunity(105L)).count());
                    commAux.getUsers(commAux.getCommunity(105L)).forEach(set::add);
                    LongStream.rangeClosed(105, 106).forEach(u -> Assert.assertTrue(set.contains(u)));
                    Assert.assertEquals(2L, commAux.getUsers(commAux.getCommunity(107L)).count());
                    commAux.getUsers(commAux.getCommunity(107L)).forEach(set::add);
                    LongStream.rangeClosed(107, 108).forEach(u -> Assert.assertTrue(set.contains(u)));
                    Assert.assertEquals(1L, commAux.getUsers(commAux.getCommunity(109L)).count());
                    commAux.getUsers(commAux.getCommunity(109L)).forEach(set::add);
                    LongStream.rangeClosed(109, 109).forEach(u -> Assert.assertTrue(set.contains(u)));
                }
                case 10 -> LongStream.rangeClosed(100, 109).forEach(u -> Assert.assertEquals(1L, commAux.getUsers(commAux.getCommunity(u)).count()));
                default -> Assert.assertEquals(i, commAux.getNumCommunities());
            }
        });

    }

    /**
     * Checks the method that gets a single community division given the number of communities
     * we want to retrieve.
     */
    @Test
    public void getCommunitiesByNumberIndividualTest()
    {
        Communities<Long> comm;
        Set<Long> set = new HashSet<>();

        // Test with 5 communities
        comm = this.dendogram.getCommunitiesByNumber(5);
        Assert.assertEquals(5, comm.getNumCommunities());
        Assert.assertEquals(2L, comm.getUsers(comm.getCommunity(100L)).count());
        comm.getUsers(comm.getCommunity(100L)).forEach(set::add);
        LongStream.rangeClosed(100, 101).forEach(u -> Assert.assertTrue(set.contains(u)));

        Assert.assertEquals(3L, comm.getUsers(comm.getCommunity(102L)).count());
        comm.getUsers(comm.getCommunity(102L)).forEach(set::add);
        LongStream.rangeClosed(102, 104).forEach(u -> Assert.assertTrue(set.contains(u)));

        Assert.assertEquals(2L, comm.getUsers(comm.getCommunity(105L)).count());
        comm.getUsers(comm.getCommunity(105L)).forEach(set::add);
        LongStream.rangeClosed(105, 106).forEach(u -> Assert.assertTrue(set.contains(u)));

        Assert.assertEquals(2L, comm.getUsers(comm.getCommunity(107L)).count());
        comm.getUsers(comm.getCommunity(107L)).forEach(set::add);
        LongStream.rangeClosed(107, 108).forEach(u -> Assert.assertTrue(set.contains(u)));

        Assert.assertEquals(1L, comm.getUsers(comm.getCommunity(109L)).count());
        comm.getUsers(comm.getCommunity(109L)).forEach(set::add);
        LongStream.rangeClosed(109, 109).forEach(u -> Assert.assertTrue(set.contains(u)));


        // Test a unique community
        comm = this.dendogram.getCommunitiesByNumber(1);
        set.clear();
        Assert.assertEquals(1, comm.getNumCommunities());
        LongStream.rangeClosed(100, 109).forEach(set::add);
        comm.getUsers(0).forEach(u -> Assert.assertTrue(set.contains(u)));

        // Test the maximum number of communities
        comm = this.dendogram.getCommunitiesByNumber(10);
        Assert.assertEquals(10, comm.getNumCommunities());
        for (int i = 0; i < comm.getNumCommunities(); ++i)
        {
            Assert.assertEquals(1L, comm.getUsers(i).count());
        }

        // Test more than the maximum number of communities
        comm = this.dendogram.getCommunitiesByNumber(100);
        Assert.assertEquals(10, comm.getNumCommunities());
        for (int i = 0; i < comm.getNumCommunities(); ++i)
        {
            Assert.assertEquals(1L, comm.getUsers(i).count());
        }

        // Test fewer communities than the minimum (1)
        Assert.assertNull(this.dendogram.getCommunitiesByNumber(0));
        Assert.assertNull(this.dendogram.getCommunitiesByNumber(-1));
    }

    /**
     * Test the sizes of the communities.
     */
    @Test
    public void getCommunitiesBySizeTest()
    {
        Map<Integer, Communities<Long>> commMap = this.dendogram.getCommunitiesBySize();
        Assert.assertEquals(10, commMap.size());
        Set<Long> set = new HashSet<>();

        // Size comparison
        IntStream.rangeClosed(1, 10).forEach(i ->
        {
            Communities<Long> comm = commMap.get(i);
            int numComm = comm.getNumCommunities();

            IntStream.range(0, numComm).forEach(c -> Assert.assertTrue(comm.getUsers(c).count() <= i));
        });

        // Correctness of the communities.
        IntStream.rangeClosed(1, 10).forEach(i ->
        {
            Communities<Long> comm = commMap.get(i);
            set.clear();

            switch (i)
            {
                case 1 ->
                {
                    Assert.assertEquals(10, comm.getNumCommunities());
                    for (int j = 0; j < comm.getNumCommunities(); ++j)
                    {
                        Assert.assertEquals(1L, comm.getUsers(j).count());
                    }
                }
                case 2 ->
                {
                    Assert.assertEquals(6, comm.getNumCommunities());
                    comm.getUsers(comm.getCommunity(100L)).forEach(set::add);
                    LongStream.rangeClosed(100L, 101L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(102L)).forEach(set::add);
                    LongStream.rangeClosed(102L, 103L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    Assert.assertEquals(1L, comm.getUsers(comm.getCommunity(104L)).count());
                    comm.getUsers(comm.getCommunity(105L)).forEach(set::add);
                    LongStream.rangeClosed(105L, 106L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(107L)).forEach(set::add);
                    LongStream.rangeClosed(107L, 108L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    Assert.assertEquals(1L, comm.getUsers(comm.getCommunity(109L)).count());
                }
                case 3, 4 ->
                {
                    Assert.assertEquals(4, comm.getNumCommunities());
                    comm.getUsers(comm.getCommunity(100L)).forEach(set::add);
                    LongStream.rangeClosed(100L, 101L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(102L)).forEach(set::add);
                    LongStream.rangeClosed(102L, 104L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(105L)).forEach(set::add);
                    LongStream.rangeClosed(105L, 106L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(107L)).forEach(set::add);
                    LongStream.rangeClosed(107L, 109L).forEach(u -> Assert.assertTrue(set.contains(u)));
                }
                case 5, 6, 7 ->
                {
                    Assert.assertEquals(3, comm.getNumCommunities());
                    comm.getUsers(comm.getCommunity(100L)).forEach(set::add);
                    LongStream.rangeClosed(100L, 101L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(102L)).forEach(set::add);
                    LongStream.rangeClosed(102L, 104L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(105L)).forEach(set::add);
                    LongStream.rangeClosed(105L, 109L).forEach(u -> Assert.assertTrue(set.contains(u)));
                }
                case 8, 9 ->
                {
                    Assert.assertEquals(2, comm.getNumCommunities());
                    comm.getUsers(comm.getCommunity(100L)).forEach(set::add);
                    LongStream.rangeClosed(100L, 101L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(102L)).forEach(set::add);
                    LongStream.rangeClosed(102L, 109L).forEach(u -> Assert.assertTrue(set.contains(u)));
                }
                case 10 ->
                {
                    Assert.assertEquals(1, comm.getNumCommunities());
                    Assert.assertEquals(10, comm.getUsers(comm.getNumCommunities() - 1).count());
                }
            }
        });
    }

    @Test
    public void getCommunitiesBySizeIndividualTest()
    {
        Set<Long> set = new HashSet<>();

        // Correctness of the communities.
        IntStream.rangeClosed(1, 11).forEach(i ->
        {
            Communities<Long> comm = this.dendogram.getCommunitiesBySize(i);
            int numComm = comm.getNumCommunities();

            IntStream.range(0, numComm).forEach(c -> Assert.assertTrue(comm.getUsers(c).count() <= i));
            set.clear();

            switch (i)
            {
                case 1 ->
                {
                    Assert.assertEquals(10, comm.getNumCommunities());
                    for (int j = 0; j < comm.getNumCommunities(); ++j)
                    {
                        Assert.assertEquals(1L, comm.getUsers(j).count());
                    }
                }
                case 2 ->
                {
                    Assert.assertEquals(6, comm.getNumCommunities());
                    comm.getUsers(comm.getCommunity(100L)).forEach(set::add);
                    LongStream.rangeClosed(100L, 101L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(102L)).forEach(set::add);
                    LongStream.rangeClosed(102L, 103L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    Assert.assertEquals(1L, comm.getUsers(comm.getCommunity(104L)).count());
                    comm.getUsers(comm.getCommunity(105L)).forEach(set::add);
                    LongStream.rangeClosed(105L, 106L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(107L)).forEach(set::add);
                    LongStream.rangeClosed(107L, 108L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    Assert.assertEquals(1L, comm.getUsers(comm.getCommunity(109L)).count());
                }
                case 3, 4 ->
                {
                    Assert.assertEquals(4, comm.getNumCommunities());
                    comm.getUsers(comm.getCommunity(100L)).forEach(set::add);
                    LongStream.rangeClosed(100L, 101L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(102L)).forEach(set::add);
                    LongStream.rangeClosed(102L, 104L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(105L)).forEach(set::add);
                    LongStream.rangeClosed(105L, 106L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(107L)).forEach(set::add);
                    LongStream.rangeClosed(107L, 109L).forEach(u -> Assert.assertTrue(set.contains(u)));
                }
                case 5, 6, 7 ->
                {
                    Assert.assertEquals(3, comm.getNumCommunities());
                    comm.getUsers(comm.getCommunity(100L)).forEach(set::add);
                    LongStream.rangeClosed(100L, 101L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(102L)).forEach(set::add);
                    LongStream.rangeClosed(102L, 104L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(105L)).forEach(set::add);
                    LongStream.rangeClosed(105L, 109L).forEach(u -> Assert.assertTrue(set.contains(u)));
                }
                case 8, 9 ->
                {
                    Assert.assertEquals(2, comm.getNumCommunities());
                    comm.getUsers(comm.getCommunity(100L)).forEach(set::add);
                    LongStream.rangeClosed(100L, 101L).forEach(u -> Assert.assertTrue(set.contains(u)));
                    set.clear();
                    comm.getUsers(comm.getCommunity(102L)).forEach(set::add);
                    LongStream.rangeClosed(102L, 109L).forEach(u -> Assert.assertTrue(set.contains(u)));
                }
                case 10, 11 ->
                {
                    Assert.assertEquals(1, comm.getNumCommunities());
                    Assert.assertEquals(10, comm.getUsers(comm.getNumCommunities() - 1).count());
                }
            }
        });
    }
}
