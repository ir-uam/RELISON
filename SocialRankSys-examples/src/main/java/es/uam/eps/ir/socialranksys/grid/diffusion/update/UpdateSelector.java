/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.update;

import es.uam.eps.ir.socialranksys.diffusion.update.UpdateMechanism;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;

import static es.uam.eps.ir.socialranksys.grid.diffusion.update.UpdateMechanismIdentifiers.ICM;
import static es.uam.eps.ir.socialranksys.grid.diffusion.update.UpdateMechanismIdentifiers.OLDER;

/**
 * Class that selects an individual update mechanism.
 * @author Javier Sanz-Cruzado Puig
 */
public class UpdateSelector
{
    /**
     * Selects and configures a update mechanism.
     * @param upr Parameters for the update mechanism.
     * @return A pair containing the name and the selected update mechanism.
     */
    public Tuple2oo<String, UpdateMechanism> select(UpdateParamReader upr)
    {
        String name = upr.getName();
        UpdateConfigurator conf;
        switch(name)
        {
            case ICM:
                conf = new IndependentCascadeModelUpdateConfigurator();
                break;
            case OLDER:
                conf = new OlderUpdateConfigurator();
                break;
            default:
                return null;
        }
        
        UpdateMechanism propagation = conf.configure(upr);
        return new Tuple2oo<>(name, propagation);
    }
}
