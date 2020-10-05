/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics;

import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.UndirectedGraph;
import es.uam.eps.ir.socialranksys.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.socialranksys.graph.fast.FastUndirectedUnweightedGraph;
import es.uam.eps.ir.socialranksys.metrics.distance.CompleteDistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.edge.EdgeBetweenness;
import es.uam.eps.ir.socialranksys.metrics.distance.graph.ASL;
import es.uam.eps.ir.socialranksys.metrics.distance.graph.Diameter;
import es.uam.eps.ir.socialranksys.metrics.distance.modes.ASLMode;
import es.uam.eps.ir.socialranksys.metrics.distance.modes.ClosenessMode;
import es.uam.eps.ir.socialranksys.metrics.distance.pair.Geodesics;
import es.uam.eps.ir.socialranksys.metrics.distance.vertex.Closeness;
import es.uam.eps.ir.socialranksys.metrics.distance.vertex.Eccentricity;
import es.uam.eps.ir.socialranksys.metrics.distance.vertex.NodeBetweenness;
import org.junit.*;

/**
 * Tests for the betweenness metric.
 *
 * @author Javier Sanz-Cruzado Puig
 */
public class BetweennessMetricsTest
{

    /**
     * Directed Strongly Connected Graph
     */
    private final DirectedGraph<Integer> directedStronglyConnected;
    /**
     * Directed Weakly Connected Graph
     */
    private final DirectedGraph<Integer> directedWeaklyConnected;
    /**
     * Directed Unconnected Graph (not even weakly)
     */
    private final DirectedGraph<Integer> directedNonConnected;
    /**
     * Undirected Connected Graph
     */
    private final UndirectedGraph<Integer> undirectedConnected;
    /**
     * Undirected Unconnected Graph
     */
    private final UndirectedGraph<Integer> undirectedNonConnected;
    /**
     * Directed complete graph
     */
    private final DirectedGraph<Integer> directedComplete;
    /**
     * Directed empty graph (only nodes)
     */
    private final DirectedGraph<Integer> directedEmpty;
    /**
     * Undirected complete graph
     */
    private final UndirectedGraph<Integer> undirectedComplete;
    /**
     * Undirected empty graph
     */
    private final UndirectedGraph<Integer> undirectedEmpty;

    CompleteDistanceCalculator<Integer> dscBetweennessCalculator;
    CompleteDistanceCalculator<Integer> dwcBetweennessCalculator;
    CompleteDistanceCalculator<Integer> dncBetweennessCalculator;
    CompleteDistanceCalculator<Integer> uscBetweennessCalculator;
    CompleteDistanceCalculator<Integer> uncBetweennessCalculator;
    CompleteDistanceCalculator<Integer> dcBetweennessCalculator;
    CompleteDistanceCalculator<Integer> ucBetweennessCalculator;
    CompleteDistanceCalculator<Integer> deBetweennessCalculator;
    CompleteDistanceCalculator<Integer> ueBetweennessCalculator;


    public BetweennessMetricsTest()
    {
        // Directed Empty Graph (only nodes)
        this.directedEmpty = new FastDirectedUnweightedGraph<>();
        this.directedEmpty.addNode(1);
        this.directedEmpty.addNode(2);
        this.directedEmpty.addNode(3);

        deBetweennessCalculator = new CompleteDistanceCalculator<>();

        deBetweennessCalculator.computeDistances(this.directedEmpty);

        // Undirected Empty Graph (only nodes)
        this.undirectedEmpty = new FastUndirectedUnweightedGraph<>();
        this.undirectedEmpty.addNode(1);
        this.undirectedEmpty.addNode(2);
        this.undirectedEmpty.addNode(3);

        ueBetweennessCalculator = new CompleteDistanceCalculator<>();
        ueBetweennessCalculator.computeDistances(this.undirectedEmpty);

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

        dcBetweennessCalculator = new CompleteDistanceCalculator<>();
        dcBetweennessCalculator.computeDistances(this.directedComplete);

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

        ucBetweennessCalculator = new CompleteDistanceCalculator<>();
        ucBetweennessCalculator.computeDistances(this.undirectedComplete);

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

        dscBetweennessCalculator = new CompleteDistanceCalculator<>();
        dscBetweennessCalculator.computeDistances(this.directedStronglyConnected);

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

        dwcBetweennessCalculator = new CompleteDistanceCalculator<>();
        dwcBetweennessCalculator.computeDistances(this.directedWeaklyConnected);

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

        dncBetweennessCalculator = new CompleteDistanceCalculator<>();
        dncBetweennessCalculator.computeDistances(this.directedNonConnected);

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

        uscBetweennessCalculator = new CompleteDistanceCalculator<>();
        uscBetweennessCalculator.computeDistances(this.undirectedConnected);

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

        uncBetweennessCalculator = new CompleteDistanceCalculator<>();
        uncBetweennessCalculator.computeDistances(this.undirectedNonConnected);
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

    @Test
    public void nodeBetweenness()
    {
        VertexMetric<Integer> betw = new NodeBetweenness<>(deBetweennessCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.directedEmpty), 0.0001);

        betw = new NodeBetweenness<>(ueBetweennessCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedEmpty), 0.0001);

        betw = new NodeBetweenness<>(ucBetweennessCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedComplete), 0.0001);

        betw = new NodeBetweenness<>(dcBetweennessCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.directedComplete), 0.0001);

        betw = new NodeBetweenness<>(dwcBetweennessCalculator);
        Assert.assertEquals(0.04285714, betw.averageValue(this.directedWeaklyConnected), 0.0001);

        betw = new NodeBetweenness<>(dscBetweennessCalculator);
        Assert.assertEquals(0.2857143, betw.averageValue(this.directedStronglyConnected), 0.0001);

        betw = new NodeBetweenness<>(dncBetweennessCalculator);
        Assert.assertEquals(0.025, betw.averageValue(this.directedNonConnected), 0.0001);

        betw = new NodeBetweenness<>(uscBetweennessCalculator);
        Assert.assertEquals(0.2666666667, betw.averageValue(this.undirectedConnected), 0.0001);

        betw = new NodeBetweenness<>(uncBetweennessCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedNonConnected), 0.0001);

    }

    @Test
    public void edgeBetweenness()
    {
        EdgeMetric<Integer> betw = new EdgeBetweenness<>(deBetweennessCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.directedEmpty), 0.0001);

        betw = new EdgeBetweenness<>(ueBetweennessCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedEmpty), 0.0001);

        betw = new EdgeBetweenness<>(ucBetweennessCalculator);
        Assert.assertEquals(1.0 / 6.0, betw.averageValue(this.undirectedComplete), 0.0001);

        betw = new EdgeBetweenness<>(dcBetweennessCalculator);
        Assert.assertEquals(1.0 / 12.0, betw.averageValue(this.directedComplete), 0.0001);

        betw = new EdgeBetweenness<>(dwcBetweennessCalculator);
        Assert.assertEquals(3.0 / 42.0, betw.averageValue(this.directedWeaklyConnected), 0.0001);

        betw = new EdgeBetweenness<>(dscBetweennessCalculator);
        Assert.assertEquals(9.272727272 / 42.0, betw.averageValue(this.directedStronglyConnected), 0.0001);

        betw = new EdgeBetweenness<>(dncBetweennessCalculator);
        Assert.assertEquals(2.0 / 30.0, betw.averageValue(this.directedNonConnected), 0.0001);

        betw = new EdgeBetweenness<>(uscBetweennessCalculator);
        Assert.assertEquals(2.875 / 15.0, betw.averageValue(this.undirectedConnected), 0.0001);

        betw = new EdgeBetweenness<>(uncBetweennessCalculator);
        Assert.assertEquals(1.0 / 15.0, betw.averageValue(this.undirectedNonConnected), 0.0001);

    }

    @Test
    public void averageShortestPathLength()
    {
        GraphMetric<Integer> betw = new ASL<>(deBetweennessCalculator);
        Assert.assertEquals(0.0, betw.compute(this.directedEmpty), 0.0001);
        betw = new ASL<>(deBetweennessCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(0.0, betw.compute(this.directedEmpty), 0.0001);

        betw = new ASL<>(ueBetweennessCalculator);
        Assert.assertEquals(0.0, betw.compute(this.undirectedEmpty), 0.0001);
        betw = new ASL<>(ueBetweennessCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(0.0, betw.compute(this.undirectedEmpty), 0.0001);

        betw = new ASL<>(ucBetweennessCalculator);
        Assert.assertEquals(1.0, betw.compute(this.undirectedComplete), 0.0001);
        betw = new ASL<>(ucBetweennessCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(1.0, betw.compute(this.undirectedComplete), 0.0001);

        betw = new ASL<>(dcBetweennessCalculator);
        Assert.assertEquals(1.0, betw.compute(this.directedComplete), 0.0001);
        betw = new ASL<>(dcBetweennessCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(1.0, betw.compute(this.directedComplete), 0.0001);

        betw = new ASL<>(dwcBetweennessCalculator);
        Assert.assertEquals(1.6, betw.compute(this.directedWeaklyConnected), 0.0001);
        betw = new ASL<>(dwcBetweennessCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(0.0, betw.compute(this.directedWeaklyConnected), 0.0001);

        betw = new ASL<>(dscBetweennessCalculator);
        Assert.assertEquals(2.428571, betw.compute(this.directedStronglyConnected), 0.0001);
        betw = new ASL<>(dscBetweennessCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(2.428571, betw.compute(this.directedStronglyConnected), 0.0001);

        betw = new ASL<>(dncBetweennessCalculator);
        Assert.assertEquals(1.3333333, betw.compute(this.directedNonConnected), 0.0001);
        betw = new ASL<>(dncBetweennessCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(0.375, betw.compute(this.directedNonConnected), 0.0001);

        betw = new ASL<>(uscBetweennessCalculator);
        Assert.assertEquals(1.5333333, betw.compute(this.undirectedConnected), 0.0001);
        betw = new ASL<>(uscBetweennessCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(1.5333333, betw.compute(this.undirectedConnected), 0.0001);

        betw = new ASL<>(uncBetweennessCalculator);
        Assert.assertEquals(1.0, betw.compute(this.undirectedNonConnected), 0.0001);
        betw = new ASL<>(uncBetweennessCalculator, ASLMode.COMPONENTS);
        Assert.assertEquals(1.0, betw.compute(this.undirectedNonConnected), 0.0001);
    }

    @Test
    public void eccentricity()
    {
        VertexMetric<Integer> betw = new Eccentricity<>(deBetweennessCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.directedEmpty), 0.0001);

        betw = new Eccentricity<>(ueBetweennessCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedEmpty), 0.0001);

        betw = new Eccentricity<>(ucBetweennessCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.undirectedComplete), 0.0001);

        betw = new Eccentricity<>(dcBetweennessCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.directedComplete), 0.0001);

        betw = new Eccentricity<>(dwcBetweennessCalculator);
        Assert.assertEquals(1.714286, betw.averageValue(this.directedWeaklyConnected), 0.0001);

        betw = new Eccentricity<>(dscBetweennessCalculator);
        Assert.assertEquals(4.0, betw.averageValue(this.directedStronglyConnected), 0.0001);

        betw = new Eccentricity<>(dncBetweennessCalculator);
        Assert.assertEquals(1.3333333, betw.averageValue(this.directedNonConnected), 0.0001);

        betw = new Eccentricity<>(uscBetweennessCalculator);
        Assert.assertEquals(2.33333333, betw.averageValue(this.undirectedConnected), 0.0001);

        betw = new Eccentricity<>(uncBetweennessCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.undirectedNonConnected), 0.0001);
    }

    @Test
    public void diameter()
    {
        GraphMetric<Integer> betw = new Diameter<>(deBetweennessCalculator);
        Assert.assertEquals(0.0, betw.compute(this.directedEmpty), 0.0001);

        betw = new Diameter<>(ueBetweennessCalculator);
        Assert.assertEquals(0.0, betw.compute(this.undirectedEmpty), 0.0001);

        betw = new Diameter<>(ucBetweennessCalculator);
        Assert.assertEquals(1.0, betw.compute(this.undirectedComplete), 0.0001);

        betw = new Diameter<>(dcBetweennessCalculator);
        Assert.assertEquals(1.0, betw.compute(this.directedComplete), 0.0001);

        betw = new Diameter<>(dwcBetweennessCalculator);
        Assert.assertEquals(3.0, betw.compute(this.directedWeaklyConnected), 0.0001);

        betw = new Diameter<>(dscBetweennessCalculator);
        Assert.assertEquals(6.0, betw.compute(this.directedStronglyConnected), 0.0001);

        betw = new Diameter<>(dncBetweennessCalculator);
        Assert.assertEquals(2.0, betw.compute(this.directedNonConnected), 0.0001);

        betw = new Diameter<>(uscBetweennessCalculator);
        Assert.assertEquals(3.0, betw.compute(this.undirectedConnected), 0.0001);

        betw = new Diameter<>(uncBetweennessCalculator);
        Assert.assertEquals(1.0, betw.compute(this.undirectedNonConnected), 0.0001);
    }

    @Test
    public void geodesics()
    {
        // NOTE: A node has, at least, a path to itself.

        PairMetric<Integer> geod = new Geodesics<>(deBetweennessCalculator);
        Assert.assertEquals(0.0, geod.averageValue(this.directedEmpty), 0.0001);

        geod = new Geodesics<>(ueBetweennessCalculator);
        Assert.assertEquals(0.0, geod.averageValue(this.undirectedEmpty), 0.0001);

        geod = new Geodesics<>(ucBetweennessCalculator);
        Assert.assertEquals(1.0, geod.averageValue(this.undirectedComplete), 0.0001);

        geod = new Geodesics<>(dcBetweennessCalculator);
        Assert.assertEquals(1.0, geod.averageValue(this.directedComplete), 0.0001);

        geod = new Geodesics<>(dwcBetweennessCalculator);
        Assert.assertEquals(0.452381, geod.averageValue(this.directedWeaklyConnected), 0.0001);

        geod = new Geodesics<>(dscBetweennessCalculator);
        Assert.assertEquals(1.2142857, geod.averageValue(this.directedStronglyConnected), 0.0001);

        geod = new Geodesics<>(dncBetweennessCalculator);
        Assert.assertEquals(0.3, geod.averageValue(this.directedNonConnected), 0.0001);

        geod = new Geodesics<>(uscBetweennessCalculator);
        Assert.assertEquals(1.3333333, geod.averageValue(this.undirectedConnected), 0.0001);

        geod = new Geodesics<>(uncBetweennessCalculator);
        Assert.assertEquals(0.4, geod.averageValue(this.undirectedNonConnected), 0.0001);
    }

    @Test
    public void closeness()
    {
        // Directed empty graph
        VertexMetric<Integer> betw = new Closeness<>(deBetweennessCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.directedEmpty), 0.0001);
        betw = new Closeness<>(deBetweennessCalculator, ClosenessMode.COMPONENTS);
        Assert.assertEquals(0.0, betw.averageValue(this.directedEmpty), 0.0001);

        // Undirected empty graph
        betw = new Closeness<>(ueBetweennessCalculator);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedEmpty), 0.0001);
        betw = new Closeness<>(ueBetweennessCalculator, ClosenessMode.COMPONENTS);
        Assert.assertEquals(0.0, betw.averageValue(this.undirectedEmpty), 0.0001);

        // Undirected complete graph
        betw = new Closeness<>(ucBetweennessCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.undirectedComplete), 0.0001);
        betw = new Closeness<>(ucBetweennessCalculator, ClosenessMode.COMPONENTS);
        Assert.assertEquals(1.0, betw.averageValue(this.undirectedComplete), 0.0001);

        // Directed complete graph
        betw = new Closeness<>(dcBetweennessCalculator);
        Assert.assertEquals(1.0, betw.averageValue(this.directedComplete), 0.0001);
        betw = new Closeness<>(dcBetweennessCalculator, ClosenessMode.COMPONENTS);
        Assert.assertEquals(1.0, betw.averageValue(this.directedComplete), 0.0001);

        // Directed weakly connected graph
        betw = new Closeness<>(dwcBetweennessCalculator);
        Assert.assertEquals(0.265873, betw.averageValue(this.directedWeaklyConnected), 0.0001);
        betw = new Closeness<>(dwcBetweennessCalculator, ClosenessMode.COMPONENTS);
        Assert.assertEquals(0.0, betw.averageValue(this.directedWeaklyConnected), 0.0001);

        // Directed strongly connected graph
        betw = new Closeness<>(dscBetweennessCalculator);
        Assert.assertEquals(0.54126984, betw.averageValue(this.directedStronglyConnected), 0.0001);
        betw = new Closeness<>(dscBetweennessCalculator, ClosenessMode.COMPONENTS);
        Assert.assertEquals(0.43778602, betw.averageValue(this.directedStronglyConnected), 0.0001);

        // Directed non connected graph
        betw = new Closeness<>(dncBetweennessCalculator);
        Assert.assertEquals(0.25, betw.averageValue(this.directedNonConnected), 0.0001);
        betw = new Closeness<>(dncBetweennessCalculator, ClosenessMode.COMPONENTS);
        Assert.assertEquals(0.333333, betw.averageValue(this.directedNonConnected), 0.0001);

        // Undirected connected graph
        betw = new Closeness<>(uscBetweennessCalculator);
        Assert.assertEquals(0.7555555, betw.averageValue(this.undirectedConnected), 0.0001);
        betw = new Closeness<>(uscBetweennessCalculator, ClosenessMode.COMPONENTS);
        Assert.assertEquals(0.66633598, betw.averageValue(this.undirectedConnected), 0.0001);

        // Undirected non connected graph
        betw = new Closeness<>(uncBetweennessCalculator);
        Assert.assertEquals(0.4, betw.averageValue(this.undirectedNonConnected), 0.0001);
        betw = new Closeness<>(uncBetweennessCalculator, ClosenessMode.COMPONENTS);
        Assert.assertEquals(1.0, betw.averageValue(this.undirectedNonConnected), 0.0001);
    }
}
