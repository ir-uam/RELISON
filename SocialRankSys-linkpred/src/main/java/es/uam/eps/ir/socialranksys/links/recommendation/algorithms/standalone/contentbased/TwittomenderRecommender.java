/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.contentbased;

import es.uam.eps.ir.socialranksys.content.ContentVector;
import es.uam.eps.ir.socialranksys.content.TextVector;
import es.uam.eps.ir.socialranksys.content.index.exceptions.WrongModeException;
import es.uam.eps.ir.socialranksys.content.index.twittomender.TwittomenderIndex;
import es.uam.eps.ir.socialranksys.content.index.weighting.TFIDFWeightingScheme;
import es.uam.eps.ir.socialranksys.content.similarities.ContentSimilarity;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.openide.util.Exceptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Content-based recommendation algorithm, based on a TF-IDF scheme.
 * 
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class TwittomenderRecommender<U> extends UserFastRankingRecommender<U>
{

    /**
     * Vectors of each user in the network.
     */
    private final Map<U, ContentVector<U>> vectors;
    /**
     * Score cache, for simplifying the calculus.
     */
    private final Map<U, Map<U,Double>> cache;
    /**
     * Similarity method to compare the different users.
     */
    private final ContentSimilarity similarity;
    
    /**
     * Constructor
     * @param graph The training graph.
     * @param index Content index that contains information about users.
     * @param sim Similarity to compare the different users.
     */
    public TwittomenderRecommender(FastGraph<U> graph, TwittomenderIndex<U> index, ContentSimilarity sim)
    {
        super(graph);
        
        this.vectors = new HashMap<>();
        this.cache = new HashMap<>();
        index.setReadMode();
        
        this.getGraph().getAllNodes().forEach(u -> 
        {
            try 
            {
                ContentVector<U> vector = index.readUser(u, new TFIDFWeightingScheme());
                vectors.put(u, vector);
            } 
            catch (WrongModeException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        });   
        
        this.uIndex.getAllUsers().forEach(u -> this.cache.put(u, new HashMap<>()));
        this.similarity = sim;
    }

    @Override
    public Int2DoubleMap getScoresMap(int uIdx) 
    {
        U u = this.uidx2user(uIdx);
        TextVector uVector = this.vectors.get(u).getVector();
        Int2DoubleMap scores = new Int2DoubleOpenHashMap();
        
        
        this.iIndex.getAllIidx().forEach(iIdx -> 
        {
            U v = this.iidx2item(iIdx);
            if(cache.get(u).containsKey(v))
            {
                scores.put(iIdx, this.cache.get(u).get(v).doubleValue());
            }
            else if(cache.get(v).containsKey(u))
            {
                scores.put(iIdx, this.cache.get(v).get(u).doubleValue());
            }
            else
            {
                TextVector vVector = this.vectors.get(v).getVector();
                double score = this.similarity.similarity(uVector, vVector);
                cache.get(u).put(v,score);
                cache.get(v).put(u,score);
               // cache.addRelation(iIdx, uIdx, score);
                scores.put(iIdx, score);
            }
        });
        
        return scores;
    }
}
