/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.features;

import es.uam.eps.ir.ranksys.novdiv.distance.ItemDistanceModel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Item distance model which stores the distances in a cache.
 *
 * @param <I> type of the items.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CacheableItemDistanceModel<I> implements ItemDistanceModel<I>
{
    /**
     * The item distance cache.
     */
    private final Map<I, Map<I, Double>> cache;
    /**
     * The current item distance model.
     */
    private final ItemDistanceModel<I> model;
    /**
     * True if distances are symmetric (d(a,b)=d(b,a)), false otherwise.
     */
    private final boolean symmetric;

    /**
     * Constructor.
     * @param model     a distance model between items.
     * @param symmetric true if d(a,b)=d(b,a), false otherwise.
     */
    public CacheableItemDistanceModel(ItemDistanceModel<I> model, boolean symmetric)
    {
        this.model = model;
        cache = new HashMap<>();
        this.symmetric = symmetric;
    }


    @Override
    public ToDoubleFunction<I> dist(I i)
    {
        return (I j) -> dist(i,j);
    }

    @Override
    public double dist(I i, I j)
    {
        if(cache.containsKey(i) && cache.get(i).containsKey(j))
        {
            return cache.get(i).get(j);
        }
        else
        {
            double dist = model.dist(i,j);
            if(!cache.containsKey(i))
            {
                cache.put(i, new HashMap<>());
            }
            cache.get(i).put(j,dist);

            if(symmetric)
            {
                if(!cache.containsKey(j))
                {
                    cache.put(j, new HashMap<>());
                }
                cache.get(j).put(i,dist);
            }
        }
        return 0;
    }
}
