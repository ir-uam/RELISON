package es.uam.eps.ir.socialranksys.graph.complementary;

/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.UndirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Weight;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeType;
import es.uam.eps.ir.socialranksys.graph.fast.FastUndirectedWeightedGraph;
import org.junit.*;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

/**
 * Class for testing the fast implementation for undirected weighted graphs.
 * @author Javier Sanz-Cruzado Puig
 */
public class UndirectedWeightedGraphTest 
{
    
    public UndirectedWeightedGraphTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Tests the addition of nodes to the graph.
     */
    @Test
    public void nodeAddition()
    {
        // Build the graph
        UndirectedGraph<Integer> dg = new FastUndirectedWeightedGraph<>();
        Graph<Integer> compl = new UndirectedWeightedComplementaryGraph<>(dg);
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);
        
        assertEquals(3L, compl.getVertexCount());
        // Check non-existing nodes
        assertFalse( compl.containsVertex(4));
        assertFalse( compl.containsVertex(0));
        
        // Check existing nodes
        assertTrue( compl.containsVertex(1));
        assertTrue( compl.containsVertex(2));
        assertTrue( compl.containsVertex(3));
        
        
    }
    
    /**
     * Tests the addition of edges to the graph
     */
    @Test
    public void edgeAddition()
    {
        // Build the graph
        UndirectedGraph<Integer> dg = new FastUndirectedWeightedGraph<>();
        UndirectedGraph<Integer> compl = new UndirectedWeightedComplementaryGraph<>(dg);
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);
        
        // Add some edges with already existing nodes
        dg.addEdge(1,2);
        dg.addEdge(2,3);
           
        // Check the number of edges
        assertEquals(7L, compl.getEdgeCount());
        
        // Check non-existent edges
        // With non-existent nodes
        assertFalse( compl.containsEdge(4,5));
        // With an existent node and a non-existent one
        assertFalse( compl.containsEdge(3,4));
        assertFalse( compl.containsEdge(4,3));
        
        // Check existent nodes
        assertFalse( compl.containsEdge(1,2));
        assertTrue( compl.containsEdge(1,3));
        assertFalse( compl.containsEdge(2,3));
        assertFalse( compl.containsEdge(2,1));
        assertTrue( compl.containsEdge(3,1));
        assertFalse( compl.containsEdge(3,2));
        
        // Try to add an edge with a non-existent node, not adding the new node
        
        // Add a node and an edge
        dg.addEdge(3,4,true);
        assertEquals(4L, compl.getVertexCount());
        assertEquals(13L, compl.getEdgeCount());
        assertFalse( compl.containsEdge(3,4));
        assertFalse( compl.containsEdge(4,3));
        
        // Add two nodes and an edge (default behaviour)
        dg.addEdge(5,6);
        assertEquals(6L, compl.getVertexCount());
        assertEquals(32L, compl.getEdgeCount());
        assertFalse( compl.containsEdge(5, 6));   
        assertTrue( compl.containsEdge(3, 6));
        assertTrue( compl.containsEdge(6, 3));
        
        // Add a reciprocate edge
        dg.addEdge(2,1);
        assertFalse( compl.containsEdge(2,1));
        assertEquals(32L, compl.getEdgeCount());
    }
    
    /**
     * Test the types of the edges
     */
    @Test
    public void weights()
    {
        // Build the graph
        UndirectedGraph<Integer> dg = new FastUndirectedWeightedGraph<>();
        UndirectedGraph<Integer> compl = new UndirectedWeightedComplementaryGraph<>(dg);
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);
        dg.addNode(4);
        
        // Add some edges with already existing nodes
        dg.addEdge(1,2,4.0,1);
        dg.addEdge(1,2,4.0);
        dg.addEdge(1,3,5.0);
        dg.addEdge(2,3);
        
        // Check individually
        assertEquals(1.0, compl.getEdgeWeight(1, 4), 0.001);
        assertEquals(1.0, compl.getEdgeWeight(2, 4), 0.001);
        assertEquals(1.0, compl.getEdgeWeight(4, 3), 0.001);
                
        // Recover each weight and check (adjacent edges)
        List<Weight<Integer,Double>> weights = compl.getAdjacentNodesWeights(1).collect(toList());
        for(Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }
        
        weights = compl.getAdjacentNodesWeights(2).collect(toList());
        for(Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }
        
        weights = compl.getAdjacentNodesWeights(3).collect(toList());
        for(Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }
        
        // Recover each weight and check (incident edges)
        weights = compl.getIncidentNodesWeights(1).collect(toList());
        for(Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }
        
        weights = compl.getIncidentNodesWeights(2).collect(toList());
        for(Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }
        
        weights = compl.getIncidentNodesWeights(3).collect(toList());
        for(Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }
        
        // Recover each weight and check (adjacent edges)
        weights = compl.getNeighbourNodesWeights(1).collect(toList());
        for(Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }
        
        weights = compl.getNeighbourNodesWeights(2).collect(toList());
        for(Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }
        
        weights = compl.getNeighbourNodesWeights(3).collect(toList());
        for(Weight<Integer, Double> weight : weights)
        {
            assertEquals(1.0, weight.getValue(), 0.001);
        }
    }
    
    /**
     * Test the types of the edges
     */
    @Test
    public void types()
    {
        // Build the graph
        UndirectedGraph<Integer> dg = new FastUndirectedWeightedGraph<>();
        UndirectedGraph<Integer> compl = new UndirectedWeightedComplementaryGraph<>(dg);
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);
        dg.addNode(4);
        
        // Add some edges with already existing nodes
        dg.addEdge(1,2,4.0,1);
        dg.addEdge(1,2,4.0,1);
        dg.addEdge(1,3,5.0,2);
        dg.addEdge(2,3,3.0,3);
        
        // Check individually
        
        assertEquals(EdgeType.getDefaultValue(), compl.getEdgeType(4, 1));
        assertEquals(EdgeType.getDefaultValue(), compl.getEdgeType(3, 4));
        assertEquals(EdgeType.getDefaultValue(), compl.getEdgeType(4, 2));
        

                
        // Recover each type and check (adjacent edges)
        List<Weight<Integer,Integer>> types = compl.getAdjacentNodesTypes(1).collect(toList());
        for(Weight<Integer, Integer> type : types)
        {
            assertEquals(EdgeType.getDefaultValue(), (double) type.getValue(), 0.001);
        }
        
        types = compl.getAdjacentNodesTypes(2).collect(toList());
        for(Weight<Integer, Integer> type : types)
        {
            assertEquals(EdgeType.getDefaultValue(), (double) type.getValue(), 0.001);
        }
        
        types = compl.getAdjacentNodesTypes(3).collect(toList());
        for(Weight<Integer, Integer> type : types)
        {
            assertEquals(EdgeType.getDefaultValue(), (double) type.getValue(), 0.001);
        }
        
        // Recover each type and check (incident edges)
        types = compl.getIncidentNodesTypes(1).collect(toList());
        for(Weight<Integer, Integer> type : types)
        {
            assertEquals(EdgeType.getDefaultValue(), (double) type.getValue(), 0.001);
        }
        
        types = compl.getIncidentNodesTypes(2).collect(toList());
        for(Weight<Integer, Integer> type : types)
        {
            assertEquals(EdgeType.getDefaultValue(), (double) type.getValue(), 0.001);
        }
        
        types = compl.getIncidentNodesTypes(3).collect(toList());
        for(Weight<Integer, Integer> type : types)
        {
            assertEquals(EdgeType.getDefaultValue(), (double) type.getValue(), 0.001);
        }
        
        // Recover each type and check (all edges)
        types = compl.getNeighbourNodesTypes(1).collect(toList());
        for(Weight<Integer, Integer> type : types)
        {
            assertEquals(EdgeType.getDefaultValue(), (double) type.getValue(), 0.001);
        }
        
        types = compl.getNeighbourNodesTypes(2).collect(toList());
        for(Weight<Integer, Integer> type : types)
        {
            assertEquals(EdgeType.getDefaultValue(), (double) type.getValue(), 0.001);
        }
        
        types = compl.getNeighbourNodesTypes(3).collect(toList());
        for(Weight<Integer, Integer> type : types)
        {
            assertEquals(EdgeType.getDefaultValue(), (double) type.getValue(), 0.001);
        }
        
        
    }    
    /**
     * Test the degrees of the graph
     */
    @Test
    public void degrees()
    {
        // Build the graph
        UndirectedGraph<Integer> dg = new FastUndirectedWeightedGraph<>();
        UndirectedGraph<Integer> compl = new UndirectedWeightedComplementaryGraph<>(dg);
        
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);
        dg.addNode(4);
        dg.addNode(5);
        dg.addEdge(1,2);
        dg.addEdge(1,2);
        dg.addEdge(1,3);
        dg.addEdge(2,3);
        dg.addEdge(5,1);
        
        // Check the in-degrees of the nodes
        assertEquals(compl.degree(1),2);
        assertEquals(compl.degree(2),3);
        assertEquals(compl.degree(3),3);
        assertEquals(compl.degree(4),5);
        assertEquals(compl.degree(5),4);
    }
    
    /**
     * Test that checks if the number of nodes and edges which reach a node or start from 
     * it are correct.
     */
    @Test
    public void edgesAndNodesCount()
    {
        UndirectedGraph<Integer> dg = new FastUndirectedWeightedGraph<>();
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);
        dg.addEdge(1,2);
        dg.addEdge(1,2);
        dg.addEdge(1,3);
        dg.addEdge(2,3);
        
        // Check the number of adjacent edges
        assertEquals(2, dg.getAdjacentEdgesCount(1));
        assertEquals(2, dg.getAdjacentEdgesCount(2));
        assertEquals(2, dg.getAdjacentEdgesCount(3));
        
        // Check the number of adjacent nodes
        assertEquals(2, dg.getAdjacentNodes(1).count());
        assertEquals(2, dg.getAdjacentNodes(2).count());
        assertEquals(2, dg.getAdjacentNodes(3).count());
        
        assertEquals(2, dg.getAdjacentNodesCount(1));
        assertEquals(2, dg.getAdjacentNodesCount(2));
        assertEquals(2, dg.getAdjacentNodesCount(3));
        
        // Check the number of incident edges
        assertEquals(2, dg.getIncidentEdgesCount(1));
        assertEquals(2, dg.getIncidentEdgesCount(2));
        assertEquals(2, dg.getIncidentEdgesCount(3));
        
        // Check the number of incident nodes
        assertEquals(2, dg.getIncidentNodes(1).count());
        assertEquals(2, dg.getIncidentNodes(2).count());
        assertEquals(2, dg.getIncidentNodes(3).count());
        
        assertEquals(2, dg.getIncidentNodesCount(1));
        assertEquals(2, dg.getIncidentNodesCount(2));
        assertEquals(2, dg.getIncidentNodesCount(3));
        dg.addEdge(2,1);
        
        // Check the number of neighbor edges
        assertEquals(2, dg.getNeighbourEdgesCount(1));
        assertEquals(2, dg.getNeighbourEdgesCount(2));
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
