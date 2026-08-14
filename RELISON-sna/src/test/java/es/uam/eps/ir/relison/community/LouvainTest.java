/*
 *  Copyright (C) 2024 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.community;

import es.uam.eps.ir.relison.graph.UndirectedGraph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedUnweightedGraph;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.detection.modularity.Louvain;
import es.uam.eps.ir.relison.sna.metrics.communities.graph.Modularity;
import org.junit.Assert;
import org.junit.Test;

/**
 * Correctness tests for the {@link Louvain} community detection algorithm. These guard the modularity-gain
 * computation of phase 1: a bug there (e.g. double-counting the moved node in its own community) makes the local
 * moves oscillate and the detected partition wrong, which is exactly what these assertions catch.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class LouvainTest
{
    /**
     * Two triangles joined by a single bridge edge. The unambiguous optimum splits the graph into the two triangles,
     * with a modularity of 0.35714…; anything else (all-in-one, all-singletons, or a spurious split caused by a broken
     * gain) has strictly lower modularity, so Louvain must recover exactly this partition.
     */
    @Test
    public void twoTriangles()
    {
        UndirectedGraph<Integer> graph = new FastUndirectedUnweightedGraph<>();
        for (int i = 1; i <= 6; ++i) graph.addNode(i);
        // Triangle A: 1-2-3
        graph.addEdge(1, 2); graph.addEdge(1, 3); graph.addEdge(2, 3);
        // Triangle B: 4-5-6
        graph.addEdge(4, 5); graph.addEdge(4, 6); graph.addEdge(5, 6);
        // Bridge
        graph.addEdge(3, 4);

        Communities<Integer> comms = new Louvain<Integer>(0, 0.0001).detectCommunities(graph);

        Assert.assertNotNull("Louvain returned no partition", comms);
        Assert.assertEquals("Expected exactly two communities", 2, comms.getNumCommunities());

        // Each triangle is a single community, and the two triangles are different communities.
        Assert.assertEquals(comms.getCommunity(1), comms.getCommunity(2));
        Assert.assertEquals(comms.getCommunity(2), comms.getCommunity(3));
        Assert.assertEquals(comms.getCommunity(4), comms.getCommunity(5));
        Assert.assertEquals(comms.getCommunity(5), comms.getCommunity(6));
        Assert.assertNotEquals(comms.getCommunity(3), comms.getCommunity(4));

        // The recovered partition has the same modularity as the hand-built two-triangle split (which is the optimum).
        Communities<Integer> expected = new Communities<>();
        expected.addCommunity(); expected.add(1, 0); expected.add(2, 0); expected.add(3, 0);
        expected.addCommunity(); expected.add(4, 1); expected.add(5, 1); expected.add(6, 1);

        Modularity<Integer> mod = new Modularity<>();
        Assert.assertEquals(mod.compute(graph, expected), mod.compute(graph, comms), 1e-9);
    }

    /**
     * A ring of four triangles, each pair joined by a single edge. This mainly guards against the non-termination the
     * broken gain caused: the test asserts Louvain terminates, does not collapse everything into one community, and
     * finds a partition strictly better than the fully-fragmented (all-singletons) one. The exact community count is
     * left loose, since Louvain only guarantees a good local optimum, not a specific one. (We deliberately don't compare
     * against the single-community partition: RELISON's modularity is normalized, and that partition makes its
     * denominator zero, so it evaluates to NaN.)
     */
    @Test
    public void ringOfTriangles()
    {
        UndirectedGraph<Integer> graph = new FastUndirectedUnweightedGraph<>();
        for (int i = 0; i < 12; ++i) graph.addNode(i);
        for (int t = 0; t < 4; ++t)   // four triangles: {0,1,2}, {3,4,5}, {6,7,8}, {9,10,11}
        {
            int a = 3 * t;
            graph.addEdge(a, a + 1); graph.addEdge(a, a + 2); graph.addEdge(a + 1, a + 2);
        }
        // Ring bridges between consecutive triangles.
        graph.addEdge(2, 3); graph.addEdge(5, 6); graph.addEdge(8, 9); graph.addEdge(11, 0);

        Communities<Integer> comms = new Louvain<Integer>(0, 0.0001).detectCommunities(graph);
        Assert.assertNotNull(comms);
        Assert.assertTrue("Louvain collapsed everything into one community", comms.getNumCommunities() > 1);

        double q = new Modularity<Integer>().compute(graph, comms);

        Communities<Integer> singletons = new Communities<>();
        for (int i = 0; i < 12; ++i) { singletons.addCommunity(); singletons.add(i, i); }

        Modularity<Integer> mod = new Modularity<>();
        Assert.assertTrue("Louvain must beat the all-singletons partition", q > mod.compute(graph, singletons));
    }
}
