/*
 * Copyright (C) 2016 RankSys http://ranksys.org
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data.updateable.preference.fast.updateable;

import es.uam.eps.ir.ranksys.core.preference.IdPref;
import es.uam.eps.ir.ranksys.fast.preference.IdxPref;
import es.uam.eps.ir.sonalire.links.data.updateable.index.fast.FastUpdateableItemIndex;
import es.uam.eps.ir.sonalire.links.data.updateable.index.fast.FastUpdateableUserIndex;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.ints.IntIterator;
import org.ranksys.core.util.iterators.StreamDoubleIterator;
import org.ranksys.core.util.iterators.StreamIntIterator;

import java.util.function.Function;

/**
 * Extends AbstractFastUpdateablePreferenceData and implements the data access iterator-based methods
 * using the stream-based ones. Avoids duplicating code where stream-based methods
 * are preferred.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Saúl Vargas (Saul@VargasSandoval.es)
 * @param <U> user type
 * @param <I> item type
 */
public abstract class StreamsAbstractFastUpdateablePreferenceData<U, I> extends AbstractFastUpdateablePreferenceData<U, I> {

    /**
     * Constructor with default IdxPref to IdPref converter.
     *
     * @param userIndex user index
     * @param itemIndex item index
     */
    public StreamsAbstractFastUpdateablePreferenceData(FastUpdateableUserIndex<U> userIndex, FastUpdateableItemIndex<I> itemIndex) {
        super(userIndex, itemIndex);
    }

    /**
     * Constructor with custom IdxPref to IdPref converter.
     *
     * @param userIndex user index
     * @param itemIndex item index
     * @param uPrefFun user IdxPref to IdPref converter
     * @param iPrefFun item IdxPref to IdPref converter
     */
    public StreamsAbstractFastUpdateablePreferenceData(FastUpdateableUserIndex<U> userIndex, FastUpdateableItemIndex<I> itemIndex, Function<IdxPref, IdPref<I>> uPrefFun, Function<IdxPref, IdPref<U>> iPrefFun)
    {
        super(userIndex, itemIndex, uPrefFun, iPrefFun);
    }

    @Override
    public IntIterator getUidxIidxs(int uidx) 
    {
        return new StreamIntIterator(getUidxPreferences(uidx).mapToInt(IdxPref::v1));
    }

    @Override
    public DoubleIterator getUidxVs(int uidx) 
    {
        return new StreamDoubleIterator(getUidxPreferences(uidx).mapToDouble(IdxPref::v2));
    }

    @Override
    public IntIterator getIidxUidxs(int iidx)
    {
        return new StreamIntIterator(getIidxPreferences(iidx).mapToInt(IdxPref::v1));
    }

    @Override
    public DoubleIterator getIidxVs(int iidx) 
    {
        return new StreamDoubleIterator(getIidxPreferences(iidx).mapToDouble(IdxPref::v2));
    }

    @Override
    public boolean useIteratorsPreferentially()
    {
        return false;
    }

}
