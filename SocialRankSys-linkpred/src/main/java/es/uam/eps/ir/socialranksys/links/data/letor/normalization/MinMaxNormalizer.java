/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor.normalization;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.fast.FastRecommendation;
import org.ranksys.core.util.tuples.Tuple2id;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the min-max normalization, which rescales the results to
 * interval [0,1]
 *
 *
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 */
public class MinMaxNormalizer<U,I> implements FastNormalizer<U,I>
{
    @Override
    public FastRecommendation normalize(FastRecommendation recommendation)
    {
        List<Tuple2id> iidxs = recommendation.getIidxs();

        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;

        for(Tuple2id iidx : iidxs)
        {
            if(Double.isFinite(iidx.v2()))
            {
                if(iidx.v2() > max) max = iidx.v2();
                if(iidx.v2() < min) min = iidx.v2();
            }
        }

        List<Tuple2id> newIidxs = new ArrayList<>();
        if(max == min || !Double.isFinite(max) || !Double.isFinite(min))
        {
            for(Tuple2id iidx : iidxs)
            {
                newIidxs.add(new Tuple2id(iidx.v1, 0.0));
            }
        }
        else
        {
            for(Tuple2id iidx : iidxs)
            {
                if(Double.isFinite(iidx.v2()))
                {
                    newIidxs.add(new Tuple2id(iidx.v1, (iidx.v2 - min)/(max - min)));
                }
                else
                {
                    if(iidx.v2 > 0) newIidxs.add(new Tuple2id(iidx.v1, 1.0));
                    else newIidxs.add(new Tuple2id(iidx.v1, 0.0));
                }
            }
        }

        return new FastRecommendation(recommendation.getUidx(), newIidxs);
    }

    @Override
    public Recommendation<U,I> normalize(Recommendation<U,I> recommendation)
    {
        List<Tuple2od<I>> items = recommendation.getItems();

        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;

        for(Tuple2od<I> item : items)
        {
            if(Double.isFinite(item.v2()))
            {
                if(item.v2() > max) max = item.v2();
                if(item.v2() < min) min = item.v2();
            }
        }

        List<Tuple2od<I>> newItems = new ArrayList<>();
        if(max == min || !Double.isFinite(max) || !Double.isFinite(min))
        {
            for(Tuple2od<I> item : items)
            {
                newItems.add(new Tuple2od<>(item.v1, 0.0));
            }
        }
        else
        {
            for(Tuple2od<I> item : items)
            {
                if(Double.isFinite(item.v2()))
                {
                    newItems.add(new Tuple2od<>(item.v1, (item.v2 - min)/(max - min)));
                }
                else
                {
                    if(item.v2 > 0) newItems.add(new Tuple2od<>(item.v1, 1.0));
                    else newItems.add(new Tuple2od<>(item.v1, 0.0));
                }
            }
        }

        return new Recommendation<>(recommendation.getUser(), newItems);
    }


    @Override
    public List<Double> normalize(List<Double> values)
    {
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;

        for(double val : values)
        {
            if(Double.isFinite(val))
            {
                if(val > max) max = val;
                if(val < min) min = val;
            }
        }

        List<Double> newValues = new ArrayList<>();
        if(max == min || !Double.isFinite(max) || !Double.isFinite(min))
        {
            for(double val : values)
            {
                newValues.add(0.0);
            }
        }
        else
        {
            for(double val : values)
            {
                if (Double.isFinite(val))
                {
                    newValues.add((val - min) / (max - min));
                }
                else
                {
                    if (val > 0) newValues.add(1.0);
                    else newValues.add(0.0);
                }
            }
        }

        return newValues;
    }
}
