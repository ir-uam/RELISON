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
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2id;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;

/**
 * Neutral normalization (does not normalize the scores).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 */
public class NoNormalizer<U,I> implements FastNormalizer<U,I>
{
    @Override
    public Recommendation<U,I> normalize(Recommendation<U,I> recommendation)
    {
        U u = recommendation.getUser();
        List<Tuple2od<I>> rec = new ArrayList<>(recommendation.getItems());
        return new Recommendation<>(u, rec);
    }

    @Override
    public FastRecommendation normalize(FastRecommendation recommendation)
    {
        int uidx = recommendation.getUidx();
        List<Tuple2id> rec = new ArrayList<>(recommendation.getIidxs());
        return new FastRecommendation(uidx, rec);
    }

    @Override
    public FastIndividualNormalizer getInstance()
    {
        return new IndividualNoNormalizer();
    }

    @Override
    public List<Double> normalize(List<Double> items)
    {
        return new ArrayList<>(items);
    }


    private static class IndividualNoNormalizer implements FastIndividualNormalizer
    {
        Int2DoubleMap values = new Int2DoubleOpenHashMap();

        @Override
        public void add(Tuple2id tuple)
        {
            values.put(tuple.v1, tuple.v2);
        }

        @Override
        public double getScore(int iidx)
        {
            return values.getOrDefault(iidx, Double.NaN);
        }
    }


}
