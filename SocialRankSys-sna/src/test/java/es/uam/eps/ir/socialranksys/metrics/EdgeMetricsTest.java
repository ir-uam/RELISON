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
import org.junit.*;

/**
 * Automated unit tests for edge metrics.
 *
 * @author Javier Sanz-Cruzado Puig
 */
public class EdgeMetricsTest
{

    /**
     * Directed Strongly Connected Graph
     */
    private DirectedGraph<Integer> directedStronglyConnected;
    /**
     * Directed Weakly Connected Graph
     */
    private DirectedGraph<Integer> directedWeaklyConnected;
    /**
     * Directed Unconnected Graph (not even weakly)
     */
    private DirectedGraph<Integer> directedNonConnected;
    /**
     * Undirected Connected Graph
     */
    private UndirectedGraph<Integer> undirectedConnected;
    /**
     * Undirected Unconnected Graph
     */
    private UndirectedGraph<Integer> undirectedNonConnected;
    /**
     * Directed complete graph
     */
    private DirectedGraph<Integer> directedComplete;
    /**
     * Directed empty graph (only nodes)
     */
    private DirectedGraph<Integer> directedEmpty;
    /**
     * Undirected complete graph
     */
    private UndirectedGraph<Integer> undirectedComplete;
    /**
     * Undirected empty graph
     */
    private UndirectedGraph<Integer> undirectedEmpty;


    public EdgeMetricsTest()
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

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    @Test
    public void embededness()
    {

    }

    @Test
    public void friendOfAFriend()
    {

    }
}
