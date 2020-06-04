/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.sampling;


import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.data.letor.sampling.IndividualSampler;

/**
 * Functions for retrieving an individual sampler from a grid.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
@FunctionalInterface
public interface IndividualSamplerFunction<U> 
{
    /**
     * Obtains an individual sampler
     * @param graph the graph for obtaining the sample.
     * @param extraEdges another edge with useful information.
     * @param prefData preference data for the sampler.
     * @return the individual sampler.
     */
    IndividualSampler<U> apply(FastGraph<U> graph, FastGraph<U> extraEdges, FastPreferenceData<U, U> prefData);
}
