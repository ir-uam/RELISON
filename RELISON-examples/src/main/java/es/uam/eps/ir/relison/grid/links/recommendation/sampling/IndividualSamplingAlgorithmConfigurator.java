/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.sampling;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.grid.Parameters;
import es.uam.eps.ir.relison.links.data.letor.sampling.IndividualSampler;
import org.jooq.lambda.tuple.Tuple2;


/**
 * Definition of the classes for obtaining the parameters for different sampling approaches.
 * Such sampling approaches are applied to each different user in the network separately.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public interface IndividualSamplingAlgorithmConfigurator<U>
{
    /**
     * Obtains an individual sampling algorithm.
     * @param params        the parameter selection for the approach.
     * @param trainGraph    the original graph.
     * @param testGraph     a test graph to consider when sampling.
     * @param prefData      the training preference data.
     * @return a pair containing the name of the algorithm and the configured algorithm.
     */
    Tuple2<String, IndividualSampler<U>> grid(Parameters params, FastGraph<U> trainGraph, FastGraph<U> testGraph, FastPreferenceData<U, U> prefData);
    
    /**
     * Obtains a function for obtaining an individual sampling algorithm.
     * @param params the sampling parameters.
     * @return a pair containing the name of the algorithm and a function for obtaining the configured algorithm.
     */
    Tuple2<String, IndividualSamplerFunction<U>> grid(Parameters params);
}
