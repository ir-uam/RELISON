/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.sampling;


import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.data.letor.sampling.IndividualSampler;

/**
 * Functions for obtaining an sampling approach that, given a user, selects
 * a group of candidate links for the prediction.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
@FunctionalInterface
public interface IndividualSamplerFunction<U> 
{
    /**
     * Obtains an individual sampler
     * @param graph         the graph for obtaining the sample.
     * @param extraEdges    a graph containing a set of additional edges (for instance, a test graph).
     * @param prefData      preference data for the sampler.
     * @return the individual sampler.
     */
    IndividualSampler<U> apply(FastGraph<U> graph, FastGraph<U> extraEdges, FastPreferenceData<U, U> prefData);
}
