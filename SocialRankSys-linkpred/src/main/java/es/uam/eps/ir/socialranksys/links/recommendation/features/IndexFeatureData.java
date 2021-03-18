/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.features;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.socialranksys.content.index.ForwardIndex;
import es.uam.eps.ir.socialranksys.content.index.freq.FreqVector;
import es.uam.eps.ir.socialranksys.content.index.freq.TermFreq;
import es.uam.eps.ir.socialranksys.content.search.VSMSearchEngine;
import org.jooq.lambda.tuple.Tuple2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class for loading feature data from an index.
 *
 * @param <I> type of the items.
 */
public class IndexFeatureData<I> implements FeatureData<I,String,Double>
{
    /**
     * Item to feature map.
     */
    private final Map<I, Map<String,Double>> itemMap;
    /**
     * Feature to item map.
     */
    private final Map<String, Map<I, Double>> featMap;

    /**
     * Constructor.
     * @param itemMap item to features map.
     * @param featMap features to items map.
     */
    protected IndexFeatureData(Map<I, Map<String, Double>> itemMap, Map<String, Map<I,Double>> featMap)
    {
        this.itemMap = itemMap;
        this.featMap = featMap;
    }

    @Override
    public int numItemsWithFeatures()
    {
        return itemMap.size();
    }

    @Override
    public int numFeaturesWithItems()
    {
        return featMap.size();
    }

    @Override
    public Stream<I> getItemsWithFeatures()
    {
        return itemMap.keySet().stream();
    }

    @Override
    public Stream<String> getFeaturesWithItems()
    {
        return featMap.keySet().stream();
    }

    @Override
    public Stream<Tuple2<I, Double>> getFeatureItems(String f)
    {
        return featMap.getOrDefault(f, new HashMap<>()).entrySet().stream().map(entry -> new Tuple2<>(entry.getKey(), entry.getValue()));
    }

    @Override
    public Stream<Tuple2<String, Double>> getItemFeatures(I i)
    {
        return itemMap.getOrDefault(i, new HashMap<>()).entrySet().stream().map(entry -> new Tuple2<>(entry.getKey(), entry.getValue()));

    }

    @Override
    public int numFeatures(I i)
    {
        return itemMap.getOrDefault(i, new HashMap<>()).size();
    }

    @Override
    public int numItems(String f)
    {
        return featMap.getOrDefault(f, new HashMap<>()).size();
    }

    @Override
    public boolean containsFeature(String f)
    {
        return featMap.containsKey(f);
    }

    @Override
    public int numFeatures()
    {
        return featMap.size();
    }

    @Override
    public Stream<String> getAllFeatures()
    {
        return featMap.keySet().stream();
    }

    @Override
    public boolean containsItem(I i)
    {
        return itemMap.containsKey(i);
    }

    @Override
    public int numItems()
    {
        return itemMap.size();
    }

    @Override
    public Stream<I> getAllItems()
    {
        return itemMap.keySet().stream();
    }

    /**
     * Loads the feature data from a forward index.
     * @param fIndex the forward index.
     * @param <I> type of the items.
     * @return the feature data object.
     */
    public static <I> IndexFeatureData<I> load(ForwardIndex<I> fIndex)
    {
        Map<I, Map<String, Double>> itemMap = new ConcurrentHashMap<>();
        Map<String, Map<I,Double>> featMap = new ConcurrentHashMap<>();

        int numDocs = fIndex.numDocs();
        IntStream.range(0, numDocs).parallel().forEach(iidx ->
        {
            try
            {
                Map<String, Double> map = new HashMap<>();
                I i = fIndex.getContent(iidx);
                FreqVector fv = fIndex.getContentVector(iidx);
                if (fv != null)
                {
                    for (TermFreq freq : fv)
                    {
                        String term = freq.getTerm();
                        double tf = freq.getFreq() + 0.0;
                        double df = fIndex.getDocFreq(term);

                        double tfidf = VSMSearchEngine.tfidf(tf, df, numDocs);
                        map.put(term, tfidf);


                        if (!featMap.containsKey(term))
                        {
                            synchronized (IndexFeatureData.class)
                            {
                                if (!featMap.containsKey(term))
                                {
                                    featMap.put(term, new ConcurrentHashMap<>());
                                }
                            }
                        }
                        Map<I, Double> auxMap = featMap.get(term);
                        auxMap.put(i, tfidf);
                    }
                }

                if(!map.isEmpty()) itemMap.put(i, map);
            }
            catch(IOException ioe)
            {

            }
        });

        return new IndexFeatureData<>(itemMap, featMap);
    }
}
