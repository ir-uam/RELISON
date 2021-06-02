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
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Selection mechanism that only propagates those received pieces which have been received (at least) a fixed
 * number of times. It propagates any information piece that the user has received, from, at least, a
 * given proportion of his neighbors.
 *
 * <p>
 *      <b>Reference:</b>  D. Kempe, J. Kleinberg, and E. Tardos. Maximizing the spread of influence through a social network, KDD 2003, pp. 137–146 (2003).
 * </p>
 *
 * It fixes the (maximum) number of received pieces to propagate.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class LimitedCountThresholdSelectionMechanism<U extends Serializable,I extends Serializable,P> extends CountSelectionMechanism<U,I,P>
{
    /**
     * Number of users that transmit an information piece before a user chooses to share it.
     */
    private final int threshold;

    /**
     * Orientation for selecting the neighborhood.
     */
    private final EdgeOrientation orientation;

    /**
     * Constructor.
     * @param numOwn        number of own pieces to propagate.
     * @param numRec        maximum number of received pieces to propagate.
     * @param threshold     number of users that transmit an information piece before a user chooses to share it.
     * @param orientation   orientation for selecting the number of neighbors to consider.
     */
    public LimitedCountThresholdSelectionMechanism(int numOwn, int numRec, int threshold, EdgeOrientation orientation)
    {
        super(numOwn, numRec);
        this.threshold = threshold;
        this.orientation = orientation;
    }

    /**
     *
     * @param numOwn        number of own pieces to propagate.
     * @param numRec        maximum number of received pieces to propagate.
     * @param threshold     number of users that transmit an information piece before a user chooses to share it.
     * @param orientation   orientation for selecting the number of neighbors to consider.
     * @param numRepr       number of pieces to repropagate.
     */
    public LimitedCountThresholdSelectionMechanism(int numOwn, int numRec, int threshold, EdgeOrientation orientation, int numRepr)
    {
        super(numOwn, numRec, numRepr);
        this.threshold = threshold;
        this.orientation = orientation;
    }
    
    @Override
    protected List<PropagatedInformation> getReceivedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {
        // We first obtain al the possible information to repropagate:
        int userId = data.getUserIndex().object2idx(user.getUserId());
        List<PropagatedInformation> aux = new ArrayList<>();

        // Select the pieces to propagate.
        user.getReceivedInformation().forEach(info ->
        {
            if(info.getTimes() >= this.threshold)
            {
                aux.add(new PropagatedInformation(info.getInfoId(), numIter, userId));
            }
        });

        // We do randomly select the information pieces to propagate among the ones received earlier.
        return this.getPropagatedInformation(userId, this.getNumReceived(), numIter, aux);
    }    
}
