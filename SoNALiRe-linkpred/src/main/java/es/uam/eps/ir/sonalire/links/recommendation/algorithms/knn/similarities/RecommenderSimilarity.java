/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.knn.similarities;

import es.uam.eps.ir.ranksys.fast.FastRecommendation;
import es.uam.eps.ir.ranksys.rec.fast.FastRecommender;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.function.IntToDoubleFunction;
import java.util.stream.Stream;

/**
 * Class which applies any contact recommendation algorithm as a graph
 * similarity for contact recommendation / link prediction.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class RecommenderSimilarity extends GraphSimilarity
{
    /**
     * A fast recommendation algorithm.
     */
    private final FastRecommender<?,?> recommender;

    /**
     * Constructor.
     *
     * @param graph the social network graph.
     */
    public RecommenderSimilarity(FastGraph<?> graph, FastRecommender<?,?> recommender)
    {
        super(graph);
        this.recommender = recommender;
    }

    @Override
    public IntToDoubleFunction similarity(int uidx)
    {
        FastRecommendation sim = recommender.getRecommendation(uidx);
        Int2DoubleMap map = new Int2DoubleOpenHashMap();
        map.defaultReturnValue(Double.NEGATIVE_INFINITY);

        sim.getIidxs().forEach(i -> map.put(i.v1, i.v2));

        return (vidx) -> map.getOrDefault(vidx, map.defaultReturnValue());
    }

    @Override
    public Stream<Tuple2id> similarElems(int uidx)
    {
        FastRecommendation sim = recommender.getRecommendation(uidx);
        return sim.getIidxs().stream();
    }
}