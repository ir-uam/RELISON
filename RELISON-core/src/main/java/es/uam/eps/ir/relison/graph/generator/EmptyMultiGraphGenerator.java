/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.generator;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastDirectedUnweightedMultiGraph;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastDirectedWeightedMultiGraph;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastUndirectedUnweightedMultiGraph;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastUndirectedWeightedMultiGraph;

/**
 * Creates an empty multigraph
 *
 * @param <U> Type of the nodes.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class EmptyMultiGraphGenerator<U> implements GraphGenerator<U>
{
    /**
     * Indicates if the graph is directed.
     */
    private boolean directed;
    /**
     * Indicates if the generator has been configured.
     */
    private boolean configured = false;
    /**
     * Indicates if the graph is weighted.
     */
    private boolean weighted;

    @Override
    public void configure(Object... configuration)
    {
        if (!(configuration == null) && configuration.length == 2)
        {
            boolean auxDirected = (boolean) configuration[0];
            boolean auxWeighted = (boolean) configuration[1];


            this.configure(auxDirected, auxWeighted);
        }
        else
        {
            configured = false;
        }

    }

    /**
     * Configures the graph generator.
     *
     * @param directed indicates if the graph is directed
     * @param weighted indicates if the graph is weighted
     */
    public void configure(boolean directed, boolean weighted)
    {
        this.directed = directed;
        this.weighted = weighted;
        this.configured = true;
    }

    @Override
    public Graph<U> generate() throws GeneratorNotConfiguredException
    {
        if (!configured)
        {
            throw new GeneratorNotConfiguredException("Empty graph: The generator was not configured");
        }

        Graph<U> graph;
        if (directed)
        {
            if (weighted)
            {
                graph = new FastDirectedWeightedMultiGraph<>();
            }
            else
            {
                graph = new FastDirectedUnweightedMultiGraph<>();
            }
        }
        else if (weighted)
        {
            graph = new FastUndirectedWeightedMultiGraph<>();
        }
        else
        {
            graph = new FastUndirectedUnweightedMultiGraph<>();
        }

        return graph;
    }

}
