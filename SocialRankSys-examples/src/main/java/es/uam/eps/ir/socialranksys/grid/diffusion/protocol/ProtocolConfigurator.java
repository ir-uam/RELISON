/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.protocol;

import es.uam.eps.ir.socialranksys.diffusion.protocols.Protocol;

import java.io.Serializable;

/**
 * Interface for configuring protocols for information diffusion, given their properties.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public interface ProtocolConfigurator<U extends Serializable,I extends Serializable,F>
{
    /**
     * Configures a protocol.
     * @param params the parameters for configuring the protocol.
     * @return the protocol.
     */
    Protocol<U,I,F> configure(YAMLProtocolParameterReader params);
}
