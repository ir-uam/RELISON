/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.filter;

import es.uam.eps.ir.relison.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.grid.Parameters;
import org.jooq.lambda.tuple.Tuple2;
import org.ranksys.formats.parsing.Parser;

import java.io.Serializable;

import static es.uam.eps.ir.relison.grid.diffusion.filter.FilterIdentifiers.*;

/**
 * Class for selecting and configuring a filter for the information diffusion
 * process from a set of parameters.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the information pieces.
 * @param <F> Type of the features of the users / information pieces.
 */
public class FilterSelector<U extends Serializable,I extends Serializable, F>
{
    /**
     * Feature parser
     */
    private final Parser<F> parser;
    /**
     * Default value for the feature.
     */
    private final F defaultValue;
    /**
     * The test graph.
     */
    private final Graph<U> testGraph;
    
    /**
     * Constructor.
     * @param parser        a parser for the features.
     * @param defaultValue  the default value for the feature.
     */
    public FilterSelector(Parser<F> parser, F defaultValue)
    {
        this.parser = parser;
        this.defaultValue = defaultValue;
        this.testGraph = null;
    }
    
    /**
     * Constructor.
     * @param parser        a parser for the features.
     * @param defaultValue  the default value for the feature.
     * @param testGraph     the test graph.
     */
    public FilterSelector(Parser<F> parser, F defaultValue, Graph<U> testGraph)
    {
        this.parser = parser;
        this.defaultValue = defaultValue;
        this.testGraph = testGraph;
    }

    /**
     * Selects a filter.
     * @param name          the name of the filter.
     * @param parameters    the parameter configuration of the filter.
     * @return the configurator of the filter.
     */
    public Tuple2<String, DataFilter<U,I,F>> select(String name, Parameters parameters)
    {
        FilterConfigurator<U,I,F> fgs = this.selectConfig(name);
        DataFilter<U,I,F> filter = fgs.getFilter(parameters);

        return new Tuple2<>(name, filter);
    }
    
    /**
     * Selects a filter.
     * @param name the name of the filter.
     * @return the configurator of the filter.
     */
    public FilterConfigurator<U, I, F> selectConfig(String name)
    {
        return switch (name)
        {
            case BASIC -> new BasicFilterConfigurator<>();
            case TAG -> new ContainsInformationFeatureFilterConfigurator<>();
            case TAGSEL -> new InformationFeatureSelectionFilterConfigurator<>(parser);
            case NUMTWEETS -> new NumInformationPiecesFilterConfigurator<>();
            case EMPTYTAG -> new EmptyFeatureFilterConfigurator<>(this.defaultValue);
            case CREATOR -> new WithCreatorFilterConfigurator<>();
            case RELEVANTEDGES -> new RelevantEdgesFilterConfigurator<>(this.testGraph);
            case MINIMUMPIECES -> new MinimumFrequencyInformationFeatureFilterConfigurator<>();
            case ONLYREPR -> new OnlyRepropagatedPiecesFilterConfigurator<>();
            default -> null;
        };
    }
}
