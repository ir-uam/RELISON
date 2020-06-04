/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.sight;

import es.uam.eps.ir.socialranksys.diffusion.sight.SightMechanism;

import java.io.Serializable;

/**
 * Configures a sight mechanism.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public interface SightConfigurator<U extends Serializable,I extends Serializable,P>
{
    /**
     * Configures a sight mechanism for the information pieces received by an user.
     * @param params the parameters of the mechanism.
     * @return the sight mechanism.
     */
    SightMechanism<U,I,P> configure(SightParamReader params);
}
