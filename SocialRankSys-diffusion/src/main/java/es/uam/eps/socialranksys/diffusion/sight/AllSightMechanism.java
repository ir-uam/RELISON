/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.sight;

import es.uam.eps.socialranksys.diffusion.data.Data;
import es.uam.eps.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;

/**
 * Sight mechanism that selects every piece of information that has arrived.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * @param <I> Type of the information pieces
 * @param <P> Type of the parameters
 */
public class AllSightMechanism<U extends Serializable,I extends Serializable,P> implements SightMechanism<U,I,P>
{
    @Override
    public boolean seesInformation(UserState<U> user, Data<U,I,P> data, PropagatedInformation prop)
    {
        return !user.containsPropagatedInformation(prop.getInfoId());
    }
    
}
