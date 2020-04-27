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
import es.uam.eps.ir.ranksys.fast.FastRecommendation;
import org.ranksys.core.util.tuples.Tuple2id;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the min-max normalization, which rescales the results to
 * interval [0,1]
 * @author Javier Sanz-Cruzado Puig
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
    public List<Double> normalize(List<Double> items)
    {
        return new ArrayList<>(items);
    }
    
}
