/* 
 *  Copyright (C) 2015 Information Retrieval Group at Universidad AutÃ³noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.generator;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.socialranksys.graph.multigraph.fast.FastDirectedUnweightedMultiGraph;
import es.uam.eps.ir.socialranksys.graph.multigraph.fast.FastDirectedWeightedMultiGraph;
import es.uam.eps.ir.socialranksys.graph.multigraph.fast.FastUndirectedUnweightedMultiGraph;
import es.uam.eps.ir.socialranksys.graph.multigraph.fast.FastUndirectedWeightedMultiGraph;

/**
 * Creates an empty multigraph
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the nodes.
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
        if(!(configuration == null) && configuration.length == 2)
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
        if(!configured)
            throw new GeneratorNotConfiguredException("Empty graph: The generator was not configured");
        
        Graph<U> graph;
        if(directed)
            if(weighted)
                graph = new FastDirectedWeightedMultiGraph<>();
            else
                graph = new FastDirectedUnweightedMultiGraph<>();
        else
            if(weighted)
                graph = new FastUndirectedWeightedMultiGraph<>();
            else
                graph = new FastUndirectedUnweightedMultiGraph<>();
        
        return graph; 
    }
    
}
