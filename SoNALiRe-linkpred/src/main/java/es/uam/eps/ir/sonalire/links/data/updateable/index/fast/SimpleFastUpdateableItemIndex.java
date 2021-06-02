/* 
 * Copyright (C) 2015 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data.updateable.index.fast;

import es.uam.eps.ir.ranksys.fast.index.SimpleFastItemIndex;

import java.util.stream.Stream;


/**
 * Simple implementation of FastUpdateableItemIndex backed by a bi-map IdxIndex
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Saúl Vargas (saul.vargas@uam.es)
 *
 * @param <I> type of the items
 */
public class SimpleFastUpdateableItemIndex<I> extends SimpleFastItemIndex<I> implements FastUpdateableItemIndex<I>
{
    @Override
    public int addItem(I i) 
    {
        return this.add(i);
    }
    
    /*@Override
    public int removeItem(I i)
    {
        return this.remove(i);
    }*/
    
    /**
     * Creates an item index from a stream of item objects.
     *
     * @param <I> type of the items
     * @param items stream of item objects
     * @return a fast item index
     */
    public static <I> SimpleFastUpdateableItemIndex<I> load(Stream<I> items) {
        SimpleFastUpdateableItemIndex<I> itemIndex = new SimpleFastUpdateableItemIndex<>();
        items.forEach(itemIndex::addItem);
        return itemIndex;
    }
}
