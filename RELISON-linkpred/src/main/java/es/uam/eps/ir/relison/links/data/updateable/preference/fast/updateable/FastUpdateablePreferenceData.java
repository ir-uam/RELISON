/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.data.updateable.preference.fast.updateable;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.relison.links.data.updateable.index.fast.FastUpdateableItemIndex;
import es.uam.eps.ir.relison.links.data.updateable.index.fast.FastUpdateableUserIndex;
import es.uam.eps.ir.relison.links.data.updateable.preference.UpdateablePreferenceData;

/**
 * Interface for updateable preference data.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 */
public interface FastUpdateablePreferenceData<U,I> extends UpdateablePreferenceData<U,I>, FastPreferenceData<U,I>, FastUpdateableUserIndex<U>, FastUpdateableItemIndex<I>
{
    
}
