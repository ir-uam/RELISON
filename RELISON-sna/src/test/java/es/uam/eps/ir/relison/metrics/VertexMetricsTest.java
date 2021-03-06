/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics;

import es.uam.eps.ir.relison.graph.DirectedGraph;
import es.uam.eps.ir.relison.graph.UndirectedGraph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedUnweightedGraph;
import es.uam.eps.ir.relison.sna.metrics.VertexMetric;
import es.uam.eps.ir.relison.sna.metrics.vertex.Degree;
import es.uam.eps.ir.relison.sna.metrics.vertex.InverseDegree;
import es.uam.eps.ir.relison.sna.metrics.vertex.LocalClusteringCoefficient;
import org.junit.*;

/**
 * Automated unit tests for the vertex metrics.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class VertexMetricsTest
{
    /**
     * Directed strongly connected graph.
     */
    private DirectedGraph<Integer> directedStronglyConnected;
    /**
     * Directed weakly connected graph.
     */
    private DirectedGraph<Integer> directedWeaklyConnected;
    /**
     * Directed disconnected graph (not even weakly).
     */
    private DirectedGraph<Integer> directedNonConnected;
    /**
     * Undirected connected graph.
     */
    private UndirectedGraph<Integer> undirectedConnected;
    /**
     * Undirected disconnected graph.
     */
    private UndirectedGraph<Integer> undirectedNonConnected;
    /**
     * Directed complete graph.
     */
    private DirectedGraph<Integer> directedComplete;
    /**
     * Directed empty graph (only nodes).
     */
    private DirectedGraph<Integer> directedEmpty;
    /**
     * Undirected complete graph.
     */
    private UndirectedGraph<Integer> undirectedComplete;
    /**
     * Undirected empty graph.
     */
    private UndirectedGraph<Integer> undirectedEmpty;

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

        // Undirected Complete Graph
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

        // Directed Strongly Connected
        this.directedStronglyConnected = new FastDirectedUnweightedGraph<>();
        this.directedStronglyConnected.addNode(1);
        this.directedStronglyConnected.addNode(2);
        this.directedStronglyConnected.addNode(3);
        this.directedStronglyConnected.addNode(4);
        this.directedStronglyConnected.addNode(5);
        this.directedStronglyConnected.addNode(6);
        this.directedStronglyConnected.addNode(7);
        this.directedStronglyConnected.addEdge(1, 2);
        this.directedStronglyConnected.addEdge(1, 3);
        this.directedStronglyConnected.addEdge(2, 4);
        this.directedStronglyConnected.addEdge(3, 4);
        this.directedStronglyConnected.addEdge(3, 5);
        this.directedStronglyConnected.addEdge(3, 6);
        this.directedStronglyConnected.addEdge(4, 6);
        this.directedStronglyConnected.addEdge(5, 6);
        this.directedStronglyConnected.addEdge(6, 1);
        this.directedStronglyConnected.addEdge(6, 7);
        this.directedStronglyConnected.addEdge(7, 2);

        // Directed Weakly Connected
        this.directedWeaklyConnected = new FastDirectedUnweightedGraph<>();
        this.directedWeaklyConnected.addNode(1);
        this.directedWeaklyConnected.addNode(2);
        this.directedWeaklyConnected.addNode(3);
        this.directedWeaklyConnected.addNode(4);
        this.directedWeaklyConnected.addNode(5);
        this.directedWeaklyConnected.addNode(6);
        this.directedWeaklyConnected.addNode(7);
        this.directedWeaklyConnected.addEdge(1, 2);
        this.directedWeaklyConnected.addEdge(1, 3);
        this.directedWeaklyConnected.addEdge(2, 4);
        this.directedWeaklyConnected.addEdge(3, 4);
        this.directedWeaklyConnected.addEdge(3, 5);
        this.directedWeaklyConnected.addEdge(4, 6);
        this.directedWeaklyConnected.addEdge(5, 6);
        this.directedWeaklyConnected.addEdge(7, 2);

        // Directed Non Connected
        this.directedNonConnected = new FastDirectedUnweightedGraph<>();
        this.directedNonConnected.addNode(1);
        this.directedNonConnected.addNode(2);
        this.directedNonConnected.addNode(3);
        this.directedNonConnected.addNode(4);
        this.directedNonConnected.addNode(5);
        this.directedNonConnected.addNode(6);
        this.directedNonConnected.addEdge(1, 2);
        this.directedNonConnected.addEdge(3, 1);
        this.directedNonConnected.addEdge(3, 2);
        this.directedNonConnected.addEdge(4, 5);
        this.directedNonConnected.addEdge(5, 6);
        this.directedNonConnected.addEdge(6, 4);


        // Undirected Connected
        this.undirectedConnected = new FastUndirectedUnweightedGraph<>();
        this.undirectedConnected.addNode(1);
        this.undirectedConnected.addNode(2);
        this.undirectedConnected.addNode(3);
        this.undirectedConnected.addNode(4);
        this.undirectedConnected.addNode(5);
        this.undirectedConnected.addNode(6);
        this.undirectedConnected.addEdge(1, 2);
        this.undirectedConnected.addEdge(1, 4);
        this.undirectedConnected.addEdge(1, 6);
        this.undirectedConnected.addEdge(2, 3);
        this.undirectedConnected.addEdge(3, 4);
        this.undirectedConnected.addEdge(4, 5);
        this.undirectedConnected.addEdge(4, 6);
        this.undirectedConnected.addEdge(5, 6);

        // Undirected Non Connected
        this.undirectedNonConnected = new FastUndirectedUnweightedGraph<>();
        this.undirectedNonConnected.addNode(1);
        this.undirectedNonConnected.addNode(2);
        this.undirectedNonConnected.addNode(3);
        this.undirectedNonConnected.addNode(4);
        this.undirectedNonConnected.addNode(5);
        this.undirectedNonConnected.addNode(6);
        this.undirectedNonConnected.addEdge(1, 2);
        this.undirectedNonConnected.addEdge(1, 3);
        this.undirectedNonConnected.addEdge(2, 3);
        this.undirectedNonConnected.addEdge(4, 5);
        this.undirectedNonConnected.addEdge(4, 6);
        this.undirectedNonConnected.addEdge(5, 6);
    }

    @After
    public void tearDown()
    {
    }


    /**
     * Tets the degree of the nodes.
     */
    @Test
    public void degree()
    {
        VertexMetric<Integer> inMetric = new Degree<>(EdgeOrientation.IN);

        Assert.assertEquals(0.0, inMetric.averageValue(directedEmpty), 0.001);
        Assert.assertEquals(0.0, inMetric.averageValue(undirectedEmpty), 0.001);
        Assert.assertEquals(3.0, inMetric.averageValue(directedComplete), 0.001);
        Assert.assertEquals(3.0, inMetric.averageValue(undirectedComplete), 0.001);
        Assert.assertEquals(1.142857143, inMetric.averageValue(directedWeaklyConnected), 0.001);
        Assert.assertEquals(1.571428571, inMetric.averageValue(directedStronglyConnected), 0.001);
        Assert.assertEquals(1.0, inMetric.averageValue(directedNonConnected), 0.001);
        Assert.assertEquals(2.66666667, inMetric.averageValue(undirectedConnected), 0.001);
        Assert.assertEquals(2.0, inMetric.averageValue(undirectedNonConnected), 0.001);
        Assert.assertEquals(0.0, inMetric.averageValue(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(0.0, inMetric.averageValue(new FastUndirectedUnweightedGraph<>()), 0.001);

        VertexMetric<Integer> outMetric = new Degree<>(EdgeOrientation.OUT);
        // Out-Gini
        Assert.assertEquals(inMetric.averageValue(directedEmpty), outMetric.averageValue(directedEmpty), 0.001);
        Assert.assertEquals(inMetric.averageValue(undirectedEmpty), outMetric.averageValue(undirectedEmpty), 0.001);
        Assert.assertEquals(inMetric.averageValue(directedComplete), outMetric.averageValue(directedComplete), 0.001);
        Assert.assertEquals(inMetric.averageValue(undirectedComplete), outMetric.averageValue(undirectedComplete), 0.001);
        Assert.assertEquals(inMetric.averageValue(directedWeaklyConnected), outMetric.averageValue(directedWeaklyConnected), 0.001);
        Assert.assertEquals(inMetric.averageValue(directedStronglyConnected), outMetric.averageValue(directedStronglyConnected), 0.001);
        Assert.assertEquals(inMetric.averageValue(directedNonConnected), outMetric.averageValue(directedNonConnected), 0.001);
        Assert.assertEquals(inMetric.averageValue(undirectedConnected), inMetric.averageValue(undirectedConnected), 0.001);
        Assert.assertEquals(inMetric.averageValue(undirectedNonConnected), outMetric.averageValue(undirectedNonConnected), 0.001);
        Assert.assertEquals(inMetric.averageValue(new FastDirectedUnweightedGraph<>()), outMetric.averageValue(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(inMetric.averageValue(new FastUndirectedUnweightedGraph<>()), outMetric.averageValue(new FastUndirectedUnweightedGraph<>()), 0.001);
    }

    /**
     * Tests the inverse degree of the nodes.
     */
    @Test
    public void inverseDegree()
    {
        VertexMetric<Integer> inMetric = new InverseDegree<>(EdgeOrientation.IN);

        Assert.assertEquals(1.0, inMetric.averageValue(directedEmpty), 0.001);
        Assert.assertEquals(1.0, inMetric.averageValue(undirectedEmpty), 0.001);
        Assert.assertEquals(0.25, inMetric.averageValue(directedComplete), 0.001);
        Assert.assertEquals(0.25, inMetric.averageValue(undirectedComplete), 0.001);
        Assert.assertEquals(0.571428571, inMetric.averageValue(directedWeaklyConnected), 0.001);
        Assert.assertEquals(0.416666667, inMetric.averageValue(directedStronglyConnected), 0.001);
        Assert.assertEquals(0.555555556, inMetric.averageValue(directedNonConnected), 0.001);
        Assert.assertEquals(0.2833333, inMetric.averageValue(undirectedConnected), 0.001);
        Assert.assertEquals(0.333333, inMetric.averageValue(undirectedNonConnected), 0.001);
        Assert.assertEquals(0.0, inMetric.averageValue(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(0.0, inMetric.averageValue(new FastUndirectedUnweightedGraph<>()), 0.001);

        VertexMetric<Integer> outMetric = new InverseDegree<>(EdgeOrientation.OUT);
        // Out-Gini
        Assert.assertEquals(1.0, outMetric.averageValue(directedEmpty), 0.001);
        Assert.assertEquals(1.0, outMetric.averageValue(undirectedEmpty), 0.001);
        Assert.assertEquals(0.25, outMetric.averageValue(directedComplete), 0.001);
        Assert.assertEquals(0.25, outMetric.averageValue(undirectedComplete), 0.001);
        Assert.assertEquals(0.523809524, outMetric.averageValue(directedWeaklyConnected), 0.001);
        Assert.assertEquals(0.416666667, outMetric.averageValue(directedStronglyConnected), 0.001);
        Assert.assertEquals(0.555555556, outMetric.averageValue(directedNonConnected), 0.001);
        Assert.assertEquals(0.28333333, outMetric.averageValue(undirectedConnected), 0.001);
        Assert.assertEquals(0.333333, outMetric.averageValue(undirectedNonConnected), 0.001);
        Assert.assertEquals(0.0, outMetric.averageValue(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(0.0, outMetric.averageValue(new FastUndirectedUnweightedGraph<>()), 0.001);

    }

    /**
     * Tests the local clustering coefficient of the nodes.
     */
    @Test
    public void localClustCoef()
    {
        VertexMetric<Integer> inMetric = new LocalClusteringCoefficient<>();

        Assert.assertEquals(0.0, inMetric.averageValue(directedEmpty), 0.001);
        Assert.assertEquals(0.0, inMetric.averageValue(undirectedEmpty), 0.001);
        Assert.assertEquals(1.0, inMetric.averageValue(directedComplete), 0.001);
        Assert.assertEquals(1.0, inMetric.averageValue(undirectedComplete), 0.001);
        Assert.assertEquals(0.0, inMetric.averageValue(directedWeaklyConnected), 0.001);
        Assert.assertEquals(1.5 / 7.0, inMetric.averageValue(directedStronglyConnected), 0.001);
        Assert.assertEquals(1.0 / 6.0, inMetric.averageValue(directedNonConnected), 0.001);
        Assert.assertEquals(0.3888888888, inMetric.averageValue(undirectedConnected), 0.001);
        Assert.assertEquals(1.0, inMetric.averageValue(undirectedNonConnected), 0.001);
        Assert.assertEquals(0.0, inMetric.averageValue(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(0.0, inMetric.averageValue(new FastUndirectedUnweightedGraph<>()), 0.001);
    }
}
