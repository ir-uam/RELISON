/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.updateable.preference;

import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.socialranksys.links.data.updateable.Updateable;

/**
 * Interface for updateable preference data.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 */
public interface UpdateablePreferenceData<U,I> extends PreferenceData<U,I>, Updateable<U,I>
{
    
}
