/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.features;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.relison.community.Communities;
import org.jooq.lambda.tuple.Tuple2;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Class for loading feature data from an index.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CommunityFeatureData<U> implements FeatureData<U,Integer,Double>
{
    /**
     * Item to feature map.
     */
    private final Map<U, Map<Integer,Double>> itemMap;
    /**
     * Feature to item map.
     */
    private final Map<Integer, Map<U, Double>> featMap;

    /**
     * Constructor.
     * @param itemMap item to features map.
     * @param featMap features to items map.
     */
    protected CommunityFeatureData(Map<U, Map<Integer, Double>> itemMap, Map<Integer, Map<U,Double>> featMap)
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
    public Stream<U> getItemsWithFeatures()
    {
        return itemMap.keySet().stream();
    }

    @Override
    public Stream<Integer> getFeaturesWithItems()
    {
        return featMap.keySet().stream();
    }

    @Override
    public Stream<Tuple2<U, Double>> getFeatureItems(Integer f)
    {
        return featMap.getOrDefault(f, new HashMap<>()).entrySet().stream().map(entry -> new Tuple2<>(entry.getKey(), entry.getValue()));
    }

    @Override
    public Stream<Tuple2<Integer, Double>> getItemFeatures(U i)
    {
        return itemMap.getOrDefault(i, new HashMap<>()).entrySet().stream().map(entry -> new Tuple2<>(entry.getKey(), entry.getValue()));

    }

    @Override
    public int numFeatures(U i)
    {
        return itemMap.getOrDefault(i, new HashMap<>()).size();
    }

    @Override
    public int numItems(Integer f)
    {
        return featMap.getOrDefault(f, new HashMap<>()).size();
    }

    @Override
    public boolean containsFeature(Integer f)
    {
        return featMap.containsKey(f);
    }

    @Override
    public int numFeatures()
    {
        return featMap.size();
    }

    @Override
    public Stream<Integer> getAllFeatures()
    {
        return featMap.keySet().stream();
    }

    @Override
    public boolean containsItem(U i)
    {
        return itemMap.containsKey(i);
    }

    @Override
    public int numItems()
    {
        return itemMap.size();
    }

    @Override
    public Stream<U> getAllItems()
    {
        return itemMap.keySet().stream();
    }

    /**
     * Loads the feature data from a forward index.
     * @param comms the communities users belong to.
     * @param <U>   type of the users
     * @return the feature data object.
     */
    public static <U> CommunityFeatureData<U> load(Communities<U> comms)
    {
        Map<U, Map<Integer, Double>> itemMap = new ConcurrentHashMap<>();
        Map<Integer, Map<U,Double>> featMap = new ConcurrentHashMap<>();

        comms.getCommunities().forEach(comm ->
        {
            Map<U, Double> commMap = new HashMap<>();
            comms.getUsers(comm).forEach(u ->
            {
                commMap.put(u, 1.0);
                if(!itemMap.containsKey(u))
                    itemMap.put(u, new HashMap<>());
                itemMap.get(u).put(comm, 1.0);
            });
            featMap.put(comm, commMap);
        });

        return new CommunityFeatureData<>(itemMap, featMap);
    }
}
