/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.update;


import es.uam.eps.socialranksys.diffusion.update.IndependentCascadeModelUpdateMechanism;
import es.uam.eps.socialranksys.diffusion.update.UpdateMechanism;

/**
 * Configures a Independent Cascade Model update mechanism.
 * @author Javier Sanz-Cruzado Puig
 */
public class IndependentCascadeModelUpdateConfigurator implements UpdateConfigurator
{
    
    @Override
    public UpdateMechanism configure(UpdateParamReader params)
    {
        return new IndependentCascadeModelUpdateMechanism();
    }
}
