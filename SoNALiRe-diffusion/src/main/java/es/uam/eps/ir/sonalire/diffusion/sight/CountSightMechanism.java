/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.sight;

import es.uam.eps.ir.sonalire.diffusion.data.Data;
import es.uam.eps.ir.sonalire.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.sonalire.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.*;

/**
 * The user sees (at most) a fixed number of (different) information pieces each iteration. The pieces are chosen
 * randomly among all the received ones. If two instances of the same piece are received and one of them is seen,
 * we assume that both have been.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class CountSightMechanism<U extends Serializable,I extends Serializable,P> implements SightMechanism<U,I,P>
{
    /**
     * Number of pieces of information that a user sees in a single iteration.
     */
    private final int numSight;

    /**
     * Constructor.
     * @param numSight number of pieces of information that a user sees (at most) in a single iteration.
     */
    public CountSightMechanism(int numSight)
    {
        this.numSight = numSight;
    }


    @Override
    public List<PropagatedInformation> seesInformation(UserState<U> user, Data<U, I, P> data, List<PropagatedInformation> prop)
    {
        Map<Integer, List<PropagatedInformation>> info = new HashMap<>();
        List<Integer> pieces = new ArrayList<>();

        prop.forEach(piece ->
        {
            int id = piece.getInfoId();
            if(!info.containsKey(id))
            {
                info.put(id, new ArrayList<>());
                pieces.add(id);
            }
            info.get(id).add(piece);
        });

        List<PropagatedInformation> defList = new ArrayList<>();
        if(pieces.size() <= this.numSight)
        {
            defList.addAll(prop);
        }
        else
        {
            Collections.shuffle(pieces);
            pieces.subList(0, this.numSight).forEach(id -> defList.addAll(info.get(id)));
        }

        return defList;
    }

    @Override
    public void resetSelections(Data<U,I,P> data)
    {
    }
    
}
