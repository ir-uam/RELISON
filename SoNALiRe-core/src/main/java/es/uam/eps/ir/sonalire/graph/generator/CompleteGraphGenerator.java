/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.graph.generator;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.sonalire.utils.generator.Generator;

/**
 * Class for generating complete graphs.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CompleteGraphGenerator<U> implements GraphGenerator<U>
{
    /**
     * Number of nodes of the generated graphs.
     */
    private int numNodes;
    /**
     * User generator.
     */
    private Generator<U> generator;
    /**
     * Indicates if the generated graphs are directed or undirected.
     */
    private boolean directed;
    /**
     * Indicates if autoloops are also allowed.
     */
    private boolean autoloops;
    /**
     * True if the graph has already been configured, false if not.
     */
    private boolean configured;

    @Override
    public void configure(Object... configuration)
    {
        if (!(configuration == null) && configuration.length == 3)
        {
            boolean auxDirected = (boolean) configuration[0];
            int auxNumNodes = (int) configuration[1];
            Generator<U> auxGenerator = (Generator<U>) configuration[2];

            this.configure(auxDirected, auxNumNodes, auxGenerator);
        }
        else if (!(configuration == null) && configuration.length == 4)
        {
            boolean auxDirected = (boolean) configuration[0];
            int auxNumNodes = (int) configuration[1];
            boolean auxAutoloops = (boolean) configuration[2];
            Generator<U> auxGenerator = (Generator<U>) configuration[3];

            this.configure(auxDirected, auxNumNodes, auxAutoloops, auxGenerator);
        }
        else
        {
            this.configured = false;
        }

    }

    /**
     * Configures the graph.
     *
     * @param directed  Indicates if the node is directed or not.
     * @param numNodes  Number of nodes of the graph.
     * @param generator Object that automatically creates the indicated number of nodes.
     */
    public void configure(boolean directed, int numNodes, Generator<U> generator)
    {
        this.numNodes = numNodes;
        this.directed = directed;
        this.generator = generator;
        this.configured = true;
        this.autoloops = false;
    }

    /**
     * Configures the graph.
     *
     * @param directed  Indicates if the node is directed or not.
     * @param numNodes  Number of nodes of the graph.
     * @param autoloops Indicates if the graph allows autoloops or not.
     * @param generator Object that automatically creates the indicated number of nodes.
     */
    public void configure(boolean directed, int numNodes, boolean autoloops, Generator<U> generator)
    {
        this.numNodes = numNodes;
        this.directed = directed;
        this.generator = generator;
        this.autoloops = autoloops;
        this.configured = true;
    }

    @Override
    public Graph<U> generate() throws GeneratorNotConfiguredException
    {
        if (!this.configured)
        {
            throw new GeneratorNotConfiguredException("Barabási-Albert: The model was not configured");
        }

        EmptyGraphGenerator<U> gen = new EmptyGraphGenerator<>();
        gen.configure(this.directed, false);
        Graph<U> graph = gen.generate();
        this.generator.reset();

        for (int i = 0; i < this.numNodes; ++i)
        {
            U user = this.generator.generate();
            graph.addNode(user);
        }

        graph.getAllNodes().forEach(u -> graph.getAllNodes().forEach(v ->
                                                                     {
                                                                         if (this.autoloops || !u.equals(v))
                                                                         {
                                                                             graph.addEdge(u, v);
                                                                         }
                                                                     }));

        return graph;

    }

}
