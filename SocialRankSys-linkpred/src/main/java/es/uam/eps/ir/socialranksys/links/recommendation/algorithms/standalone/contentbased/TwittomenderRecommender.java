/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.contentbased;

import es.uam.eps.ir.socialranksys.content.index.ForwardIndex;
import es.uam.eps.ir.socialranksys.content.index.freq.FreqVector;
import es.uam.eps.ir.socialranksys.content.search.SearchEngine;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.io.IOException;
import java.util.Map;

/**
 * Content-based recommendation algorithm, based on a TF-IDF scheme.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class TwittomenderRecommender<U> extends UserFastRankingRecommender<U>
{
    /**
     * A forward index.
     */
    private final ForwardIndex<U> index;
    /**
     * A search engine for accelerating the search.
     */
    private final SearchEngine engine;

    /**
     * Constructor
     * @param graph The training graph.
     * @param index Content index that contains information about users.
     */
    public TwittomenderRecommender(FastGraph<U> graph, ForwardIndex<U> index, SearchEngine engine)
    {
        super(graph);
        this.engine = engine;
        this.index = index;

    }

    @Override
    public Int2DoubleMap getScoresMap(int uIdx) 
    {
        Int2DoubleMap map = new Int2DoubleOpenHashMap();
        U u = this.uidx2user(uIdx);
        int uidxInIndex = index.getContentId(u);

        try
        {
            FreqVector uVector = index.getContentVector(uidxInIndex);
            if(uVector == null)
            {
                return map;
            }

            Map<Integer, Double> searchRes = this.engine.search(uVector);

            for(Map.Entry<Integer, Double> entry : searchRes.entrySet())
            {
                U v = index.getContent(entry.getKey());
                if(graph.containsVertex(v))
                {
                    int vidx = this.graph.object2idx(v);
                    map.put(vidx, entry.getValue().doubleValue());
                }
            }
        }
        catch (IOException ioe)
        {
            return map;
        }

        return map;
    }
}
