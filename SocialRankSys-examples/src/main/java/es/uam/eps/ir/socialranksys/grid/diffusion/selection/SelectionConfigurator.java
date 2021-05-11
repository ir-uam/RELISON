/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.selection;

import es.uam.eps.ir.socialranksys.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.socialranksys.grid.Parameters;

import java.io.Serializable;

/**
 * Interface for configuring a selection mechanism.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public interface SelectionConfigurator<U extends Serializable,I extends Serializable, F>
{
    /**
     * Configures a selection mechanism for the information pieces to propagate.
     * @param params the parameters of the selection mechanism.
     * @return the selection mechanism.
     */
    SelectionMechanism<U,I, F> configure(Parameters params);
}
