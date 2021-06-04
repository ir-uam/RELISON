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
import es.uam.eps.ir.relison.metrics.graph.*;
import org.junit.*;

import static java.lang.Double.NaN;

/**
 * Automated unit tests for metrics that affect the whole network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class GraphMetricsTest
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

    public GraphMetricsTest()
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
     * Tests the values of clustering coefficient
     */
    @Test
    public void clusteringCoefficient()
    {
        GraphMetric<Integer> metric = new ClusteringCoefficient<>();

        Assert.assertEquals(0.0, metric.compute(directedEmpty), 0.001);
        Assert.assertEquals(0.0, metric.compute(undirectedEmpty), 0.001);
        Assert.assertEquals(1.0, metric.compute(directedComplete), 0.001);
        Assert.assertEquals(1.0, metric.compute(undirectedComplete), 0.001);
        Assert.assertEquals(2.0 / 17.0, metric.compute(directedStronglyConnected), 0.001);
        Assert.assertEquals(0.0, metric.compute(directedWeaklyConnected), 0.001);
        Assert.assertEquals(0.25, metric.compute(directedNonConnected), 0.001);
        Assert.assertEquals(0.4, metric.compute(undirectedConnected), 0.001);
        Assert.assertEquals(1.0, metric.compute(undirectedNonConnected), 0.001);
        Assert.assertEquals(0.0, metric.compute(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(0.0, metric.compute(new FastUndirectedUnweightedGraph<>()), 0.001);
    }

    /**
     * Tests the values of the Gini index for the degree.
     */
    @Test
    public void degreeGini()
    {
        GraphMetric<Integer> inMetric = new DegreeGini<>(EdgeOrientation.IN);

        // In-Gini
        Assert.assertEquals(1.0, inMetric.compute(directedEmpty), 0.001);
        Assert.assertEquals(1.0, inMetric.compute(undirectedEmpty), 0.001);
        Assert.assertEquals(1.0, inMetric.compute(directedComplete), 0.001);
        Assert.assertEquals(1.0, inMetric.compute(undirectedComplete), 0.001);
        Assert.assertEquals(0.541666667, inMetric.compute(directedWeaklyConnected), 0.001);
        Assert.assertEquals(0.7272727272, inMetric.compute(directedStronglyConnected), 0.001);
        Assert.assertEquals(0.6666666667, inMetric.compute(directedNonConnected), 0.001);
        Assert.assertEquals(0.825, inMetric.compute(undirectedConnected), 0.001);
        Assert.assertEquals(1.0, inMetric.compute(undirectedNonConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(new FastUndirectedUnweightedGraph<>()), 0.001);

        GraphMetric<Integer> outMetric = new DegreeGini<>(EdgeOrientation.OUT);
        // Out-Gini
        Assert.assertEquals(1.0, outMetric.compute(directedEmpty), 0.001);
        Assert.assertEquals(1.0, outMetric.compute(undirectedEmpty), 0.001);
        Assert.assertEquals(1.0, outMetric.compute(directedComplete), 0.001);
        Assert.assertEquals(1.0, outMetric.compute(undirectedComplete), 0.001);
        Assert.assertEquals(0.6666666667, outMetric.compute(directedWeaklyConnected), 0.001);
        Assert.assertEquals(0.7272727272, outMetric.compute(directedStronglyConnected), 0.001);
        Assert.assertEquals(0.6666666667, outMetric.compute(directedNonConnected), 0.001);
        Assert.assertEquals(0.825, outMetric.compute(undirectedConnected), 0.001);
        Assert.assertEquals(1.0, outMetric.compute(undirectedNonConnected), 0.001);
        Assert.assertEquals(NaN, outMetric.compute(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(NaN, outMetric.compute(new FastUndirectedUnweightedGraph<>()), 0.001);
    }

    /**
     * Tests the density values of the graphs
     */
    @Test
    public void density()
    {
        GraphMetric<Integer> metric = new Density<>();

        Assert.assertEquals(0.0, metric.compute(directedEmpty), 0.001);
        Assert.assertEquals(0.0, metric.compute(undirectedEmpty), 0.001);
        Assert.assertEquals(1.0, metric.compute(directedComplete), 0.001);
        Assert.assertEquals(1.0, metric.compute(undirectedComplete), 0.001);
        Assert.assertEquals(11.0 / 42.0, metric.compute(directedStronglyConnected), 0.001);
        Assert.assertEquals(8.0 / 42.0, metric.compute(directedWeaklyConnected), 0.001);
        Assert.assertEquals(0.2, metric.compute(directedNonConnected), 0.001);
        Assert.assertEquals(8.0 / 15.0, metric.compute(undirectedConnected), 0.001);
        Assert.assertEquals(0.4, metric.compute(undirectedNonConnected), 0.001);
        Assert.assertEquals(0.0, metric.compute(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(0.0, metric.compute(new FastUndirectedUnweightedGraph<>()), 0.001);
    }

    /**
     * Tests the pair Gini of the degrees of the graph. Since none of the test graphs are multigraphs,
     * then, the value for all of them is NaN.
     */
    @Test
    public void pairGini()
    {
        GraphMetric<Integer> inMetric = new EdgeGini<>(EdgeGiniMode.INTERLINKS);

        // In-Gini
        Assert.assertEquals(NaN, inMetric.compute(directedEmpty), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(undirectedEmpty), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(directedComplete), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(undirectedComplete), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(directedStronglyConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(directedWeaklyConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(directedNonConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(undirectedConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(undirectedNonConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(new FastUndirectedUnweightedGraph<>()), 0.001);

        inMetric = new EdgeGini<>(EdgeGiniMode.COMPLETE);

        // In-Gini
        Assert.assertEquals(NaN, inMetric.compute(directedEmpty), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(undirectedEmpty), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(directedComplete), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(undirectedComplete), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(directedStronglyConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(directedWeaklyConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(directedNonConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(undirectedConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(undirectedNonConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(new FastUndirectedUnweightedGraph<>()), 0.001);

        inMetric = new EdgeGini<>(EdgeGiniMode.SEMICOMPLETE);

        // In-Gini
        Assert.assertEquals(NaN, inMetric.compute(directedEmpty), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(undirectedEmpty), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(directedComplete), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(undirectedComplete), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(directedStronglyConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(directedWeaklyConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(directedNonConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(undirectedConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(undirectedNonConnected), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(NaN, inMetric.compute(new FastUndirectedUnweightedGraph<>()), 0.001);
    }

    /**
     * Tests the number of edges of the graph
     */
    @Test
    public void numEdges()
    {
        GraphMetric<Integer> inMetric = new NumEdges<>();

        // In-Gini
        Assert.assertEquals(0.0, inMetric.compute(directedEmpty), 0.001);
        Assert.assertEquals(0.0, inMetric.compute(undirectedEmpty), 0.001);
        Assert.assertEquals(12.0, inMetric.compute(directedComplete), 0.001);
        Assert.assertEquals(6.0, inMetric.compute(undirectedComplete), 0.001);
        Assert.assertEquals(11.0, inMetric.compute(directedStronglyConnected), 0.001);
        Assert.assertEquals(8.0, inMetric.compute(directedWeaklyConnected), 0.001);
        Assert.assertEquals(6.0, inMetric.compute(directedNonConnected), 0.001);
        Assert.assertEquals(8.0, inMetric.compute(undirectedConnected), 0.001);
        Assert.assertEquals(6.0, inMetric.compute(undirectedNonConnected), 0.001);
        Assert.assertEquals(0.0, inMetric.compute(new FastDirectedUnweightedGraph<>()), 0.001);
        Assert.assertEquals(0.0, inMetric.compute(new FastUndirectedUnweightedGraph<>()), 0.001);
    }
}
