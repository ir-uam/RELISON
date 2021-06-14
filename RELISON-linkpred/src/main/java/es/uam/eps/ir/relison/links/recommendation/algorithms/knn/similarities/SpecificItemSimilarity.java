/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.knn.similarities;

import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.nn.item.sim.ItemSimilarity;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;

/**
 * Similarity between items.
 *
 * @param <I> type of the items.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SpecificItemSimilarity<I> extends ItemSimilarity<I>
{
    /**
     * Constructor.
     * @param iIndex    the item index.
     * @param sim       the similarity.
     */
    public SpecificItemSimilarity(FastItemIndex<I> iIndex, Similarity sim)
    {
        super(iIndex, sim);
    }
}
