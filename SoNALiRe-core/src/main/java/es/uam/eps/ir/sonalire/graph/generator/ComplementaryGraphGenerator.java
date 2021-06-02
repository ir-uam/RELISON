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
import es.uam.eps.ir.sonalire.graph.complementary.DirectedUnweightedComplementaryGraph;
import es.uam.eps.ir.sonalire.graph.complementary.DirectedWeightedComplementaryGraph;
import es.uam.eps.ir.sonalire.graph.complementary.UndirectedUnweightedComplementaryGraph;
import es.uam.eps.ir.sonalire.graph.complementary.UndirectedWeightedComplementaryGraph;
import es.uam.eps.ir.sonalire.graph.generator.exception.GeneratorNotConfiguredException;

/**
 * Generates complementary graphs.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ComplementaryGraphGenerator<U> implements GraphGenerator<U>
{
    /**
     * Indicates if the graph has been configured
     */
    boolean configured = false;
    /**
     * The original graph
     */
    private Graph<U> graph;
    /**
     * Indicates if the graph is directed
     */
    private boolean directed;
    /**
     * Indicates if the graph is weighted
     */
    private boolean weighted;

    @Override
    public void configure(Object... configuration)
    {
        if (!(configuration == null) && configuration.length == 1)
        {
            Graph<U> g = (Graph<U>) configuration[0];

            this.configure(g);
        }
        else
        {
            configured = false;
        }
    }

    /**
     * Configures the generator.
     *
     * @param g Original graph.
     */
    public void configure(Graph<U> g)
    {
        if (g != null)
        {
            this.graph = g;
            this.directed = g.isDirected();
            this.weighted = g.isWeighted();
            configured = true;
        }
        else
        {
            configured = false;
        }

    }

    @Override
    public Graph<U> generate() throws GeneratorNotConfiguredException
    {
        if (!configured)
        {
            throw new GeneratorNotConfiguredException("The generator was not configured");
        }

        Graph<U> g;
        if (directed)
        {
            if (weighted)
            {
                g = new DirectedWeightedComplementaryGraph<>(graph);
            }
            else
            {
                g = new DirectedUnweightedComplementaryGraph<>(graph);
            }
        }
        else if (weighted)
        {
            g = new UndirectedWeightedComplementaryGraph<>(graph);
        }
        else
        {
            g = new UndirectedUnweightedComplementaryGraph<>(graph);
        }

        return g;
    }

}
