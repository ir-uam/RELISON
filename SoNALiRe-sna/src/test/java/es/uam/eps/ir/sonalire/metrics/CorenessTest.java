/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics;


import es.uam.eps.ir.sonalire.graph.DirectedGraph;
import es.uam.eps.ir.sonalire.graph.UndirectedGraph;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.sonalire.graph.fast.FastUndirectedUnweightedGraph;
import es.uam.eps.ir.sonalire.graph.multigraph.fast.FastUndirectedUnweightedMultiGraph;
import es.uam.eps.ir.sonalire.metrics.vertex.Coreness;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.junit.*;

import java.util.Map;

/**
 * Automated unit tests for the coreness metric.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CorenessTest
{
    /**
     * Directed complete graph
     */
    private DirectedGraph<Integer> directedComplete;
    /**
     * Directed complete graph
     */
    private UndirectedGraph<Integer> undirectedComplete;

    /**
     * Directed empty graph (only nodes)
     */
    private DirectedGraph<Integer> directedEmpty;
    /**
     * Undirected graph
     */
    private UndirectedGraph<Integer> undirected;
    /**
     * Directed graph
     */
    private DirectedGraph<Integer> directed;
    /**
     * Undirected empty graph
     */
    private UndirectedGraph<Integer> undirectedEmpty;
    /**
     * Undirected multi graph.
     */
    private UndirectedGraph<Integer> undirectedMultiGraph;

    public CorenessTest()
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
        // Directed Empty Graph (only nodes)
        this.directedEmpty = new FastDirectedUnweightedGraph<>();
        this.directedEmpty.addNode(1);
        this.directedEmpty.addNode(2);
        this.directedEmpty.addNode(3);

        // Undirected Empty Graph (only nodes)
        this.undirectedEmpty = new FastUndirectedUnweightedGraph<>();
        this.undirectedEmpty.addNode(1);
        this.undirectedEmpty.addNode(2);
        this.undirectedEmpty.addNode(3);

        // Directed Complete Graph
        this.directedComplete = new FastDirectedUnweightedGraph<>();
        this.directedComplete.addNode(1);
        this.directedComplete.addNode(2);
        this.directedComplete.addNode(3);
        this.directedComplete.addNode(4);
        this.directedComplete.addEdge(1, 2);
        this.directedComplete.addEdge(1, 3);
        this.directedComplete.addEdge(1, 4);
        this.directedComplete.addEdge(2, 1);
        this.directedComplete.addEdge(2, 3);
        this.directedComplete.addEdge(2, 4);
        this.directedComplete.addEdge(3, 1);
        this.directedComplete.addEdge(3, 2);
        this.directedComplete.addEdge(3, 4);
        this.directedComplete.addEdge(4, 1);
        this.directedComplete.addEdge(4, 2);
        this.directedComplete.addEdge(4, 3);

        // Directed Complete Graph
        this.undirectedComplete = new FastUndirectedUnweightedGraph<>();
        this.undirectedComplete.addNode(1);
        this.undirectedComplete.addNode(2);
        this.undirectedComplete.addNode(3);
        this.undirectedComplete.addNode(4);
        this.undirectedComplete.addEdge(1, 2);
        this.undirectedComplete.addEdge(1, 3);
        this.undirectedComplete.addEdge(1, 4);
        this.undirectedComplete.addEdge(2, 3);
        this.undirectedComplete.addEdge(2, 4);
        this.undirectedComplete.addEdge(3, 4);

        // Undirected graph
        this.undirected = new FastUndirectedUnweightedGraph<>();
        this.undirected.addNode(0);
        this.undirected.addNode(1);
        this.undirected.addNode(2);
        this.undirected.addNode(3);
        this.undirected.addNode(4);
        this.undirected.addNode(5);
        this.undirected.addNode(6);
        this.undirected.addNode(7);
        this.undirected.addNode(8);
        this.undirected.addNode(10);
        this.undirected.addNode(11);
        this.undirected.addNode(12);
        this.undirected.addNode(13);
        this.undirected.addNode(14);
        this.undirected.addNode(15);
        this.undirected.addNode(16);
        this.undirected.addNode(17);
        this.undirected.addNode(18);
        this.undirected.addNode(19);
        this.undirected.addNode(20);
        this.undirected.addNode(21);

        this.undirected.addEdge(0, 2);
        this.undirected.addEdge(1, 2);
        this.undirected.addEdge(2, 3);
        this.undirected.addEdge(2, 4);
        this.undirected.addEdge(3, 4);
        this.undirected.addEdge(3, 5);
        this.undirected.addEdge(4, 10);
        this.undirected.addEdge(4, 11);
        this.undirected.addEdge(4, 12);
        this.undirected.addEdge(5, 6);
        this.undirected.addEdge(5, 7);
        this.undirected.addEdge(5, 8);
        this.undirected.addEdge(6, 7);
        this.undirected.addEdge(6, 8);
        this.undirected.addEdge(7, 8);

        this.undirected.addEdge(7, 10);
        this.undirected.addEdge(7, 13);
        this.undirected.addEdge(10, 11);
        this.undirected.addEdge(10, 12);
        this.undirected.addEdge(10, 13);
        this.undirected.addEdge(10, 14);
        this.undirected.addEdge(11, 12);
        this.undirected.addEdge(13, 14);
        this.undirected.addEdge(13, 15);
        this.undirected.addEdge(16, 17);
        this.undirected.addEdge(17, 18);
        this.undirected.addEdge(17, 19);
        this.undirected.addEdge(18, 20);
        this.undirected.addEdge(19, 20);


        // Undirected multi-graph
        this.undirectedMultiGraph = new FastUndirectedUnweightedMultiGraph<>();
        this.undirectedMultiGraph.addNode(0);
        this.undirectedMultiGraph.addNode(1);
        this.undirectedMultiGraph.addNode(2);
        this.undirectedMultiGraph.addNode(3);
        this.undirectedMultiGraph.addNode(4);
        this.undirectedMultiGraph.addNode(5);
        this.undirectedMultiGraph.addNode(6);
        this.undirectedMultiGraph.addNode(7);
        this.undirectedMultiGraph.addNode(8);
        this.undirectedMultiGraph.addNode(10);
        this.undirectedMultiGraph.addNode(11);
        this.undirectedMultiGraph.addNode(12);
        this.undirectedMultiGraph.addNode(13);
        this.undirectedMultiGraph.addNode(14);
        this.undirectedMultiGraph.addNode(15);
        this.undirectedMultiGraph.addNode(16);
        this.undirectedMultiGraph.addNode(17);
        this.undirectedMultiGraph.addNode(18);
        this.undirectedMultiGraph.addNode(19);
        this.undirectedMultiGraph.addNode(20);
        this.undirectedMultiGraph.addNode(21);

        this.undirectedMultiGraph.addEdge(0, 2);
        this.undirectedMultiGraph.addEdge(1, 2);
        this.undirectedMultiGraph.addEdge(2, 3);
        this.undirectedMultiGraph.addEdge(2, 4);
        this.undirectedMultiGraph.addEdge(3, 4);
        this.undirectedMultiGraph.addEdge(3, 5);
        this.undirectedMultiGraph.addEdge(4, 10);
        this.undirectedMultiGraph.addEdge(4, 11);
        this.undirectedMultiGraph.addEdge(4, 12);
        this.undirectedMultiGraph.addEdge(5, 6);
        this.undirectedMultiGraph.addEdge(5, 7);
        this.undirectedMultiGraph.addEdge(5, 8);
        this.undirectedMultiGraph.addEdge(6, 7);
        this.undirectedMultiGraph.addEdge(6, 8);
        this.undirectedMultiGraph.addEdge(7, 8);

        this.undirectedMultiGraph.addEdge(7, 10);
        this.undirectedMultiGraph.addEdge(7, 13);
        this.undirectedMultiGraph.addEdge(10, 11);
        this.undirectedMultiGraph.addEdge(10, 12);
        this.undirectedMultiGraph.addEdge(10, 13);
        this.undirectedMultiGraph.addEdge(10, 14);
        this.undirectedMultiGraph.addEdge(11, 12);
        this.undirectedMultiGraph.addEdge(13, 14);
        this.undirectedMultiGraph.addEdge(13, 15);
        this.undirectedMultiGraph.addEdge(16, 17);
        this.undirectedMultiGraph.addEdge(17, 18);
        this.undirectedMultiGraph.addEdge(17, 19);
        this.undirectedMultiGraph.addEdge(18, 20);
        this.undirectedMultiGraph.addEdge(19, 20);
        this.undirectedMultiGraph.addEdge(0, 2);
        this.undirectedMultiGraph.addEdge(1, 2);
        this.undirectedMultiGraph.addEdge(2, 3);
        this.undirectedMultiGraph.addEdge(2, 4);
        this.undirectedMultiGraph.addEdge(3, 4);
        this.undirectedMultiGraph.addEdge(3, 5);
        this.undirectedMultiGraph.addEdge(4, 10);
        this.undirectedMultiGraph.addEdge(4, 11);
        this.undirectedMultiGraph.addEdge(4, 12);
        this.undirectedMultiGraph.addEdge(5, 6);
        this.undirectedMultiGraph.addEdge(5, 7);
        this.undirectedMultiGraph.addEdge(5, 8);
        this.undirectedMultiGraph.addEdge(6, 7);
        this.undirectedMultiGraph.addEdge(6, 8);
        this.undirectedMultiGraph.addEdge(7, 8);

        this.undirectedMultiGraph.addEdge(7, 10);
        this.undirectedMultiGraph.addEdge(7, 13);
        this.undirectedMultiGraph.addEdge(10, 11);
        this.undirectedMultiGraph.addEdge(10, 12);
        this.undirectedMultiGraph.addEdge(10, 13);
        this.undirectedMultiGraph.addEdge(10, 14);
        this.undirectedMultiGraph.addEdge(11, 12);
        this.undirectedMultiGraph.addEdge(13, 14);
        this.undirectedMultiGraph.addEdge(13, 15);
        this.undirectedMultiGraph.addEdge(16, 17);
        this.undirectedMultiGraph.addEdge(17, 18);
        this.undirectedMultiGraph.addEdge(17, 19);
        this.undirectedMultiGraph.addEdge(18, 20);
        this.undirectedMultiGraph.addEdge(19, 20);

        // Directed graph
        this.directed = new FastDirectedUnweightedGraph<>();
        this.directed.addNode(0);
        this.directed.addNode(1);
        this.directed.addNode(2);
        this.directed.addNode(3);
        this.directed.addNode(4);
        this.directed.addNode(5);
        this.directed.addNode(6);
        this.directed.addNode(7);
        this.directed.addNode(8);
        this.directed.addNode(10);
        this.directed.addNode(11);
        this.directed.addNode(12);
        this.directed.addNode(13);
        this.directed.addNode(14);
        this.directed.addNode(15);
        this.directed.addNode(16);
        this.directed.addNode(17);
        this.directed.addNode(18);
        this.directed.addNode(19);
        this.directed.addNode(20);
        this.directed.addNode(21);

        this.directed.addEdge(0, 2);
        this.directed.addEdge(1, 2);
        this.directed.addEdge(2, 3);
        this.directed.addEdge(2, 4);
        this.directed.addEdge(3, 4);
        this.directed.addEdge(3, 5);
        this.directed.addEdge(4, 10);
        this.directed.addEdge(4, 11);
        this.directed.addEdge(4, 12);
        this.directed.addEdge(5, 6);
        this.directed.addEdge(5, 7);
        this.directed.addEdge(5, 8);
        this.directed.addEdge(6, 7);
        this.directed.addEdge(6, 8);
        this.directed.addEdge(7, 8);
        this.directed.addEdge(7, 10);
        this.directed.addEdge(7, 13);
        this.directed.addEdge(10, 11);
        this.directed.addEdge(10, 12);
        this.directed.addEdge(10, 13);
        this.directed.addEdge(10, 14);
        this.directed.addEdge(11, 12);
        this.directed.addEdge(13, 14);
        this.directed.addEdge(13, 15);
        this.directed.addEdge(16, 17);
        this.directed.addEdge(17, 18);
        this.directed.addEdge(17, 19);
        this.directed.addEdge(18, 20);
        this.directed.addEdge(19, 20);

        this.directed.addEdge(2, 0);
        this.directed.addEdge(2, 1);
        this.directed.addEdge(3, 2);
        this.directed.addEdge(4, 2);
        this.directed.addEdge(4, 3);
        this.directed.addEdge(5, 3);
        this.directed.addEdge(10, 4);
        this.directed.addEdge(11, 4);
        this.directed.addEdge(12, 4);
        this.directed.addEdge(6, 5);
        this.directed.addEdge(7, 5);
        this.directed.addEdge(8, 5);
        this.directed.addEdge(7, 6);
        this.directed.addEdge(8, 6);
        this.directed.addEdge(8, 7);

        this.directed.addEdge(10, 7);
        this.directed.addEdge(13, 7);
        this.directed.addEdge(11, 10);
        this.directed.addEdge(12, 10);
        this.directed.addEdge(13, 10);
        this.directed.addEdge(14, 10);
        this.directed.addEdge(12, 11);
        this.directed.addEdge(14, 13);
        this.directed.addEdge(15, 13);
        this.directed.addEdge(17, 16);
        this.directed.addEdge(18, 17);
        this.directed.addEdge(19, 17);
        this.directed.addEdge(20, 18);
        this.directed.addEdge(20, 19);
    }

    @After
    public void tearDown()
    {
    }


    /**
     * Checks the coreness for a group of empty networks.
     */
    @Test
    public void empty()
    {
        VertexMetric<Integer> inMetric = new Coreness<>(EdgeOrientation.IN);
        Map<Integer, Double> inValues = inMetric.compute(directedEmpty);
        for(int user : inValues.keySet())
        {
            Assert.assertEquals(0, inValues.get(user).intValue());
        }

        VertexMetric<Integer> outMetric = new Coreness<>(EdgeOrientation.OUT);
        Map<Integer, Double> outValues = outMetric.compute(directedEmpty);
        for(int user : outValues.keySet())
        {
            Assert.assertEquals(0, outValues.get(user).intValue());
        }

        VertexMetric<Integer> undMetric = new Coreness<>(EdgeOrientation.UND);
        Map<Integer, Double> undValues = undMetric.compute(directedEmpty);
        for(int user : undValues.keySet())
        {
            Assert.assertEquals(0, undValues.get(user).intValue());
        }

        undValues = undMetric.compute(undirectedEmpty);
        for(int user : undValues.keySet())
        {
            Assert.assertEquals(0, undValues.get(user).intValue());
        }
    }

    /**
     * Checks the coreness for a group of complete networks.
     */
    @Test
    public void complete()
    {
        VertexMetric<Integer> inMetric = new Coreness<>(EdgeOrientation.IN);
        Map<Integer, Double> inValues = inMetric.compute(directedComplete);
        for(int user : inValues.keySet())
        {
            Assert.assertEquals(directedComplete.getVertexCount()-1, inValues.get(user).intValue());
        }

        VertexMetric<Integer> outMetric = new Coreness<>(EdgeOrientation.OUT);
        Map<Integer, Double> outValues = outMetric.compute(directedComplete);
        for(int user : outValues.keySet())
        {
            Assert.assertEquals(directedComplete.getVertexCount()-1, outValues.get(user).intValue());
        }

        VertexMetric<Integer> undMetric = new Coreness<>(EdgeOrientation.UND);
        Map<Integer, Double> undValues = undMetric.compute(directedComplete);
        for(int user : undValues.keySet())
        {
            Assert.assertEquals(2*(directedComplete.getVertexCount()-1), undValues.get(user).intValue());
        }

        undValues = undMetric.compute(undirectedComplete);
        for(int user : undValues.keySet())
        {
            Assert.assertEquals(undirectedComplete.getVertexCount()-1, undValues.get(user).intValue());
        }
    }

    /**
     * Checks the coreness for an undirected network.
     */
    @Test
    public void undirected()
    {
        VertexMetric<Integer> inMetric = new Coreness<>(EdgeOrientation.IN);
        Map<Integer, Double> inValues = inMetric.compute(undirected);

        Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
        counter.defaultReturnValue(0);
        for(int user : inValues.keySet())
        {
            int core = inValues.get(user).intValue();
            counter.addTo(core, 1);
        }

        Assert.assertEquals(1, counter.get(0));
        Assert.assertEquals(4, counter.get(1));
        Assert.assertEquals(8, counter.get(2));
        Assert.assertEquals(8, counter.get(3));
    }

    /**
     * Checks the coreness of an undirected multi-graph
     */
    @Test
    public void undirectedMultigraph()
    {
        VertexMetric<Integer> inMetric = new Coreness<>(EdgeOrientation.IN);
        Map<Integer, Double> inValues = inMetric.compute(undirectedMultiGraph);

        Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
        counter.defaultReturnValue(0);
        for(int user : inValues.keySet())
        {
            int core = inValues.get(user).intValue();
            counter.addTo(core, 1);
        }

        Assert.assertEquals(1, counter.get(0));
        Assert.assertEquals(0, counter.get(1));
        Assert.assertEquals(4, counter.get(2));
        Assert.assertEquals(0, counter.get(3));
        Assert.assertEquals(8, counter.get(4));
        Assert.assertEquals(0, counter.get(5));
        Assert.assertEquals(8, counter.get(6));
    }

    /**
     * Checks the different variants of the coreness for a directed network.
     */
    @Test
    public void directed()
    {
        VertexMetric<Integer> inMetric = new Coreness<>(EdgeOrientation.IN);
        Map<Integer, Double> inValues = inMetric.compute(directed);

        Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
        counter.defaultReturnValue(0);
        for(int user : inValues.keySet())
        {
            int core = inValues.get(user).intValue();
            counter.addTo(core, 1);
        }

        Assert.assertEquals(1, counter.get(0));
        Assert.assertEquals(4, counter.get(1));
        Assert.assertEquals(8, counter.get(2));
        Assert.assertEquals(8, counter.get(3));

        VertexMetric<Integer> outMetric = new Coreness<>(EdgeOrientation.OUT);
        Map<Integer, Double> outValues = outMetric.compute(directed);
        counter.clear();
        for(int user : outValues.keySet())
        {
            int core = outValues.get(user).intValue();
            counter.addTo(core, 1);
        }

        Assert.assertEquals(1, counter.get(0));
        Assert.assertEquals(4, counter.get(1));
        Assert.assertEquals(8, counter.get(2));
        Assert.assertEquals(8, counter.get(3));

        VertexMetric<Integer> undMetric = new Coreness<>(EdgeOrientation.UND);
        Map<Integer, Double> undValues = undMetric.compute(directed);
        counter.clear();
        for(int user : undValues.keySet())
        {
            int core = undValues.get(user).intValue();
            counter.addTo(core, 1);
        }

        Assert.assertEquals(1, counter.get(0));
        Assert.assertEquals(0, counter.get(1));
        Assert.assertEquals(4, counter.get(2));
        Assert.assertEquals(0, counter.get(3));
        Assert.assertEquals(8, counter.get(4));
        Assert.assertEquals(0, counter.get(5));
        Assert.assertEquals(8, counter.get(6));
    }
}
