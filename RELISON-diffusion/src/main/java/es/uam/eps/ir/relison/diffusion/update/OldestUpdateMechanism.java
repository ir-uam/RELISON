/* 
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.update;


import es.uam.eps.ir.relison.diffusion.data.PropagatedInformation;

/**
 * Update mechanism that just takes the oldest information piece.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * */
public class OldestUpdateMechanism implements UpdateMechanism
{

    @Override
    public PropagatedInformation updateSeen(PropagatedInformation oldInfo, PropagatedInformation newInfo)
    {
        // We keep the old
        return oldInfo;
    }

    @Override
    public PropagatedInformation updateDiscarded(PropagatedInformation oldInfo, PropagatedInformation newInfo)
    {
        return oldInfo;
    }
    
}
