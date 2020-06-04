/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.sampling;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.links.data.letor.sampling.IndividualSampler;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;


/**
 * Class for performing the grid search for a given partition algorithm.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public interface IndividualSamplingAlgorithmConfigurator<U>
{
    /**
     * Obtains an individual sampling algorithm.
     * @param params The parameters of the sampling.
     * @param trainGraph the original graph.
     * @param testGraph a graph to take into account when sampling
     * @param prefData preference data.
     * @return a pair containing the name of the algorithm and the configured algorithm.
     */
    Tuple2oo<String, IndividualSampler<U>> grid(Parameters params, FastGraph<U> trainGraph, FastGraph<U> testGraph, FastPreferenceData<U, U> prefData);
    
    /**
     * Obtains a function for obtaining an individual sampling algorithm.
     * @param params The parameters of sampling.
     * @return a pair containing the name of the algorithm as well as the parameters of the algorithm.
     */
    Tuple2oo<String, IndividualSamplerFunction<U>> grid(Parameters params);
}
