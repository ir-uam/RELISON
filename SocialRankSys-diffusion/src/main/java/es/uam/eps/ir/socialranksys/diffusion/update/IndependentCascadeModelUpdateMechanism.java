/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.update;


import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;

/**
 * Update Mechanism for the Independent Cascade Model.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * */
public class IndependentCascadeModelUpdateMechanism implements UpdateMechanism
{

    @Override
    public PropagatedInformation updateSeen(PropagatedInformation oldInfo, PropagatedInformation newInfo)
    {
        // In this case, the old information should not exist, so only the new one is returned.
        return newInfo;
    }

    @Override
    public PropagatedInformation updateDiscarded(PropagatedInformation oldInfo, PropagatedInformation newInfo)
    {
        // The previously discarded information remains discarded. In this model, only the new model information 
        // has the opportunity to be propagated.That information is the information formed by users that had not
        // previously propagated information. As the information cannot arrive from the same user twice, then
        // only newInfo is returned
        
        return newInfo;
        /*
        Set<Integer> set1 = new HashSet<>(oldInfo.getCreators());
        Set<Integer> set2 = new HashSet<>(oldInfo.getCreators());
        set1.retainAll(set2);
        
        
        if(!set1.isEmpty())
        {
            int[] array = new int[set1.size()];
            return new PropagatedInformation(newInfo.getInfoId(), newInfo.getTimestamp(),array);
        }
        return null;*/
    }
    
}
