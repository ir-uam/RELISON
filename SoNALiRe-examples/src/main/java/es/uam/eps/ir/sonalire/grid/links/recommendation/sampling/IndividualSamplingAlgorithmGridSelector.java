/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autonoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.recommendation.sampling;


import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.links.data.letor.sampling.IndividualSampler;
import org.jooq.lambda.tuple.Tuple2;
import org.ranksys.formats.parsing.Parser;

/**
 * Given a grid, this class obtains a sampling algorithm, to apply to every target user for a
 * link prediction / contact recommendation approach.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class IndividualSamplingAlgorithmGridSelector<U>
{   
    /**
     * Obtains a configured individual sampling algorithm.
     *
     * @param name          the name of the algorithm.
     * @param param         the set of parameters of the algorithm.
     * @param graph         the graph to sample.
     * @param extraEdges    a graph useful for the sample (for example, a test graph).
     * @param prefData      preference data representing the graph.
     * @param uParser       a parser for reading users from text.
     * @return a pair containing the name of the algorithm and its configured object.
     */
    public Tuple2<String, IndividualSampler<U>> getIndividualSamplingAlgorithm(String name, Parameters param, FastGraph<U> graph, FastGraph<U> extraEdges, FastPreferenceData<U,U> prefData, Parser<U> uParser)
    {
        IndividualSamplingAlgorithmConfigurator<U> configurator = this.getConfigurator(name,uParser);
        if(configurator != null)
        {
            return configurator.grid(param, graph, extraEdges, prefData);
        }
        return null;
    }
    
    /**
     * Obtains a function for obtaining a configured individual sampling algorithm.
     *
     * @param name      the name of the algorithm.
     * @param param     the parameters of the algorithm.
     * @param uParser   a parser for reading users from text.
     *
     * @return a pair containing the name of the algorithm and a function to retrieve
     * a configured sampler.
     */
    public Tuple2<String, IndividualSamplerFunction<U>> getIndividualSamplingAlgorithm(String name, Parameters param, Parser<U> uParser)
    {
        IndividualSamplingAlgorithmConfigurator<U> configurator = this.getConfigurator(name, uParser);
        if(configurator != null)
        {
            return configurator.grid(param);
        }
        return null;
    }
    
    /**
     * Obtains a configurator for an individual sampling algorithm.
     *
     * @param name the name of the algorithm.
     * @return the configurator if exists, null otherwise.
     */
    public IndividualSamplingAlgorithmConfigurator<U> getConfigurator(String name, Parser<U> uParser)
    {
        return switch (name)
        {
            case IndividualSamplingAlgorithmIdentifiers.ALL -> new AllSamplerConfigurator<>();
            case IndividualSamplingAlgorithmIdentifiers.DISTANCETWO -> new DistanceTwoIndividualSamplerConfigurator<>();
            case IndividualSamplingAlgorithmIdentifiers.DISTANCETWOLP -> new DistanceTwoLinkPredictionIndividualSamplerConfigurator<>();
            case IndividualSamplingAlgorithmIdentifiers.LINKPRED -> new LinkPredictionSamplerConfigurator<>();
            case IndividualSamplingAlgorithmIdentifiers.RECOMMENDER -> new RecommenderIndividualSamplerConfigurator<>(uParser);
            default -> null;
        };
    }
}
