/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data.letor.sampling;


import es.uam.eps.ir.sonalire.graph.Graph;

/**
 * Interface for all the classes that generate subsamples from a graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 *
 * @param <V> type of the vertices
 */
public interface Sampler<V>
{   
    /**
     * Generates a subsample from a graph.
     * @param fullGraph     graph from which we extract the subsample.
     * @param percentage    percentage of nodes/edges to extract (depending on the method).
     * @return the required subsample, or null if an error ocurred.
     */
    Graph<V> sample(Graph<V> fullGraph, double percentage);
    
    /**
     * Generates a subsample from a graph.
     * @param fullGraph     graph from which we extract the subsample.
     * @param num           the exact number of nodes/edges to extract.
     * @return the required subsample, or null if an error ocurred.
     */
    Graph<V> sample(Graph<V> fullGraph, int num);
}
