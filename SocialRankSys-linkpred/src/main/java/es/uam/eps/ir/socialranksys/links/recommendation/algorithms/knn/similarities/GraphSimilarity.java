/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.knn.similarities;


import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;

/**
 * Abstract class for representing similarities extracted from graph properties.
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 */
public abstract class GraphSimilarity implements Similarity
{
    /**
     * The graph.
     */
    protected final FastGraph<?> graph;
    
    /**
     * Constructor.
     * @param graph the social network graph. 
     */
    public GraphSimilarity(FastGraph<?> graph)
    {
        this.graph = graph;
    }
    
    
}
