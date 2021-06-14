/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.normalizer;

import java.util.function.Supplier;

/**
 * Examples of normalizers.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Normalizers
{
    /**
     * It does not apply any normalization.
     * @param <I> type of the users.
     * @return a normalization scheme which does not normalize.
     */
    public static <I> Supplier<Normalizer<I>> noNorm()
    {
        return NoNormalizer::new;
    }

    /**
     * Obtains a normalizer that normalizes according to ranking position.
     * @param <I> type of the users.
     * @return a normalization scheme which normalizes according to ranking position.
     */
    public static <I> Supplier<Normalizer<I>> ranksim()
    {
        return RanksimNormalizer::new;
    }

    /**
     * Obtains a normalizer that normalizes according to the maximum and minimum values in
     * the scores.
     * @param <I> type of the users.
     * @return a normalization scheme that normalizes according to the maximum and minimum values in
     * the scores.
     */
    public static <I> Supplier<Normalizer<I>> minmax()
    {
        return MinMaxNormalizer::new;
    }

    /**
     * Obtains a normalizer that normalizes scores so they follow a normal distribution.
     * @param <I> type of the users.
     * @return a normalization scheme that normalizes scores so they follow a normal distribution.
     */
    public static <I> Supplier<Normalizer<I>> zscore()
    {
        return ZScoreNormalizer::new;
    }
}
