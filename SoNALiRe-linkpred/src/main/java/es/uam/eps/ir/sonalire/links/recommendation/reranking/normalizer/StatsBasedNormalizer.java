/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer;


import es.uam.eps.ir.ranksys.core.util.Stats;

/**
 * Normalizer based on the data statistics.
 *
 * @param <I> type of the items.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class StatsBasedNormalizer<I> implements Normalizer<I>
{
    /**
     * Data statistics.
     */
    protected final Stats stats = new Stats();

    @Override
    public void add(I i, double val)
    {
        this.stats.accept(val);
    }
}
