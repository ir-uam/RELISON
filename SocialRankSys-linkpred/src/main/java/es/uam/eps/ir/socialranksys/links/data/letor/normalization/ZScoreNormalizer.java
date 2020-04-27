/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor.normalization;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.ranksys.fast.FastRecommendation;
import org.ranksys.core.util.tuples.Tuple2id;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the z-score normalization.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 * @param <I> type of the items.
 */
public class ZScoreNormalizer<U,I> implements FastNormalizer<U,I> 
{
    @Override
    public List<Double> normalize(List<Double> values)
    {
        Stats stats = new Stats();

        values.stream().filter(Double::isFinite).forEach(stats::accept);

        List<Double> newValues = new ArrayList<>();
        if(stats.getStandardDeviation() == 0)
        {
            for(double val : values) newValues.add(0.0);
        }
        else
        {
            double mean = stats.getMean();
            double sd = stats.getStandardDeviation();
            double min = stats.getMin();
            double max = stats.getMax();

            for(double val : values)
            {
                if(Double.isFinite(val))
                {
                    newValues.add((val - mean)/sd);
                }
                else
                {
                    if(val > 0) newValues.add((max - mean)/sd);
                    else newValues.add((min-mean)/sd);
                }
            }
        }

        return newValues;
    }

    @Override
    public FastRecommendation normalize(FastRecommendation recommendation)
    {
        int uidx = recommendation.getUidx();
        List<Tuple2id> iidxs = recommendation.getIidxs();
        Stats stats = new Stats();
        
        iidxs.stream().filter(iidx -> Double.isFinite(iidx.v2)).forEach((iidx) -> stats.accept(iidx.v2));
        
        List<Tuple2id> newIidxs = new ArrayList<>();
        if(stats.getStandardDeviation() == 0)
        {
            for(Tuple2id iidx : iidxs)
            {
                newIidxs.add(new Tuple2id(iidx.v1, 0.0));
            }
        }
        else
        {
            double mean = stats.getMean();
            double sd = stats.getStandardDeviation();
            double min = stats.getMin();
            double max = stats.getMax();

            for(Tuple2id iidx : iidxs)
            {
                if(Double.isFinite(iidx.v2))
                {
                    newIidxs.add(new Tuple2id(iidx.v1, (iidx.v2 - mean) / sd));
                }
                else
                {
                    if(iidx.v2 > 0) newIidxs.add(new Tuple2id(iidx.v1, (max-mean)/sd));
                    else newIidxs.add(new Tuple2id(iidx.v1, (min-mean)/sd));
                }
            }
        }
        return new FastRecommendation(uidx,newIidxs);
    }

    @Override
    public Recommendation<U,I> normalize(Recommendation<U,I> recommendation)
    {
        U u = recommendation.getUser();
        List<Tuple2od<I>> items = recommendation.getItems();
        Stats stats = new Stats();

        items.stream().filter(iidx -> Double.isFinite(iidx.v2))
                      .forEach((iidx) -> stats.accept(iidx.v2));

        List<Tuple2od<I>> newItems = new ArrayList<>();
        if (stats.getStandardDeviation() == 0)
        {
            for (Tuple2od<I> item : items)
            {
                newItems.add(new Tuple2od<>(item.v1, 0.0));
            }
        }
        else
        {
            double mean = stats.getMean();
            double sd = stats.getStandardDeviation();
            double min = stats.getMin();
            double max = stats.getMax();

            for (Tuple2od<I> item : items)
            {
                if (Double.isFinite(item.v2))
                {
                    newItems.add(new Tuple2od<>(item.v1, (item.v2 - mean) / sd));
                }
                else
                {
                    if (item.v2 > 0) newItems.add(new Tuple2od<>(item.v1, (max - mean) / sd));
                    else newItems.add(new Tuple2od<>(item.v1, (min - mean) / sd));
                }
            }
        }
        return new Recommendation<>(u, newItems);
    }
}
