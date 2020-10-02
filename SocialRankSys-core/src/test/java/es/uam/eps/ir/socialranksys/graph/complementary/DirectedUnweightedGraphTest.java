package es.uam.eps.ir.socialranksys.graph.complementary;
/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Weight;
import es.uam.eps.ir.socialranksys.graph.fast.FastDirectedUnweightedGraph;
import org.junit.*;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

/**
 * Class that tests the fast directed unweighted implementation of graphs.
 *
 * @author Javier Sanz-Cruzado Puig
 */
public class DirectedUnweightedGraphTest
{

    public DirectedUnweightedGraphTest()
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
     * Tests the addition of nodes to the graph.
     */
    @Test
    public void nodeAddition()
    {
        // Build the graph
        DirectedGraph<Integer> dg = new FastDirectedUnweightedGraph<>();
        assertTrue(dg.addNode(1));
        assertTrue(dg.addNode(2));
        assertTrue(dg.addNode(3));
        assertFalse(dg.addNode(1));

        // Check the number of vertices
        assertEquals(3L, dg.getVertexCount());

        // Check non-existing nodes
        assertFalse(dg.containsVertex(4));
        assertFalse(dg.containsVertex(0));

        // Check existing nodes
        assertTrue(dg.containsVertex(1));
        assertTrue(dg.containsVertex(2));
        assertTrue(dg.containsVertex(3));
    }

    /**
     * Tests the addition of edges to the graph
     */
    @Test
    public void edgeAddition()
    {
        // Build the graph
        DirectedGraph<Integer> dg = new FastDirectedUnweightedGraph<>();
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);

        // Add some edges with already existing nodes
        assertTrue(dg.addEdge(1, 2));
        assertFalse(dg.addEdge(1, 2));
        assertTrue(dg.addEdge(1, 3));
        assertTrue(dg.addEdge(2, 3));

        // Check the number of edges
        assertEquals(3L, dg.getEdgeCount());

        // Check non-existent edges
        // With non-existent nodes
        assertFalse(dg.containsEdge(4, 5));
        // With an existent node and a non-existent one
        assertFalse(dg.containsEdge(3, 4));
        assertFalse(dg.containsEdge(4, 3));
        // With both nodes existent
        assertFalse(dg.containsEdge(3, 2));

        // Check existent edges
        assertTrue(dg.containsEdge(1, 2));
        assertTrue(dg.containsEdge(1, 3));
        assertTrue(dg.containsEdge(2, 3));

        // Try to add an edge with a non-existent node, not adding the new node
        assertFalse(dg.addEdge(3, 4, false));
        assertEquals(3L, dg.getVertexCount());
        assertEquals(3L, dg.getEdgeCount());

        // Add a node and an edge
        assertTrue(dg.addEdge(3, 4, true));
        assertEquals(4L, dg.getVertexCount());
        assertEquals(4L, dg.getEdgeCount());
        assertTrue(dg.containsEdge(3, 4));

        // Add two nodes and an edge (default behaviour)
        assertTrue(dg.addEdge(5, 6));
        assertEquals(6L, dg.getVertexCount());
        assertEquals(5L, dg.getEdgeCount());
        assertTrue(dg.containsEdge(5, 6));

        // Add a reciprocate edge
        assertTrue(dg.addEdge(2, 1));
        assertTrue(dg.containsEdge(2, 1));
        assertEquals(6L, dg.getEdgeCount());
    }

    /**
     * Test the weights of the edges
     */
    @Test
    public void weights()
    {
        // Build the graph
        DirectedGraph<Integer> dg = new FastDirectedUnweightedGraph<>();
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);

        // Add some edges with already existing nodes
        dg.addEdge(1, 2, 4.0, 1);
        dg.addEdge(1, 2, 4.0);
        dg.addEdge(1, 3, 5.0);
        dg.addEdge(2, 3, 3.0);

        // Check individually
        assertEquals(1.0, dg.getEdgeWeight(1, 2), 0.001);
        assertEquals(1.0, dg.getEdgeWeight(1, 3), 0.001);
        assertEquals(1.0, dg.getEdgeWeight(2, 3), 0.001);

        // Recover each weight and check (adjacent edges)
        List<Weight<Integer, Double>> weights = dg.getAdjacentNodesWeights(1).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }

        weights = dg.getAdjacentNodesWeights(2).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }

        weights = dg.getAdjacentNodesWeights(3).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }

        // Recover each weight and check (incident edges)
        weights = dg.getIncidentNodesWeights(1).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }

        weights = dg.getIncidentNodesWeights(2).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }

        weights = dg.getIncidentNodesWeights(3).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }
    }

    /**
     * Test the weights of the edges
     */
    @Test
    public void types()
    {
        // Build the graph
        DirectedGraph<Integer> dg = new FastDirectedUnweightedGraph<>();
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);

        // Add some edges with already existing nodes
        dg.addEdge(1, 2, 4.0, 1);
        dg.addEdge(1, 2, 4.0, 1);
        dg.addEdge(1, 3, 5.0, 2);
        dg.addEdge(2, 3, 3.0, 3);

        // Check individually
        assertEquals(1, dg.getEdgeType(1, 2));
        assertEquals(2, dg.getEdgeType(1, 3));
        assertEquals(3, dg.getEdgeType(2, 3));

        // Recover each weight and check (adjacent)
        List<Weight<Integer, Integer>> types = dg.getAdjacentNodesTypes(1).collect(toList());
        for (Weight<Integer, Integer> type : types)
        {
            if (type.getIdx().equals(2))
            {
                assertEquals(1, type.getValue(), 0.001);
            }
            else
            {
                assertEquals(2, type.getValue(), 0.001);
            }
        }

        types = dg.getAdjacentNodesTypes(2).collect(toList());
        for (Weight<Integer, Integer> type : types)
        {
            assertEquals(3, type.getValue(), 0.001);
        }

        types = dg.getAdjacentNodesTypes(3).collect(toList());
        for (Weight<Integer, Integer> type : types)
        {
            assertEquals(0, type.getValue(), 0.001);
        }

        // Recover each weight and check (incident)
        types = dg.getIncidentNodesTypes(3).collect(toList());
        for (Weight<Integer, Integer> type : types)
        {
            if (type.getIdx().equals(1))
            {
                assertEquals(2, type.getValue(), 0.001);
            }
            else
            {
                assertEquals(3, type.getValue(), 0.001);
            }
        }

        types = dg.getIncidentNodesTypes(2).collect(toList());
        for (Weight<Integer, Integer> type : types)
        {
            assertEquals(1, type.getValue(), 0.001);
        }

        types = dg.getIncidentNodesTypes(1).collect(toList());
        for (Weight<Integer, Integer> type : types)
        {
            assertEquals(0, type.getValue(), 0.001);
        }

        dg.addEdge(2, 1, 1.0, 2);
    }

    /**
     * Test the degrees of the graph
     */
    @Test
    public void degrees()
    {
        // Build the graph
        DirectedGraph<Integer> dg = new FastDirectedUnweightedGraph<>();
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);
        dg.addEdge(1, 2);
        dg.addEdge(1, 2);
        dg.addEdge(1, 3);
        dg.addEdge(2, 3);

        // Check the in-degrees of the nodes
        assertEquals(dg.inDegree(1), 0);
        assertEquals(dg.inDegree(2), 1);
        assertEquals(dg.inDegree(3), 2);

        // Check the out-degrees of the nodes
        assertEquals(dg.outDegree(1), 2);
        assertEquals(dg.outDegree(2), 1);
        assertEquals(dg.outDegree(3), 0);
    }

    /**
     * Test that checks if the number of nodes and edges which reach a node or start from
     * it are correct.
     */
    @Test
    public void edgesAndNodesCount()
    {
        DirectedGraph<Integer> dg = new FastDirectedUnweightedGraph<>();
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);
        dg.addEdge(1, 2);
        dg.addEdge(1, 2);
        dg.addEdge(1, 3);
        dg.addEdge(2, 3);

        // Check the number of adjacent edges
        assertEquals(2, dg.getAdjacentEdgesCount(1));
        assertEquals(1, dg.getAdjacentEdgesCount(2));
        assertEquals(0, dg.getAdjacentEdgesCount(3));

        // Check the number of adjacent nodes
        assertEquals(2, dg.getAdjacentNodes(1).count());
        assertEquals(1, dg.getAdjacentNodes(2).count());
        assertEquals(0, dg.getAdjacentNodes(3).count());

        assertEquals(2, dg.getAdjacentNodesCount(1));
        assertEquals(1, dg.getAdjacentNodesCount(2));
        assertEquals(0, dg.getAdjacentNodesCount(3));

        // Check the number of incident edges
        assertEquals(0, dg.getIncidentEdgesCount(1));
        assertEquals(1, dg.getIncidentEdgesCount(2));
        assertEquals(2, dg.getIncidentEdgesCount(3));

        // Check the number of incident nodes
        assertEquals(0, dg.getIncidentNodes(1).count());
        assertEquals(1, dg.getIncidentNodes(2).count());
        assertEquals(2, dg.getIncidentNodes(3).count());

        assertEquals(0, dg.getIncidentNodesCount(1));
        assertEquals(1, dg.getIncidentNodesCount(2));
        assertEquals(2, dg.getIncidentNodesCount(3));
        dg.addEdge(2, 1);

        // Check the number of neighbor edges
        assertEquals(3, dg.getNeighbourEdgesCount(1));
        assertEquals(3, dg.getNeighbourEdgesCount(2));
        assertEquals(2, dg.getNeighbourEdgesCount(3));

        // Check the number of neighbor nodes
        assertEquals(2, dg.getNeighbourNodes(1).count());
        assertEquals(2, dg.getNeighbourNodes(2).count());
        assertEquals(2, dg.getNeighbourNodes(3).count());

        assertEquals(2, dg.getNeighbourNodesCount(1));
        assertEquals(2, dg.getNeighbourNodesCount(2));
        assertEquals(2, dg.getNeighbourNodesCount(3));
    }
}
