/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.update;

import es.uam.eps.ir.sonalire.diffusion.update.UpdateMechanism;
import es.uam.eps.ir.sonalire.grid.Parameters;

/**
 * Interface for configuring an update mechanism.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface UpdateConfigurator 
{
    /**
     * Configures a update mechanism for the information pieces.
     * @param params the parameters of the mechanism.
     * @return the update mechanism.
     */
    UpdateMechanism configure(Parameters params);
}
