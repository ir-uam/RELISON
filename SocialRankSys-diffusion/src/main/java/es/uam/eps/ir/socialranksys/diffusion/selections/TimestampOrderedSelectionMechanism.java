/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.selections;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.socialranksys.diffusion.simulation.SimulationState;
import es.uam.eps.ir.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Selection mechanism that takes the real timestamps of the users into account.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> The type of the users.
 * @param <I> The type of the information pieces.
 * @param <P> The type of the features.
 */
public class TimestampOrderedSelectionMechanism<U extends Serializable, I extends Serializable, P> extends CountSelectionMechanism<U,I,P>  
{
    /**
     * Constructor.
     * @param numOwn Number of own information pieces to propagate.
     * @param numPropagate Number of received information pieces to propagate.
     */
    public TimestampOrderedSelectionMechanism(int numOwn, int numPropagate) 
    {
        super(numOwn, numPropagate);
    }

    /**
     * Constructor.
     * @param numOwn Number of own information pieces to propagate.
     * @param numPropagate Number of received information pieces to propagate.
     * @param numRepropagate Number of propagated information pieces to repropagate.
     */
    public TimestampOrderedSelectionMechanism(int numOwn, int numPropagate, int numRepropagate) 
    {
        super(numOwn, numPropagate, numRepropagate);
    }

    @Override
    protected List<PropagatedInformation> getOwnInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {
        int uidx = data.getUserIndex().object2idx(user.getUserId());
        List<PropagatedInformation> prop = new ArrayList<>();
        user.getOwnInformation().sorted((x,y) -> 
        {
            I i1 = data.getInformationPiecesIndex().idx2object(x.getInfoId());
            I i2 = data.getInformationPiecesIndex().idx2object(y.getInfoId());
            long time = data.getTimestamp(i1) - data.getTimestamp(i2);
            
            if(time < 0)
                return -1;
            else if (time > 0)
                return 1;
            else
                return 0;
        }).limit(this.getNumOwn()).forEach(piece -> prop.add(new PropagatedInformation(piece.getInfoId(), numIter, uidx)));
        
        return prop;
    }

    
    
}
