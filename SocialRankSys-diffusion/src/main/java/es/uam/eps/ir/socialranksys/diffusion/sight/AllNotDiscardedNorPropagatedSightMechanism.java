/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
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
 * This mechanism sees all the pieces of information who have not been previously discarded nor propagated.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class AllNotDiscardedNorPropagatedSightMechanism<U extends Serializable,I extends Serializable,P> extends IndividualSightMechanism<U,I,P>
{
    @Override
    public boolean seesInformation(UserState<U> user, Data<U,I,P> data, PropagatedInformation prop)
    {
        return !user.containsDiscardedInformation(prop.getInfoId()) && !user.containsPropagatedInformation(prop.getInfoId());
    }
}
