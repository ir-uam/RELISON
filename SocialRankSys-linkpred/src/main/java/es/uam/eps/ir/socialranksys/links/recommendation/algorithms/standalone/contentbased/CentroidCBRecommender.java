/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.contentbased;

import es.uam.eps.ir.socialranksys.content.index.freq.FreqVector;
import es.uam.eps.ir.socialranksys.content.index.freq.TermFreq;
import es.uam.eps.ir.socialranksys.content.index.individual.WrapperIndividualForwardContentIndex;
import es.uam.eps.ir.socialranksys.content.search.VSMSearchEngine;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2od;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Content-based recommendation algorithm, based on a TF-IDF scheme.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CentroidCBRecommender<U> extends UserFastRankingRecommender<U>
{
    /**
     * The centroids of the users.
     */
    private final Map<U, Map<String, Double>> centroids;
    /**
     * Inverse index for the centroids.
     */
    private final Map<String, Map<U, Double>> invCentroids;
    /**
     * Modules of the centroids.
     */
    private final Map<U, Double> modules;

    /**
     * Constructor
     * @param graph The training graph.
     * @param index Content index that contains information about users.
     */
    public CentroidCBRecommender(FastGraph<U> graph, WrapperIndividualForwardContentIndex<?,U> index, EdgeOrientation orient)
    {
        super(graph);
        this.centroids = new ConcurrentHashMap<>();
        this.invCentroids = new ConcurrentHashMap<>();
        this.modules = new ConcurrentHashMap<>();

        int numDocs = index.numDocs();

        // Content cache, so it does not have to be retrieved each time from the index.
        Map<Integer, List<Tuple2od<String>>> fvs = new ConcurrentHashMap<>();

        AtomicInteger atom = new AtomicInteger(0);
        // Then, once we have the index, we just find the centroids for each user in the network (and its modules)
        graph.getAllNodes().parallel().forEach(u ->
        {
            Object2DoubleOpenHashMap<String> centroid = new Object2DoubleOpenHashMap<>();
            // For each neighbor node:
            graph.getNeighbourhood(u, orient).forEach(v ->
                index.getContents(v).forEach(content ->
                {
                    try
                    {
                        if(fvs.containsKey(content))
                        {
                            List<Tuple2od<String>> list = fvs.get(content);
                            for(Tuple2od<String> tuple : list)
                            {
                                String term = tuple.v1;
                                double tfidf = tuple.v2;
                                centroid.addTo(tuple.v1, tuple.v2);

                                if(!invCentroids.containsKey(term))
                                    this.createTerm(term);
                                Map<U, Double> map = invCentroids.get(term);
                                map.put(u, map.getOrDefault(u, 0.0) + tfidf);
                            }
                        }
                        else
                        {
                            FreqVector vector = index.getContentVector(content);
                            List<Tuple2od<String>> list = new ArrayList<>();
                            if (vector != null)
                            {
                                for (TermFreq tf : vector)
                                {
                                    String term = tf.getTerm();
                                    double freq = tf.getFreq();
                                    double df = index.getDocFreq(term);
                                    double tfidf = VSMSearchEngine.tfidf(freq, df, numDocs);
                                    centroid.addTo(term, tfidf);

                                    if(!invCentroids.containsKey(term))
                                        this.createTerm(term);
                                    Map<U, Double> map = invCentroids.get(term);
                                    map.put(u, map.getOrDefault(u, 0.0) + tfidf);
                                    list.add(new Tuple2od<>(term,tfidf));
                                }

                                fvs.put(content, list);
                            }
                        }
                    }
                    catch(IOException ioe)
                    {

                    }
                })

            );
            this.centroids.put(u, centroid);
            int init = atom.incrementAndGet();
            if(init % 100 == 0) System.err.println("Processed " + init + " users");
        });

        // Find the modules:
        graph.getAllNodes().parallel().forEach(u -> modules.put(u, Math.sqrt(this.centroids.get(u).values().stream().mapToDouble(v -> v*v).sum())));
    }

    /**
     * Add a term to the inverse centroid.
     * @param term the term.
     */
    private synchronized void createTerm(String term)
    {
        if(!invCentroids.containsKey(term))
        {
            invCentroids.put(term, new ConcurrentHashMap<>());
        }
    }

    /**
     * Constructor
     * @param graph The training graph.
     * @param index Content index that contains information about users.
     */
    public CentroidCBRecommender(FastGraph<U> graph, WrapperIndividualForwardContentIndex<?,U> index)
    {
        super(graph);
        this.centroids = new ConcurrentHashMap<>();
        this.invCentroids = new ConcurrentHashMap<>();
        this.modules = new ConcurrentHashMap<>();


        int numDocs = index.numDocs();
        // Find the centroids for each user.
        graph.getAllNodes().forEach(u ->
        {
            Object2DoubleOpenHashMap<String> centroid = new Object2DoubleOpenHashMap<>();
            index.getContents(u).forEach(content ->
            {
                try
                {
                    FreqVector vector = index.getContentVector(content);
                    for(TermFreq tf : vector)
                    {
                        String term = tf.getTerm();
                        double freq = tf.getFreq();
                        double df = index.getDocFreq(term);
                        double tfidf = VSMSearchEngine.tfidf(freq, df, numDocs);
                        centroid.addTo(term, tfidf);

                        Map<U, Double> map = invCentroids.getOrDefault(term, new ConcurrentHashMap<>());
                        map.put(u, map.getOrDefault(u, 0.0) + tfidf);
                        invCentroids.put(term, map);
                        this.centroids.put(u, centroid);
                    }
                }
                catch(IOException ioe)
                {

                }
            });
        });
        // Find the modules:
        graph.getAllNodes().parallel().forEach(u -> modules.put(u, Math.sqrt(this.centroids.get(u).values().stream().mapToDouble(v -> v*v).sum())));
    }

    @Override
    public Int2DoubleMap getScoresMap(int uIdx)
    {
        Int2DoubleOpenHashMap scores = new Int2DoubleOpenHashMap();
        U u = this.uidx2user(uIdx);

        centroids.get(u).forEach((term, uVal) ->
            invCentroids.getOrDefault(term, new HashMap<>()).forEach((v, vVal) ->
            {
                int vidx = this.user2uidx(v);
                scores.addTo(vidx, uVal*vVal/modules.get(v));
            }));

        return scores;
    }
}
