/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
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
import java.util.HashMap;
import java.util.Map;

/**
 * The user only sees a fixed number of the received objects.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class CountSightMechanism<U extends Serializable,I extends Serializable,P> implements SightMechanism<U,I,P>
{
    /**
     * Number of pieces of information that a user sees in a single iteration.
     */
    private final int numSight;
    
    private final Map<U, Integer> map;
    /**
     * Constructor.
     * @param numSight Number of pieces of information that a user sees in a single iteration. 
     */
    public CountSightMechanism(int numSight)
    {
        this.numSight = numSight;
        map = new HashMap<>();
    }

    @Override
    public boolean seesInformation(UserState<U> user, Data<U,I,P> data, PropagatedInformation prop)
    {
        U u = user.getUserId();
        if(this.map.get(u) < this.numSight)
        {
            this.map.put(u, this.map.get(u)+1);
            return !user.containsPropagatedInformation(prop.getInfoId());
        }
        
        return false;
    }
    
    @Override
    public void resetSelections(Data<U,I,P> data)
    {
        map.clear();
    }
    
    
    
    
}
