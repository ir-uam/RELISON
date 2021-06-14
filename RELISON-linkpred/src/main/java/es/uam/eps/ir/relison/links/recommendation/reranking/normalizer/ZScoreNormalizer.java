/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.normalizer;

/**
 * Z-Score normalizer. Normalizes the values so they follow a
 * normal distribution with 0 mean and variance 1.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <I> type of the items.
 */
public class ZScoreNormalizer<I> extends StatsBasedNormalizer<I>
{
    @Override
    public double norm(I i, double value)
    {
        return (value - stats.getMean())/stats.getStandardDeviation();
    }
}
