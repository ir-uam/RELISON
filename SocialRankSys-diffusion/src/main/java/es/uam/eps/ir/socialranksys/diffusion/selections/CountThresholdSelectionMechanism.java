/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Aut�noma
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
 * Selection mechanism that only propagates those received pieces which have been received (at least) a fixed
 * number of times. It propagates any information piece that the user has received at least such amount of times.
 *
 * <p>
 *      <b>Reference:</b>  D. Kempe, J. Kleinberg, and E. Tardos. Maximizing the spread of influence through a social network, KDD 2003, pp. 137–146 (2003).
 * </p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class CountThresholdSelectionMechanism<U extends Serializable,I extends Serializable, P> extends CountSelectionMechanism<U,I,P> 
{
    /**
     * Number of users that transmit an information piece before a user chooses to share it.
     */
    private final int threshold;
    
    /**
     * Constructor.
     * @param numOwn    number of own pieces to propagate.
     * @param threshold number of users that transmit an information piece before a user chooses to share it.
     */
    public CountThresholdSelectionMechanism(int numOwn, int threshold)
    {
        super(numOwn, SelectionConstants.NONE, SelectionConstants.NONE);
        this.threshold = threshold;
    }
    
    /**
     * Constructor.
     * @param numOwn    number of own pieces to propagate.
     * @param threshold number of users that transmit an information piece before a user chooses to share it.
     * @param numRepr   number of already propagated pieces to repropagate.
     */
    public CountThresholdSelectionMechanism(int numOwn, int threshold, int numRepr)
    {
        super(numOwn, SelectionConstants.NONE, numRepr);
        this.threshold = threshold;
    }
    
    @Override
    protected List<PropagatedInformation> getReceivedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {
        int userId = data.getUserIndex().object2idx(user.getUserId());
        List<PropagatedInformation> receivedToPropagate = new ArrayList<>();

        // Select the pieces to propagate.
        user.getReceivedInformation().forEach(info -> 
        {
            if(info.getTimes() >= this.threshold)
            {
                receivedToPropagate.add(new PropagatedInformation(info.getInfoId(), numIter, userId));
            }
        });
        
        return receivedToPropagate;
    }    
}
