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
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridSelector;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.links.data.letor.sampling.IndividualSampler;
import es.uam.eps.ir.socialranksys.links.data.letor.sampling.RecommenderIndividualSampler;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import org.jooq.lambda.tuple.Tuple2;
import org.ranksys.formats.parsing.Parser;

import java.util.function.Supplier;


/**
 * Class for configuring distance two individual samplers.
 * 
 * @author Javier Sanz-Cruzado Puig
 * 
 * @param <U> Type of the users.
 */
public class RecommenderIndividualSamplerConfigurator<U> implements IndividualSamplingAlgorithmConfigurator<U>
{
    /**
     * Identifier for the recommendation algorithm.
     */
    private final static String REC = "rec";
    /**
     * Identifier for the cutoff of the recommendation.
     */
    private final static String K = "k";
    
    private final AlgorithmGridSelector<U> gridSelector;
    
    public RecommenderIndividualSamplerConfigurator(Parser<U> uParser)
    {
        this.gridSelector = new AlgorithmGridSelector<>(uParser);
    }
    
    
    @Override
    public Tuple2<String, IndividualSampler<U>> grid(Parameters params, FastGraph<U> trainGraph, FastGraph<U> testGraph, FastPreferenceData<U,U> prefData)
    {
        Tuple2oo<String, Parameters> rec = params.getParametersValue(REC);
        Integer k = params.getIntegerValue(K);
        
        if(rec == null || k == null) return null;

        Tuple2oo<String, Supplier<Recommender<U,U>>> recommender = this.gridSelector.getRecommender(rec.v1(), rec.v2(), trainGraph, prefData);
        if(recommender == null) return null;
        
        String name = IndividualSamplingAlgorithmIdentifiers.RECOMMENDER + "_" + recommender.v1() + "_k";
        IndividualSampler<U> sampler = new RecommenderIndividualSampler<>(trainGraph, recommender.v2().get(), k);
        return new Tuple2<>(name, sampler);
    }

    @Override
    public Tuple2<String, IndividualSamplerFunction<U>> grid(Parameters params)
    {
        Tuple2oo<String, Parameters> rec = params.getParametersValue(REC);
        Integer k = params.getIntegerValue(K);
        
        if(rec == null || k == null) return null;
        
        Tuple2oo<String, RecommendationAlgorithmFunction<U>> recommender = this.gridSelector.getRecommender(rec.v1(), rec.v2());

        String name = IndividualSamplingAlgorithmIdentifiers.RECOMMENDER + "_" + recommender.v1() + "_" + k;
        IndividualSamplerFunction<U> function = (FastGraph<U> graph, FastGraph<U> extraEdges, FastPreferenceData<U,U> prefData) ->
        {
            Recommender<U,U> alg = recommender.v2().apply(graph, prefData);
            if(alg == null) return null;
            return new RecommenderIndividualSampler<>(graph, alg, k);
        };
        
        return new Tuple2<>(name, function);
        
    }
    
    
    
}
