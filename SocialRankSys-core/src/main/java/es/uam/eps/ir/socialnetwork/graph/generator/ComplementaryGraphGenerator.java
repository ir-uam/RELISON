/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph.generator;

import es.uam.eps.ir.socialnetwork.graph.Graph;
import es.uam.eps.ir.socialnetwork.graph.complementary.DirectedUnweightedComplementaryGraph;
import es.uam.eps.ir.socialnetwork.graph.complementary.DirectedWeightedComplementaryGraph;
import es.uam.eps.ir.socialnetwork.graph.complementary.UndirectedUnweightedComplementaryGraph;
import es.uam.eps.ir.socialnetwork.graph.complementary.UndirectedWeightedComplementaryGraph;

/**
 * Generates complementary graphs.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class ComplementaryGraphGenerator<U> implements GraphGenerator<U>
{
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
    /**
     * Indicates if the graph has been configured
     */
    boolean configured = false;
    
    @SuppressWarnings("unchecked")
    @Override
    public void configure(Object... configuration)
    {
        if(!(configuration == null) && configuration.length == 1)
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
     * @param g Original graph.
     */
    public void configure(Graph<U> g)
    {
        if(g != null)
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
        if(!configured)
        {
            throw new GeneratorNotConfiguredException("The generator was not configured");
        }
        
        Graph<U> g;
        if(directed)
            if(weighted)
                g = new DirectedWeightedComplementaryGraph<>(graph);
            else
                g = new DirectedUnweightedComplementaryGraph<>(graph);
        else
            if(weighted)
                g = new UndirectedWeightedComplementaryGraph<>(graph);
            else
                g = new UndirectedUnweightedComplementaryGraph<>(graph);
        
        return g; 
    }
    
}
