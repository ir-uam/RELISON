/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.filler;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Filler that completes ranking with random recommendation.
 *
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the items.
 */
public class RandomFiller<U,I> extends AbstractFastFiller<U,I>
{
    /**
     * Random number generator.
     */
    private final Random rng;
    /**
     * A list containing the selected order for the items.
     */
    private final List<Integer> order;
    
    /**
     * Constructor.
     * @param prefData  preference data.
     * @param seed      random number generator seed.
     */
    public RandomFiller(FastPreferenceData<U,I> prefData, long seed)
    {
        super(prefData);
        this.rng = new Random(seed);
        this.order = new ArrayList<>();
        prefData.getAllIidx().forEach(this.order::add);
        Collections.shuffle(order, rng);
    }
       
    /**
     * Constructor.
     * @param prefData preference data.
     */
    public RandomFiller(FastPreferenceData<U,I> prefData)
    {
        this(prefData, System.currentTimeMillis());
    }

    @Override
    protected void resetMethod()
    {
        Collections.shuffle(order, rng);
    }

    @Override
    public IntStream fillerList(int uidx)
    {
        return this.order.stream().mapToInt(x->x);
    }

    @Override
    public Stream<I> fillerList(U u)
    {
        return this.order.stream().map(iidx -> this.prefData.iidx2item(iidx));
    }
    
    
}
