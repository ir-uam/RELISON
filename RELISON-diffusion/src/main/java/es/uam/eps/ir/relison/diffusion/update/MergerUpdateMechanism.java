/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.update;


import es.uam.eps.ir.relison.diffusion.data.PropagatedInformation;

/**
 * Updates the previously received elements with information obtained from the new ones.
 * Merges both pieces of propagated information.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MergerUpdateMechanism implements UpdateMechanism
{

    @Override
    public PropagatedInformation updateSeen(PropagatedInformation oldInfo, PropagatedInformation newInfo)
    {
        return newInfo.update(oldInfo);
    }

    @Override
    public PropagatedInformation updateDiscarded(PropagatedInformation oldInfo, PropagatedInformation newInfo) 
    {
        return newInfo.update(oldInfo);
    }
    
}
