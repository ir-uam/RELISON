/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.sight;


import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;

/**
 * Sees the pieces of information that come from all users, but have not been discarded before.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class AllNotDiscardedSightMechanism<U extends Serializable,I extends Serializable,P> implements SightMechanism<U,I,P> 
{

   /* @Override
    public Stream<PropagatedInformation> seeInformation(UserState<U> user, Data<U, I, P> data) 
    {
        return user.getNewInformation().filter(info -> !user.containsDiscardedInformation(info.getInfoId()));
    }*/
    
    @Override
    public boolean seesInformation(UserState<U> user, Data<U,I,P> data, PropagatedInformation prop)
    {
        return !user.containsDiscardedInformation(prop.getInfoId()) && !user.containsPropagatedInformation(prop.getInfoId());
    }

    
}
