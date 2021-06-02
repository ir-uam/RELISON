/* 
 * Copyright (C) 2015 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data.updateable.index;

import es.uam.eps.ir.ranksys.core.index.ItemIndex;

import java.util.stream.Stream;

/**
 * Updateable index for a set of items.
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Saúl Vargas (saul.vargas@uam.es)
 *
 * @param <I> type of the items
 */
public interface UpdateableItemIndex<I> extends ItemIndex<I>
{
    /**
     * Adds a new item.
     * @param i the item.
     * @return the identifier of the new item
     */
    int addItem(I i);

    /**
     * Adds a set of items to the index
     * @param items a stream containing the items to add.
     */
    default void addItems(Stream<I> items)
    {
        items.forEach(this::addItem);
    }
}
