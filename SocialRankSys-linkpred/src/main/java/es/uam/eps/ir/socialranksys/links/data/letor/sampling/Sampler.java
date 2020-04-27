/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor.sampling;


import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Interface for all the classes that generate subsamples from a graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <V> type of the vertices
 */
public interface Sampler<V>
{   
    /**
     * Generates a subsample from a graph.
     * @param fullGraph Graph from which we extract the subsample
     * @param percentage Percentage of nodes/edges to extract (depending on the method)
     * @return the required subsample, or null if an error ocurred.
     */
    Graph<V> sample(Graph<V> fullGraph, double percentage);
    
    /**
     * Generates a subsample from a graph.
     * @param fullGraph Graph from which we extract the subsample
     * @param num The exact number of nodes/edges to extract
     * @return the required subsample, or null if an error ocurred.
     */
    Graph<V> sample(Graph<V> fullGraph, int num);
}
