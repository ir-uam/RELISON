/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.sampling;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.links.data.letor.sampling.AllSampler;
import es.uam.eps.ir.socialranksys.links.data.letor.sampling.IndividualSampler;
import org.jooq.lambda.tuple.Tuple2;

/**
 * Class for configuring a sampling approach which takes all nodes.
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * 
 * @param <U> type of the users.
 */
public class AllSamplerConfigurator<U> implements IndividualSamplingAlgorithmConfigurator<U>
{
    @Override
    public Tuple2<String, IndividualSampler<U>> grid(Parameters params, FastGraph<U> trainGraph, FastGraph<U> testGraph, FastPreferenceData<U,U> prefData)
    {
        String name = IndividualSamplingAlgorithmIdentifiers.ALL;
        IndividualSampler<U> sampler = new AllSampler<>(trainGraph);
        return new Tuple2<>(name, sampler);
    }

    @Override
    public Tuple2<String, IndividualSamplerFunction<U>> grid(Parameters params)
    {
        String name = IndividualSamplingAlgorithmIdentifiers.ALL;
        IndividualSamplerFunction<U> function = (FastGraph<U> trainGraph, FastGraph<U> testGraph, FastPreferenceData<U,U> prefData) ->
                new AllSampler<>(trainGraph);
        
        return new Tuple2<>(name, function);
    }
    
    
    
}
