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
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.links.data.letor.sampling.DistanceTwoLinkPredictionIndividualSampler;
import es.uam.eps.ir.socialranksys.links.data.letor.sampling.IndividualSampler;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;

/**
 * Class for configuring distance two individual samplers.
 * 
 * @author Javier Sanz-Cruzado Puig
 * 
 * @param <U> Type of the users.
 */
public class DistanceTwoLinkPredictionIndividualSamplerConfigurator<U> implements IndividualSamplingAlgorithmConfigurator<U>
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
    public Tuple2oo<String, IndividualSampler<U>> grid(Parameters params, FastGraph<U> trainGraph, FastGraph<U> testGraph, FastPreferenceData<U,U> prefData)
    {
        EdgeOrientation uSel = params.getOrientationValue(USEL);
        EdgeOrientation vSel = params.getOrientationValue(VSEL);
        
        if(uSel == null || vSel == null) return null;
        String name = IndividualSamplingAlgorithmIdentifiers.DISTANCETWOLP + "_" + uSel + "_" + vSel;
        IndividualSampler<U> sampler = new DistanceTwoLinkPredictionIndividualSampler<>(trainGraph, testGraph, uSel, vSel);
        return new Tuple2oo<>(name, sampler);
    }

    @Override
    public Tuple2oo<String, IndividualSamplerFunction<U>> grid(Parameters params)
    {
        EdgeOrientation uSel = params.getOrientationValue(USEL);
        EdgeOrientation vSel = params.getOrientationValue(VSEL);
        
        if(uSel == null || vSel == null) return null;
        String name = IndividualSamplingAlgorithmIdentifiers.DISTANCETWOLP + "_" + uSel + "_" + vSel;
        
        IndividualSamplerFunction<U> function = (FastGraph<U> graph, FastGraph<U> extraEdges, FastPreferenceData<U,U> prefData) ->
            new DistanceTwoLinkPredictionIndividualSampler<>(graph, extraEdges, uSel, vSel);
        
        return new Tuple2oo<>(name, function);
    }
    
    
    
}
