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
import es.uam.eps.ir.sonalire.links.data.letor.sampling.DistanceTwoLinkPredictionIndividualSampler;
import es.uam.eps.ir.sonalire.links.data.letor.sampling.IndividualSampler;
import org.jooq.lambda.tuple.Tuple2;

/**
 * Class for selecting nodes at distance 2 from the target user, considering the usual approach in link prediction,
 * i.e. we first take all the nodes at distance 2 from the target user in the training set. Then, we take all nodes in that
 * set with whom the target user has created a link in the test set, and we randomly select the same number of negative
 * examples.
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * 
 * @param <U> type of the users.
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
    public Tuple2<String, IndividualSampler<U>> grid(Parameters params, FastGraph<U> trainGraph, FastGraph<U> testGraph, FastPreferenceData<U,U> prefData)
    {
        EdgeOrientation uSel = params.getOrientationValue(USEL);
        EdgeOrientation vSel = params.getOrientationValue(VSEL);
        
        if(uSel == null || vSel == null) return null;
        String name = IndividualSamplingAlgorithmIdentifiers.DISTANCETWOLP + "_" + uSel + "_" + vSel;
        IndividualSampler<U> sampler = new DistanceTwoLinkPredictionIndividualSampler<>(trainGraph, testGraph, uSel, vSel);
        return new Tuple2<>(name, sampler);
    }

    @Override
    public Tuple2<String, IndividualSamplerFunction<U>> grid(Parameters params)
    {
        EdgeOrientation uSel = params.getOrientationValue(USEL);
        EdgeOrientation vSel = params.getOrientationValue(VSEL);
        
        if(uSel == null || vSel == null) return null;
        String name = IndividualSamplingAlgorithmIdentifiers.DISTANCETWOLP + "_" + uSel + "_" + vSel;
        
        IndividualSamplerFunction<U> function = (FastGraph<U> graph, FastGraph<U> extraEdges, FastPreferenceData<U,U> prefData) ->
            new DistanceTwoLinkPredictionIndividualSampler<>(graph, extraEdges, uSel, vSel);
        
        return new Tuple2<>(name, function);
    }
    
    
    
}
