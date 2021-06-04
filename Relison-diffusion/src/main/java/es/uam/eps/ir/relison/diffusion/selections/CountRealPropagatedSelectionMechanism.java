/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.selections;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.relison.diffusion.simulation.SimulationState;
import es.uam.eps.ir.relison.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Selection mechanism that chooses randomly a fixed number of information pieces owned by the propagating user, and,
 * from the received ones, it randomly chooses a fixed number of pieces which the user did propagate during the actual
 * diffusion procedure (in a real life scenario).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class CountRealPropagatedSelectionMechanism<U extends Serializable,I extends Serializable,P> extends CountSelectionMechanism<U,I,P>
{
    /**
     * Constructor.
     * @param numOwn        the number of own information pieces to propagate for each user and iteration.
     * @param numPropagate  the number of received information to propagate for each user and iteration.
     */
    public CountRealPropagatedSelectionMechanism(int numOwn, int numPropagate)
    {
        super(numOwn, numPropagate, SelectionConstants.NONE);
    }

    /**
     * Constructor.
     * @param numOwn            the number of own information pieces to propagate for each user and iteration.
     * @param numPropagate      the number of received information to propagate for each user and iteration.
     * @param numRepropagate    the number of information pieces to repropagate for each user and iteration.
     */
    public CountRealPropagatedSelectionMechanism(int numOwn, int numPropagate, int numRepropagate)
    {
        super(numOwn, numPropagate, numRepropagate);
    }
    
    @Override
    protected List<PropagatedInformation> getReceivedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {

        List<PropagatedInformation> realProp = new ArrayList<>();
        int userId = data.getUserIndex().object2idx(user.getUserId());
        
        U u = user.getUserId();
        user.getReceivedInformation().forEach(info -> 
        {
            // for each information piece, we check whether the user did propagate it in real time.
            I i = data.getInformationPiecesIndex().idx2object(info.getInfoId());
            if(data.isRealRepropagatedPiece(u, i))
            {
                realProp.add(new PropagatedInformation(info.getInfoId(), numIter, userId));
            }
        });

        // we obtain a selection of such pieces.
        return this.getPropagatedInformation(userId, this.getNumReceived(), numIter, realProp);
    }
}
