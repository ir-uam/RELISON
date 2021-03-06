package es.uam.eps.ir.relison.graph;

/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


import es.uam.eps.ir.relison.graph.edges.EdgeType;
import es.uam.eps.ir.relison.graph.edges.EdgeWeight;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedWeightedGraph;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.junit.*;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

/**
 * Class for testing the fast implementation for undirected weighted graphs.
 *
 * @author Javier Sanz-Cruzado Puig
 */
public class UndirectedWeightedGraphTest
{
    /**
     * Constructor.
     */
    public UndirectedWeightedGraphTest()
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


    @Test
    public void test()
    {
        Random rnd = new Random();
        // Number of users
        int N = rnd.nextInt(5000);

        // Put them in a random order.
        List<String> users = IntStream.range(0, N).mapToObj(Integer::toString).collect(toList());
        Collections.shuffle(users);

        // Build a directed weighted graph and add all the nodes.
        FastGraph<String> graph = new FastUndirectedWeightedGraph<>();
        users.forEach(graph::addNode);

        // Generate a set of links between nodes (including autoloops)
        List<Tuple3<String, String, Double>> prefs = new ArrayList<>();

        int numPref = IntStream.range(0, N).map(i ->
        {
            String u = users.get(i);
            int K = rnd.nextInt(Math.min(500, N - i));
            Set<Integer> set = new HashSet<>();
            rnd.ints(K, 0, N - i).forEach(k ->
            {
                int aux = k;
                while (set.contains(aux))
                {
                    aux = (aux + 1) % (N - i);
                }
                set.add(aux);
                // Edge weights will be between 0 and 5
                prefs.add(new Tuple3<>(u, users.get(aux + i), 5 * rnd.nextDouble()));
            });

            return K;
        }).sum();

        // Add the different edges: since we have created numPref different edges, they will all appear in the graph.
        prefs.forEach(tuple -> graph.addEdge(tuple.v1, tuple.v2, tuple.v3, EdgeType.getDefaultValue(), false));

        // Check the size of the graph, in terms of nodes and edges
        assertEquals(graph.getVertexCount(), N);
        assertEquals(graph.getEdgeCount(), numPref);

        // Check that the graph has registered the edges, with their corresponding weights.
        if(numPref > 0)
        rnd.ints(1000, 0, numPref).forEach(i ->
        {
            String user = prefs.get(i).v1;
            String item = prefs.get(i).v2;
            double val = prefs.get(i).v3;

            double optional = graph.getEdgeWeight(user, item);
            if (!EdgeWeight.isErrorValue(optional))
            {
                assertEquals(val, optional, 0.00001);
            }
            else
            {
                fail();
            }
        });

        List<Tuple3<String, String, Double>> extraPrefs = new ArrayList<>();
        int numExtraPref = IntStream.range(0, N).map(i ->
        {
            String u = users.get(i);
            Set<Integer> set = new HashSet<>();
            int auxCount = graph.getNeighbourNodes(u).filter(v -> graph.object2idx(v) >= i).mapToInt(v ->
            {
                set.add(graph.object2idx(v) - i);
                return 1;
            }).sum();

            if ((N - auxCount - i) > 0)
            {
                 int K = rnd.nextInt(Math.min(100, N - auxCount - i));
                 rnd.ints(K, 0, N - i).forEach(k ->
                {
                    int aux = k;
                    while (set.contains(aux))
                    {
                        aux = (aux + 1) % (N - i);
                    }
                    set.add(aux);
                    // Edge weights will be between 0 and 5
                    extraPrefs.add(new Tuple3<>(u, users.get(aux + i), 5 * rnd.nextDouble()));
                });

                return K;
            }
            return 0;
        }).sum();

        // We add the new preferences
        extraPrefs.forEach(tuple -> graph.addEdge(tuple.v1, tuple.v2, tuple.v3, EdgeType.getDefaultValue(), false));

        // If the number of extra preferences is greater than zero, then, check that the links were correctly added.
        if (numExtraPref > 0)
        {
            assertEquals(graph.getEdgeCount(), numPref + numExtraPref);
            rnd.ints(1000, 0, numExtraPref).forEach(i ->
            {
                String user = extraPrefs.get(i).v1;
                String item = extraPrefs.get(i).v2;
                double val = extraPrefs.get(i).v3;

                double optional = graph.getEdgeWeight(user, item);
                if (!EdgeWeight.isErrorValue(optional))
                {
                    assertEquals(val, optional, 0.00001);
                }
                else
                {
                    fail();
                }
            });
        }

        // Now, let's check what happens when we try to access some invalid nodes / edges
        List<Tuple3<String, String, Double>> falsePrefs = new ArrayList<>();
        // We generate false links towards non-existing users.
        int falsePref = users.stream().mapToInt(u ->
        {
            int K = rnd.nextInt(Math.min(50, N));
            Set<Integer> set = new HashSet<>();
            rnd.ints(K, 0, 200).forEach(k ->
            {
                int aux = k;
                while (set.contains(aux))
                {
                    aux = (aux + 1) % 200;
                }
                set.add(aux);
                falsePrefs.add(new Tuple3<>(u, Integer.toString(aux + N), 5 * rnd.nextDouble()));
            });

            return K;
        }).sum();

        // Now, we try to add all the links to the graph.
        falsePrefs.forEach(tuple -> graph.addEdge(tuple.v1, tuple.v2, tuple.v3, EdgeType.getDefaultValue(), false));
        // and check that everything is as it should be: (the number of edges and node does not change)
        assertEquals(graph.getVertexCount(), N);
        assertEquals(graph.getEdgeCount(), numPref + numExtraPref);

        // We check that none of the false links appear in the graph.
        if(falsePref > 0)
        {
            rnd.ints(1000, 0, falsePref).forEach(i ->
            {
                String user = falsePrefs.get(i).v1;
                String item = falsePrefs.get(i).v2;
                double val = falsePrefs.get(i).v3;

                double optional = graph.getEdgeWeight(user, item);
                assertEquals(optional, EdgeWeight.getErrorValue(), 0.00001);
            });

            // Now, we do add the new users and edges.
            List<String> extraUsers = IntStream.range(N, N + 200).mapToObj(Integer::toString).collect(toList());
            Collections.shuffle(extraUsers);
            extraUsers.forEach(graph::addNode);

            falsePrefs.forEach(t -> graph.addEdge(t.v1, t.v2, t.v3, EdgeType.getDefaultValue(), false));

            // We check the new number of edges and nodes.
            assertEquals(graph.getVertexCount(), N + 200);
            assertEquals(graph.getEdgeCount(), numPref + numExtraPref + falsePref);
            // and whether the new edges are in the network or not.
            rnd.ints(1000, 0, falsePref).forEach(i ->
            {
                String user = falsePrefs.get(i).v1;
                String item = falsePrefs.get(i).v2;
                double val = falsePrefs.get(i).v3;

                double optional = graph.getEdgeWeight(user, item);
                assertEquals(optional, val, 0.00001);
            });

            // We delete some users from the graph. We delete every user which was added before (N, N+200)
            Collections.shuffle(extraUsers);
            extraUsers.forEach(u -> assertTrue(graph.removeNode(u)));
        }
        // We check that the graph has the same users and edges before the deletion.
        assertEquals(graph.getVertexCount(), N);
        assertEquals(graph.getEdgeCount(), numPref + numExtraPref);

        // Finally, we select some of the links in the graph for deletion:
        List<Tuple2<String, String>> prefsToDelete = new ArrayList<>();
        int numDeleted = users.stream().mapToInt(u ->
        {
            if(graph.getAdjacentNodesCount(u) > 0)
            {
                int K = rnd.nextInt(Math.min(20, graph.getAdjacentNodesCount(u)));
                List<String> ls = new ArrayList<>();
                graph.getAdjacentNodes(u).forEach(ls::add);
                Collections.shuffle(ls);

                ls.subList(0, K).forEach(pref -> prefsToDelete.add(new Tuple2<>(u, pref)));
                return K;
            }
            return 0;
        }).sum();

        // We remove them
        int correction = prefsToDelete.stream().mapToInt(pref -> graph.removeEdge(pref.v1, pref.v2) ? 0 : 1).sum();

        // We check that the edges have been correctly deleted.
        assertEquals(graph.getVertexCount(), N);
        assertEquals(graph.getEdgeCount(), numPref + numExtraPref - numDeleted + correction);

        if(numDeleted > 0)
        // Check that the links do not appear in the graph.
            rnd.ints(1000, 0, numDeleted).forEach(i ->
            {
                String user = prefsToDelete.get(i).v1;
                String item = prefsToDelete.get(i).v2;

                double optional = graph.getEdgeWeight(user, item);
                assertEquals(optional, EdgeWeight.getErrorValue(), 0.00001);
            });
    }

    /**
     * Tests the addition of nodes to the graph.
     */
    @Test
    public void nodeAddition()
    {
        // Build the graph
        UndirectedGraph<Integer> dg = new FastUndirectedWeightedGraph<>();
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
        UndirectedGraph<Integer> dg = new FastUndirectedWeightedGraph<>();
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
        assertTrue(dg.containsEdge(3, 2));

        // Check existent edges
        assertTrue(dg.containsEdge(1, 2));
        assertTrue(dg.containsEdge(1, 3));
        assertTrue(dg.containsEdge(2, 3));
        assertTrue(dg.containsEdge(2, 1));
        assertTrue(dg.containsEdge(3, 1));
        assertTrue(dg.containsEdge(3, 2));

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
        assertFalse(dg.addEdge(2, 1));
        assertTrue(dg.containsEdge(2, 1));
        assertEquals(5L, dg.getEdgeCount());

        assertTrue(dg.addEdge(2, 2));
        assertEquals(6L, dg.getEdgeCount());
    }

    /**
     * Test the weights of the edges
     */
    @Test
    public void weights()
    {
        // Build the graph
        UndirectedGraph<Integer> dg = new FastUndirectedWeightedGraph<>();
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);

        // Add some edges with already existing nodes
        dg.addEdge(1, 2, 4.0, 1);
        dg.addEdge(1, 2, 4.0);
        dg.addEdge(1, 3, 5.0);
        dg.addEdge(2, 3);

        // Check individually
        assertEquals(4.0, dg.getEdgeWeight(1, 2), 0.001);
        assertEquals(5.0, dg.getEdgeWeight(1, 3), 0.001);
        assertEquals(1.0, dg.getEdgeWeight(2, 3), 0.001);

        // Recover each weight and check (adjacent edges)
        List<Weight<Integer, Double>> weights = dg.getAdjacentNodesWeights(1).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(2))
            {
                assertEquals(4.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(5.0, weight.getValue(), 0.001);
            }
        }

        weights = dg.getAdjacentNodesWeights(2).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(1))
            {
                assertEquals(4.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(1.0, weight.getValue(), 0.001);
            }
        }

        weights = dg.getAdjacentNodesWeights(3).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(1))
            {
                assertEquals(5.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(1.0, weight.getValue(), 0.001);
            }
        }

        // Recover each weight and check (incident edges)
        weights = dg.getIncidentNodesWeights(1).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(2))
            {
                assertEquals(4.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(5.0, weight.getValue(), 0.001);
            }
        }

        weights = dg.getIncidentNodesWeights(2).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(1))
            {
                assertEquals(4.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(1.0, weight.getValue(), 0.001);
            }
        }

        weights = dg.getIncidentNodesWeights(3).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(1))
            {
                assertEquals(5.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(1.0, weight.getValue(), 0.001);
            }
        }

        // Recover each weight and check (all edges)
        weights = dg.getNeighbourNodesWeights(1).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(2))
            {
                assertEquals(4.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(5.0, weight.getValue(), 0.001);
            }
        }

        weights = dg.getNeighbourNodesWeights(2).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(1))
            {
                assertEquals(4.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(1.0, weight.getValue(), 0.001);
            }
        }

        weights = dg.getNeighbourNodesWeights(3).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(1))
            {
                assertEquals(5.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(1.0, weight.getValue(), 0.001);
            }
        }
    }

    /**
     * Test the weights of the edges
     */
    @Test
    public void types()
    {
        // Build the graph
        UndirectedGraph<Integer> dg = new FastUndirectedWeightedGraph<>();
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
        assertEquals(1, dg.getEdgeType(2, 1));
        assertEquals(2, dg.getEdgeType(3, 1));
        assertEquals(3, dg.getEdgeType(3, 2));

        List<Weight<Integer, Integer>> types = dg.getNeighbourNodesTypes(1).collect(toList());

        for (Weight<Integer, Integer> type : types)
        {
            if (type.getIdx().equals(2))
            {
                assertEquals(1, (int) type.getValue());
            }
            else
            {
                assertEquals(2, (int) type.getValue());
            }

        }

        types = dg.getNeighbourNodesTypes(2).collect(toList());
        for (Weight<Integer, Integer> type : types)
        {
            if (type.getIdx().equals(1))
            {
                assertEquals(1, (int) type.getValue());
            }
            else
            {
                assertEquals(3, (int) type.getValue());
            }

        }

        types = dg.getNeighbourNodesTypes(3).collect(toList());
        for (Weight<Integer, Integer> type : types)
        {
            if (type.getIdx().equals(1))
            {
                assertEquals(2, (int) type.getValue());
            }
            else
            {
                assertEquals(3, (int) type.getValue());
            }

        }

        types = dg.getAdjacentNodesTypes(1).collect(toList());

        for (Weight<Integer, Integer> type : types)
        {
            if (type.getIdx().equals(2))
            {
                assertEquals(1, (int) type.getValue());
            }
            else
            {
                assertEquals(2, (int) type.getValue());
            }

        }

        types = dg.getAdjacentNodesTypes(2).collect(toList());
        for (Weight<Integer, Integer> type : types)
        {
            if (type.getIdx().equals(1))
            {
                assertEquals(1, (int) type.getValue());
            }
            else
            {
                assertEquals(3, (int) type.getValue());
            }

        }

        types = dg.getAdjacentNodesTypes(3).collect(toList());
        for (Weight<Integer, Integer> type : types)
        {
            if (type.getIdx().equals(1))
            {
                assertEquals(2, (int) type.getValue());
            }
            else
            {
                assertEquals(3, (int) type.getValue());
            }

        }

        types = dg.getIncidentNodesTypes(1).collect(toList());

        for (Weight<Integer, Integer> type : types)
        {
            if (type.getIdx().equals(2))
            {
                assertEquals(1, (int) type.getValue());
            }
            else
            {
                assertEquals(2, (int) type.getValue());
            }

        }

        types = dg.getIncidentNodesTypes(2).collect(toList());
        for (Weight<Integer, Integer> type : types)
        {
            if (type.getIdx().equals(1))
            {
                assertEquals(1, (int) type.getValue());
            }
            else
            {
                assertEquals(3, (int) type.getValue());
            }

        }

        types = dg.getIncidentNodesTypes(3).collect(toList());
        for (Weight<Integer, Integer> type : types)
        {
            if (type.getIdx().equals(1))
            {
                assertEquals(2, (int) type.getValue());
            }
            else
            {
                assertEquals(3, (int) type.getValue());
            }

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
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);
        dg.addNode(4);
        dg.addNode(5);
        dg.addEdge(1, 2);
        dg.addEdge(1, 2);
        dg.addEdge(1, 3);
        dg.addEdge(2, 3);
        dg.addEdge(5, 1);

        // Check the in-degrees of the nodes
        assertEquals(dg.degree(1), 3);
        assertEquals(dg.degree(2), 2);
        assertEquals(dg.degree(3), 2);
        assertEquals(dg.degree(4), 0);
        assertEquals(dg.degree(5), 1);
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
        dg.addEdge(1, 2);
        dg.addEdge(1, 2);
        dg.addEdge(1, 3);
        dg.addEdge(2, 3);

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
        dg.addEdge(2, 1);

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

    /**
     * Test the weights of the edges
     */
    @Test
    public void updateWeights()
    {
        // Build the graph
        UndirectedGraph<Integer> dg = new FastUndirectedWeightedGraph<>();
        dg.addNode(1);
        dg.addNode(2);
        dg.addNode(3);

        // Add some edges with already existing nodes
        dg.addEdge(1, 2, 4.0, 1);
        dg.addEdge(1, 2, 4.0);
        dg.addEdge(1, 3, 5.0);
        dg.addEdge(2, 3);

        assertTrue(dg.updateEdgeWeight(1, 2, 5.0));
        assertTrue(dg.updateEdgeWeight(1, 3, 2.0));
        assertTrue(dg.updateEdgeWeight(2, 3, 6.0));
        assertTrue(dg.updateEdgeWeight(2, 1, 3.0));
        assertFalse(dg.updateEdgeWeight(1, 1, 3.0));
        assertFalse(dg.updateEdgeWeight(4, 1, 3.0));


        // Check individually
        assertEquals(3.0, dg.getEdgeWeight(1, 2), 0.001);
        assertEquals(2.0, dg.getEdgeWeight(1, 3), 0.001);
        assertEquals(6.0, dg.getEdgeWeight(2, 3), 0.001);

        // Recover each weight and check (adjacent edges)
        List<Weight<Integer, Double>> weights = dg.getAdjacentNodesWeights(1).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(2))
            {
                assertEquals(3.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(2.0, weight.getValue(), 0.001);
            }
        }

        weights = dg.getAdjacentNodesWeights(2).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(1))
            {
                assertEquals(3.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(6.0, weight.getValue(), 0.001);
            }
        }

        weights = dg.getAdjacentNodesWeights(3).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(1))
            {
                assertEquals(2.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(6.0, weight.getValue(), 0.001);
            }
        }

        // Recover each weight and check (incident edges)
        weights = dg.getIncidentNodesWeights(1).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(2))
            {
                assertEquals(3.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(2.0, weight.getValue(), 0.001);
            }
        }

        weights = dg.getIncidentNodesWeights(2).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(1))
            {
                assertEquals(3.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(6.0, weight.getValue(), 0.001);
            }
        }

        weights = dg.getIncidentNodesWeights(3).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(1))
            {
                assertEquals(2.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(6.0, weight.getValue(), 0.001);
            }
        }

        // Recover each weight and check (all edges)
        weights = dg.getNeighbourNodesWeights(1).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(2))
            {
                assertEquals(3.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(2.0, weight.getValue(), 0.001);
            }
        }

        weights = dg.getNeighbourNodesWeights(2).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(1))
            {
                assertEquals(3.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(6.0, weight.getValue(), 0.001);
            }
        }

        weights = dg.getNeighbourNodesWeights(3).collect(toList());
        for (Weight<Integer, Double> weight : weights)
        {
            if (weight.getIdx().equals(1))
            {
                assertEquals(2.0, weight.getValue(), 0.001);
            }
            else
            {
                assertEquals(6.0, weight.getValue(), 0.001);
            }
        }
    }

}
