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
 * Selects the propagated pieces.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class ThresholdSelectionMechanism<U extends Serializable,I extends Serializable,P> extends CountSelectionMechanism<U,I,P> 
{ 
    /**
     * Proportion of users that transmit an information piece before it is released.
     */
    private final double threshold;
    
    /**
     * Constructor.
     * @param numOwn Number of own pieces to propagate
     * @param numRec Maximum number of pieces to repropagate
     * @param threshold Proportion of users that transmit an information piece before it is released.
     */
    public ThresholdSelectionMechanism(int numOwn, int numRec, double threshold)
    {
        super(numOwn, numRec);
        this.threshold = threshold;
    }
    
    @Override
    protected List<PropagatedInformation> getReceivedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {
        List<PropagatedInformation> receivedToPropagate = new ArrayList<>();
        int userId = data.getUserIndex().object2idx(user.getUserId());
        double numThreshold = data.getGraph().getAdjacentNodesCount(user.getUserId())*this.threshold;
        List<PropagatedInformation> aux = new ArrayList<>();
        
        // Select the received information to propagate.
        user.getReceivedInformation().forEach(info -> 
        {
            if((info.getTimes()+0.0) > numThreshold)
            {
                aux.add(new PropagatedInformation(info.getInfoId(), numIter, userId));
            }
        });
               
        if(aux.size() <= this.getNumPropagate())
        {
            receivedToPropagate = aux;
        }
        else
        {
            for(int i = 0; i < this.getNumPropagate(); ++i)
            {
                int idx = rng.nextInt(aux.size());
                receivedToPropagate.add(aux.get(idx));
                aux.remove(aux.get(idx));
            }
        }
        
        return receivedToPropagate;
    }    
}
