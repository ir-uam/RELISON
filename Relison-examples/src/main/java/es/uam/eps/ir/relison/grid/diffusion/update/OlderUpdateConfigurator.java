/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.update;

import es.uam.eps.ir.relison.diffusion.update.NewestUpdateMechanism;
import es.uam.eps.ir.relison.diffusion.update.OlderUpdateMechanism;
import es.uam.eps.ir.relison.diffusion.update.UpdateMechanism;
import es.uam.eps.ir.relison.grid.Parameters;

/**
 * Configures an update mechanism that updates the information in the newest piece with the older ones.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see NewestUpdateMechanism
 */
public class OlderUpdateConfigurator implements UpdateConfigurator
{
    
    @Override
    public UpdateMechanism configure(Parameters params)
    {
        return new OlderUpdateMechanism();
    }
}
