/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.knn.similarities;

import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;

/**
 * Abstract class for representing similarities extracted from graph properties.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
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
