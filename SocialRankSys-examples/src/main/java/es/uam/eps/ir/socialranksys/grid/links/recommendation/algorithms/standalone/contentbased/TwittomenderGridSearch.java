/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.contentbased;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.content.index.ForwardIndex;
import es.uam.eps.ir.socialranksys.content.index.individual.WrapperIndividualForwardContentIndex;
import es.uam.eps.ir.socialranksys.content.index.lucene.LuceneForwardIndex;
import es.uam.eps.ir.socialranksys.content.search.VSMSearchEngine;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.contentbased.TwittomenderRecommender;
import org.ranksys.formats.parsing.Parser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers.TWITTOMENDER;

/**
 * Grid search generator for Twittomender CB algorithm.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see TwittomenderRecommender
 */
public class TwittomenderGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for the route to the index.
     */
    private static final String INDEX = "index";

    /**
     * Parser for the user identifiers.
     */
    private final Parser<U> parser;

    /**
     * Constructor.
     * @param parser the parser for the user identifiers.
     */
    public TwittomenderGridSearch(Parser<U> parser)
    {
        this.parser = parser;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();

        List<String> indexRoutes = grid.getStringValues(INDEX);
        IntStream.range(0, indexRoutes.size()).forEach(i ->
            recs.put(TWITTOMENDER + "_index" + i, (graph, prefData) ->
            {
                String route = indexRoutes.get(i);
                ForwardIndex<U> contentIndex;
                try
                {
                    contentIndex = new LuceneForwardIndex<>(route, parser);
                    return new TwittomenderRecommender<>(graph, contentIndex, new VSMSearchEngine(contentIndex));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                return null;
            })
        );

        return recs;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();

        List<String> indexRoutes = grid.getStringValues(INDEX);

        IntStream.range(0, indexRoutes.size()).forEach(i ->
            recs.put(TWITTOMENDER + "_index" + i, () ->
            {
                String route = indexRoutes.get(i);
                ForwardIndex<U> contentIndex;
                WrapperIndividualForwardContentIndex<?, U> forwardIndex;
                try
                {
                    contentIndex = new LuceneForwardIndex<>(route, parser);
                    return new TwittomenderRecommender<>(graph, contentIndex, new VSMSearchEngine(contentIndex));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                return null;
            })
        );
        return recs;
    }
}
