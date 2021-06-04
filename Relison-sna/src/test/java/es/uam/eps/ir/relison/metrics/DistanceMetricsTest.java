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
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedUnweightedGraph;
import es.uam.eps.ir.relison.metrics.distance.CompleteDistanceCalculator;
import es.uam.eps.ir.relison.metrics.distance.graph.ASL;
import es.uam.eps.ir.relison.metrics.distance.graph.Diameter;
import es.uam.eps.ir.relison.metrics.distance.modes.ASLMode;
import es.uam.eps.ir.relison.metrics.distance.pair.EdgeBetweenness;
import es.uam.eps.ir.relison.metrics.distance.pair.Geodesics;
import es.uam.eps.ir.relison.metrics.distance.vertex.Closeness;
import es.uam.eps.ir.relison.metrics.distance.vertex.Eccentricity;
import es.uam.eps.ir.relison.metrics.distance.vertex.HarmonicCentrality;
import es.uam.eps.ir.relison.metrics.distance.vertex.NodeBetweenness;
import org.junit.*;

/**
 * Automatic unit tests for distance-based metrics.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DistanceMetricsTest
{
    /**
     * Directed strongly connected graph.
     */
    private final DirectedGraph<Integer> directedStronglyConnected;
    /**
     * Directed weakly connected graph.
     */
    private final DirectedGraph<Integer> directedWeaklyConnected;
    /**
     * Directed disconnected graph (not even weakly).
     */
    private final DirectedGraph<Integer> directedNonConnected;
    /**
     * Undirected connected graph.
     */
    private final UndirectedGraph<Integer> undirectedConnected;
    /**
     * Undirected disconnected graph.
     */
    private final UndirectedGraph<Integer> undirectedNonConnected;
    /**
     * Directed complete graph.
     */
    private final DirectedGraph<Integer> directedComplete;
    /**
     * Directed empty graph (only nodes).
     */
    private final DirectedGraph<Integer> directedEmpty;
    /**
     * Undirected complete graph.
     */
    private final UndirectedGraph<Integer> undirectedComplete;
    /**
     * Undirected empty graph.
     */
    private final UndirectedGraph<Integer> undirectedEmpty;

    /**
     * Distance calculator for the directed and strongly connected network.
     */
    private final CompleteDistanceCalculator<Integer> dscCalculator;
    /**
     * Distance calculator for the directed and weakly connected network.
     */
    private final CompleteDistanceCalculator<Integer> dwcCalculator;
    /**
     * Distance calculator for the directed and disconnected connected network.
     */
    private final CompleteDistanceCalculator<Integer> dncCalculator;
    /**
     * Distance calculator for the undirected and connected network.
     */
    private final CompleteDistanceCalculator<Integer> uscCalculator;
    /**
     * Distance calculator for the undirected and disconnected network.
     */
    private final CompleteDistanceCalculator<Integer> uncCalculator;
    /**
     * Distance calculator for the directed complete network.
     */
    private final CompleteDistanceCalculator<Integer> dcCalculator;
    /**
     * Distance calculator for the undirected complete network.
     */
    private final CompleteDistanceCalculator<Integer> ucCalculator;
    /**
     * Distance calculator for the directed and empty network.
     */
    private final CompleteDistanceCalculator<Integer> deCalculator;
    /**
     * Distance calculator for the undirected and empty network.
     */
    private final CompleteDistanceCalculator<Integer> ueCalculator;

    /**
     * Constructor.
     */
    public DistanceMetricsTest()
    {
        // Directed Empty Graph (only nodes)
        this.directedEmpty = new FastDirectedUnweightedGraph<>();
        this.directedEmpty.addNode(1);
        this.directedEmpty.addNode(2);
        this.directedEmpty.addNode(3);

        deCalculator = new CompleteDistanceCalculator<>();

        deCalculator.computeDistances(this.directedEmpty);

        // Undirected Empty Graph (only nodes)
        this.undirectedEmpty = new FastUndirectedUnweightedGraph<>();
        this.undirectedEmpty.addNode(1);
        this.undirectedEmpty.addNode(2);
        this.undirectedEmpty.addNode(3);

        ueCalculator = new CompleteDistanceCalculator<>();
        ueCalculator.computeDistances(this.undirectedEmpty);

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

        dcCalculator = new CompleteDistanceCalculator<>();
        dcCalculator.computeDistances(this.directedComplete);

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

        ucCalculator = new CompleteDistanceCalculator<>();
        ucCalculator.computeDistances(this.undirectedComplete);

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

        dscCalculator = new CompleteDistanceCalculator<>();
        dscCalculator.computeDistances(this.directedStronglyConnected);

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

        dwcCalculator = new CompleteDistanceCalculator<>();
        dwcCalculator.computeDistances(this.directedWeaklyConnected);

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

        dncCalculator = new CompleteDistanceCalculator<>();
        dncCalculator.computeDistances(this.directedNonConnected);

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

        uscCalculator = new CompleteDistanceCalculator<>();
        uscCalculator.computeDistances(this.undirectedConnected);

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

        uncCalculator = new CompleteDistanceCalculator<>();
        uncCalculator.computeDistances(this.undirectedNonConnected);
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
     * Test the betweenness of the nodes.
     */
    @Test
    public void nodeBetweenness()
    {
        VertexMetric<Integer> betw = new NodeBetweenness<>(deCalculator, true);
        Assert.assertEquals(0.0, betw.averageValue(this.directedEmpty), 0.0001);

        betw = new NodeBetweenness<>(ueCalculator, true);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedEmpty), 0.0001);

        betw = new NodeBetweenness<>(ucCalculator, true);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedComplete), 0.0001);

        betw = new NodeBetweenness<>(dcCalculator, true);
        Assert.assertEquals(0.0, betw.averageValue(this.directedComplete), 0.0001);

        betw = new NodeBetweenness<>(dwcCalculator, true);
        Assert.assertEquals(0.04285714, betw.averageValue(this.directedWeaklyConnected), 0.0001);

        betw = new NodeBetweenness<>(dscCalculator, true);
        Assert.assertEquals(0.2857143, betw.averageValue(this.directedStronglyConnected), 0.0001);

        betw = new NodeBetweenness<>(dncCalculator, true);
        Assert.assertEquals(0.025, betw.averageValue(this.directedNonConnected), 0.0001);

        betw = new NodeBetweenness<>(uscCalculator, true);
        Assert.assertEquals(0.2666666667, betw.averageValue(this.undirectedConnected), 0.0001);

        betw = new NodeBetweenness<>(uncCalculator, true);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedNonConnected), 0.0001);

    }

    /**
     * Test the betweenness of the edges.
     */
    @Test
    public void edgeBetweenness()
    {
        PairMetric<Integer> betw = new EdgeBetweenness<>(deCalculator, true);
        Assert.assertEquals(0.0, betw.averageValueOnlyLinks(this.directedEmpty), 0.0001);

        betw = new EdgeBetweenness<>(ueCalculator, true);
        Assert.assertEquals(0.0, betw.averageValueOnlyLinks(this.undirectedEmpty), 0.0001);

        betw = new EdgeBetweenness<>(ucCalculator, true);
        Assert.assertEquals(1.0 / 6.0, betw.averageValueOnlyLinks(this.undirectedComplete), 0.0001);

        betw = new EdgeBetweenness<>(dcCalculator, true);
        Assert.assertEquals(1.0 / 12.0, betw.averageValueOnlyLinks(this.directedComplete), 0.0001);

        betw = new EdgeBetweenness<>(dwcCalculator, true);
        Assert.assertEquals(3.0 / 42.0, betw.averageValueOnlyLinks(this.directedWeaklyConnected), 0.0001);

        betw = new EdgeBetweenness<>(dscCalculator, true);
        Assert.assertEquals(9.272727272 / 42.0, betw.averageValueOnlyLinks(this.directedStronglyConnected), 0.0001);

        betw = new EdgeBetweenness<>(dncCalculator, true);
        Assert.assertEquals(2.0 / 30.0, betw.averageValueOnlyLinks(this.directedNonConnected), 0.0001);

        betw = new EdgeBetweenness<>(uscCalculator, true);
        Assert.assertEquals(2.875 / 15.0, betw.averageValueOnlyLinks(this.undirectedConnected), 0.0001);

        betw = new EdgeBetweenness<>(uncCalculator, true);
        Assert.assertEquals(1.0 / 15.0, betw.averageValueOnlyLinks(this.undirectedNonConnected), 0.0001);

    }

    /**
     * Test the average shortest path length.
     */
    @Test
    public void averageShortestPathLength()
    {
        GraphMetric<Integer> betw = new ASL<>(deCalculator);
        Assert.assertEquals(0.0, betw.compute(this.directedEmpty), 0.0001);
        betw = new ASL<>(deCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(0.0, betw.compute(this.directedEmpty), 0.0001);

        betw = new ASL<>(ueCalculator);
        Assert.assertEquals(0.0, betw.compute(this.undirectedEmpty), 0.0001);
        betw = new ASL<>(ueCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(0.0, betw.compute(this.undirectedEmpty), 0.0001);

        betw = new ASL<>(ucCalculator);
        Assert.assertEquals(1.0, betw.compute(this.undirectedComplete), 0.0001);
        betw = new ASL<>(ucCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(1.0, betw.compute(this.undirectedComplete), 0.0001);

        betw = new ASL<>(dcCalculator);
        Assert.assertEquals(1.0, betw.compute(this.directedComplete), 0.0001);
        betw = new ASL<>(dcCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(1.0, betw.compute(this.directedComplete), 0.0001);

        betw = new ASL<>(dwcCalculator);
        Assert.assertEquals(1.6, betw.compute(this.directedWeaklyConnected), 0.0001);
        betw = new ASL<>(dwcCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(0.0, betw.compute(this.directedWeaklyConnected), 0.0001);

        betw = new ASL<>(dscCalculator);
        Assert.assertEquals(2.428571, betw.compute(this.directedStronglyConnected), 0.0001);
        betw = new ASL<>(dscCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(2.428571, betw.compute(this.directedStronglyConnected), 0.0001);

        betw = new ASL<>(dncCalculator);
        Assert.assertEquals(1.3333333, betw.compute(this.directedNonConnected), 0.0001);
        betw = new ASL<>(dncCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(0.375, betw.compute(this.directedNonConnected), 0.0001);

        betw = new ASL<>(uscCalculator);
        Assert.assertEquals(1.5333333, betw.compute(this.undirectedConnected), 0.0001);
        betw = new ASL<>(uscCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(1.5333333, betw.compute(this.undirectedConnected), 0.0001);

        betw = new ASL<>(uncCalculator);
        Assert.assertEquals(1.0, betw.compute(this.undirectedNonConnected), 0.0001);
        betw = new ASL<>(uncCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(1.0, betw.compute(this.undirectedNonConnected), 0.0001);
    }

    /**
     * Test the eccentricity of the nodes.
     */
    @Test
    public void eccentricity()
    {
        VertexMetric<Integer> betw = new Eccentricity<>(deCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.directedEmpty), 0.0001);

        betw = new Eccentricity<>(ueCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedEmpty), 0.0001);

        betw = new Eccentricity<>(ucCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.undirectedComplete), 0.0001);

        betw = new Eccentricity<>(dcCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.directedComplete), 0.0001);

        betw = new Eccentricity<>(dwcCalculator);
        Assert.assertEquals(1.714286, betw.averageValue(this.directedWeaklyConnected), 0.0001);

        betw = new Eccentricity<>(dscCalculator);
        Assert.assertEquals(4.0, betw.averageValue(this.directedStronglyConnected), 0.0001);

        betw = new Eccentricity<>(dncCalculator);
        Assert.assertEquals(1.3333333, betw.averageValue(this.directedNonConnected), 0.0001);

        betw = new Eccentricity<>(uscCalculator);
        Assert.assertEquals(2.33333333, betw.averageValue(this.undirectedConnected), 0.0001);

        betw = new Eccentricity<>(uncCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.undirectedNonConnected), 0.0001);
    }

    /**
     * Test the diameter of the nodes.
     */
    @Test
    public void diameter()
    {
        GraphMetric<Integer> betw = new Diameter<>(deCalculator);
        Assert.assertEquals(0.0, betw.compute(this.directedEmpty), 0.0001);

        betw = new Diameter<>(ueCalculator);
        Assert.assertEquals(0.0, betw.compute(this.undirectedEmpty), 0.0001);

        betw = new Diameter<>(ucCalculator);
        Assert.assertEquals(1.0, betw.compute(this.undirectedComplete), 0.0001);

        betw = new Diameter<>(dcCalculator);
        Assert.assertEquals(1.0, betw.compute(this.directedComplete), 0.0001);

        betw = new Diameter<>(dwcCalculator);
        Assert.assertEquals(3.0, betw.compute(this.directedWeaklyConnected), 0.0001);

        betw = new Diameter<>(dscCalculator);
        Assert.assertEquals(6.0, betw.compute(this.directedStronglyConnected), 0.0001);

        betw = new Diameter<>(dncCalculator);
        Assert.assertEquals(2.0, betw.compute(this.directedNonConnected), 0.0001);

        betw = new Diameter<>(uscCalculator);
        Assert.assertEquals(3.0, betw.compute(this.undirectedConnected), 0.0001);

        betw = new Diameter<>(uncCalculator);
        Assert.assertEquals(1.0, betw.compute(this.undirectedNonConnected), 0.0001);
    }

    /**
     * Test the number of shortest paths between two nodes.
     */
    @Test
    public void geodesics()
    {
        // NOTE: A node has, at least, a path to itself.

        PairMetric<Integer> geod = new Geodesics<>(deCalculator);
        Assert.assertEquals(0.0, geod.averageValue(this.directedEmpty), 0.0001);

        geod = new Geodesics<>(ueCalculator);
        Assert.assertEquals(0.0, geod.averageValue(this.undirectedEmpty), 0.0001);

        geod = new Geodesics<>(ucCalculator);
        Assert.assertEquals(1.0, geod.averageValue(this.undirectedComplete), 0.0001);

        geod = new Geodesics<>(dcCalculator);
        Assert.assertEquals(1.0, geod.averageValue(this.directedComplete), 0.0001);

        geod = new Geodesics<>(dwcCalculator);
        Assert.assertEquals(0.452381, geod.averageValue(this.directedWeaklyConnected), 0.0001);

        geod = new Geodesics<>(dscCalculator);
        Assert.assertEquals(1.2142857, geod.averageValue(this.directedStronglyConnected), 0.0001);

        geod = new Geodesics<>(dncCalculator);
        Assert.assertEquals(0.3, geod.averageValue(this.directedNonConnected), 0.0001);

        geod = new Geodesics<>(uscCalculator);
        Assert.assertEquals(1.3333333, geod.averageValue(this.undirectedConnected), 0.0001);

        geod = new Geodesics<>(uncCalculator);
        Assert.assertEquals(0.4, geod.averageValue(this.undirectedNonConnected), 0.0001);
    }

    /**
     * Test the closeness (on its two variants) of the nodes.
     */
    @Test
    public void closeness()
    {
        // Directed empty graph
        VertexMetric<Integer> betw = new HarmonicCentrality<>(deCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.directedEmpty), 0.0001);
        betw = new Closeness<>(deCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.directedEmpty), 0.0001);

        // Undirected empty graph
        betw = new HarmonicCentrality<>(ueCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedEmpty), 0.0001);
        betw = new Closeness<>(ueCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedEmpty), 0.0001);

        // Undirected complete graph
        betw = new HarmonicCentrality<>(ucCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.undirectedComplete), 0.0001);
        betw = new Closeness<>(ucCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.undirectedComplete), 0.0001);

        // Directed complete graph
        betw = new HarmonicCentrality<>(dcCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.directedComplete), 0.0001);
        betw = new Closeness<>(dcCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.directedComplete), 0.0001);

        // Directed weakly connected graph
        betw = new HarmonicCentrality<>(dwcCalculator);
        Assert.assertEquals(0.265873, betw.averageValue(this.directedWeaklyConnected), 0.0001);
        betw = new Closeness<>(dwcCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.directedWeaklyConnected), 0.0001);

        // Directed strongly connected graph
        betw = new HarmonicCentrality<>(dscCalculator);
        Assert.assertEquals(0.54126984, betw.averageValue(this.directedStronglyConnected), 0.0001);
        betw = new Closeness<>(dscCalculator);
        Assert.assertEquals(0.43778602, betw.averageValue(this.directedStronglyConnected), 0.0001);

        // Directed non connected graph
        betw = new HarmonicCentrality<>(dncCalculator);
        Assert.assertEquals(0.25, betw.averageValue(this.directedNonConnected), 0.0001);
        betw = new Closeness<>(dncCalculator);
        Assert.assertEquals(0.333333, betw.averageValue(this.directedNonConnected), 0.0001);

        // Undirected connected graph
        betw = new HarmonicCentrality<>(uscCalculator);
        Assert.assertEquals(0.7555555, betw.averageValue(this.undirectedConnected), 0.0001);
        betw = new Closeness<>(uscCalculator);
        Assert.assertEquals(0.66633598, betw.averageValue(this.undirectedConnected), 0.0001);

        // Undirected non connected graph
        betw = new HarmonicCentrality<>(uncCalculator);
        Assert.assertEquals(0.4, betw.averageValue(this.undirectedNonConnected), 0.0001);
        betw = new Closeness<>(uncCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.undirectedNonConnected), 0.0001);
    }
}
