/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.filter;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import es.uam.eps.socialranksys.diffusion.data.filter.DataFilter;
import org.ranksys.formats.parsing.Parser;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.diffusion.filter.FilterIdentifiers.*;

/**
 * Class that selects an individual filter from a grid.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * @param <I> Type of the items
 * @param <P> Type of the parameters
 */
public class FilterSelector<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Feature parser
     */
    private final Parser<P> parser;
    /**
     * Default value for the feature.
     */
    private final P defaultValue;
    /**
     * The test graph.
     */
    private final Graph<U> testGraph;
    
    /**
     * Constructor.
     * @param parser a parser for the features.
     * @param defaultValue the default value for the feature.
     */
    public FilterSelector(Parser<P> parser, P defaultValue)
    {
        this.parser = parser;
        this.defaultValue = defaultValue;
        this.testGraph = null;
    }
    
    /**
     * Constructor.
     * @param parser a parser for the features.
     * @param defaultValue the default value for the feature.
     * @param testGraph the test graph.
     */
    public FilterSelector(Parser<P> parser, P defaultValue, Graph<U> testGraph)
    {
        this.parser = parser;
        this.defaultValue = defaultValue;
        this.testGraph = testGraph;
    }
    
    /**
     * Selects a filter.
     * @param fgr Grid containing the parameters of the filter.
     * @return A pair containing the name and the selected filter.
     */
    public Tuple2oo<String, DataFilter<U,I,P>> select(FilterParamReader fgr)
    {
        String name = fgr.getName();
        FilterConfigurator<U,I,P> fgs;
        switch(name)
        {
            case BASIC:
                fgs = new BasicFilterConfigurator<>();
                break;
            case TAG:
                fgs = new ContainsInformationFeatureFilterConfigurator<>();
                break;
            case TAGSEL:
                fgs = new InformationFeatureSelectionFilterConfigurator<>(parser);
                break;
            case NUMTWEETS:
                fgs = new NumInformationPiecesFilterConfigurator<>();
                break;
            case EMPTYTAG:
                fgs = new EmptyFeatureFilterConfigurator<>(this.defaultValue);
                break;
            case CREATOR:
                fgs = new WithCreatorFilterConfigurator<>();
                break;
            case RELEVANTEDGES:
                fgs = new RelevantEdgesFilterConfigurator<>(this.testGraph);
                break;
            case MINIMUMPIECES:
                fgs = new MinimumFrequencyInformationFeatureFilterConfigurator<>();
                break;
            case ONLYREPR:
                fgs = new OnlyRepropagatedPiecesFilterConfigurator<>();
                break;
            default:
                return null;
        }
        
        DataFilter<U,I,P> filter = fgs.getFilter(fgr);
        return new Tuple2oo<>(name, filter);
    }
}
