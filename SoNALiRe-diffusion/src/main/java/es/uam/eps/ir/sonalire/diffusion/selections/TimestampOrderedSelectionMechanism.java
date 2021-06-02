/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.selections;

import es.uam.eps.ir.sonalire.diffusion.data.Data;
import es.uam.eps.ir.sonalire.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.sonalire.diffusion.simulation.SimulationState;
import es.uam.eps.ir.sonalire.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Selection mechanism that takes into account the real timestamp of the information pieces to propagate the information owned
 * by the user. Instead of randomly selecting a fixed number of information pieces, this mechanism selects the oldest
 * available information.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> the type of the users.
 * @param <I> the type of the information pieces.
 * @param <P> the type of the features.
 */
public class TimestampOrderedSelectionMechanism<U extends Serializable, I extends Serializable, P> extends CountSelectionMechanism<U,I,P>  
{
    /**
     * Constructor.
     * @param numOwn        number of own information pieces to propagate.
     * @param numPropagate  number of received information pieces to propagate.
     */
    public TimestampOrderedSelectionMechanism(int numOwn, int numPropagate) 
    {
        super(numOwn, numPropagate);
    }

    /**
     * Constructor.
     * @param numOwn            number of own information pieces to propagate.
     * @param numPropagate      number of received information pieces to propagate.
     * @param numRepropagate    number of propagated information pieces to repropagate.
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
