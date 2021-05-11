/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.update;

import es.uam.eps.ir.socialranksys.diffusion.update.UpdateMechanism;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import org.jooq.lambda.tuple.Tuple2;

import static es.uam.eps.ir.socialranksys.grid.diffusion.update.UpdateMechanismIdentifiers.NEWEST;
import static es.uam.eps.ir.socialranksys.grid.diffusion.update.UpdateMechanismIdentifiers.OLDER;

/**
 * Class that selects an individual update mechanism.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class UpdateSelector
{
    /**
     * Selects and configures a update mechanism.
     * @param name      the name of the update mechanism.
     * @param params    the parameters of the update mechanism.
     * @return A pair containing the name and the selected update mechanism.
     */
    public Tuple2<String, UpdateMechanism> select(String name, Parameters params)
    {
        UpdateConfigurator conf;
        switch(name)
        {
            case NEWEST:
                conf = new NewestUpdateConfigurator();
                break;
            case OLDER:
                conf = new OlderUpdateConfigurator();
                break;
            default:
                return null;
        }
        
        UpdateMechanism propagation = conf.configure(params);
        return new Tuple2<>(name, propagation);
    }
}
