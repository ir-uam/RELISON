/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.recommendation.sampling;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.links.data.letor.sampling.DistanceTwoIndividualSampler;
import es.uam.eps.ir.sonalire.links.data.letor.sampling.IndividualSampler;
import org.jooq.lambda.tuple.Tuple2;

/**
 * Class for configuring a sampling approach which takes all nodes at distance two from the target user.
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * 
 * @param <U> type of the users.
 */
public class DistanceTwoIndividualSamplerConfigurator<U> implements IndividualSamplingAlgorithmConfigurator<U>
{
    /**
     * Identifier of the neighbor orientation for the sampled user.
     */
    private final static String USEL = "uSel";
    /**
     * Identifier of the neighbor orientation for the objective users.
     */
    private final static String VSEL = "vSel";
    
    @Override
    public Tuple2<String, IndividualSampler<U>> grid(Parameters params, FastGraph<U> trainGraph, FastGraph<U> testGraph, FastPreferenceData<U,U> prefData)
    {
        EdgeOrientation uSel = params.getOrientationValue(USEL);
        EdgeOrientation vSel = params.getOrientationValue(VSEL);
        
        if(uSel == null || vSel == null) return null;
        String name = IndividualSamplingAlgorithmIdentifiers.DISTANCETWO + "_" + uSel + "_" + vSel;
        IndividualSampler<U> sampler = new DistanceTwoIndividualSampler<>(trainGraph, uSel, vSel);
        return new Tuple2<>(name, sampler);
    }

    @Override
    public Tuple2<String, IndividualSamplerFunction<U>> grid(Parameters params)
    {
        EdgeOrientation uSel = params.getOrientationValue(USEL);
        EdgeOrientation vSel = params.getOrientationValue(VSEL);
        
        if(uSel == null || vSel == null) return null;
        String name = IndividualSamplingAlgorithmIdentifiers.DISTANCETWO + "_" + uSel + "_" + vSel;
        IndividualSamplerFunction<U> function = (FastGraph<U> trainGraph, FastGraph<U> testGraph, FastPreferenceData<U,U> prefData) ->
                new DistanceTwoIndividualSampler<>(trainGraph, uSel, vSel);
        
        return new Tuple2<>(name, function);
    }
    
    
    
}
