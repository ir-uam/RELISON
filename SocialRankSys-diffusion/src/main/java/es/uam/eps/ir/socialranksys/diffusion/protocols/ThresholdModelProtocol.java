/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.protocols;

import es.uam.eps.ir.socialranksys.diffusion.expiration.AllNotPropagatedExpirationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.propagation.AllNeighborsPropagationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.LimitedProportionThresholdSelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.sight.AllNotPropagatedSightMechanism;
import es.uam.eps.ir.socialranksys.diffusion.update.OlderUpdateMechanism;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.io.Serializable;

/**
 * Threshold model protocol.
 * <br>
 * Main characteristics
 * <ul>
 *  <li><b>Selection mechanism:</b> Each user takes a fixed number of information pieces from his own list. In the case of the
 *  received list, each piece of information will be propagated depending on the proportion of the neighbors that has propagated
 *  the information to him. If this proportion exceeds a threshold, then, the information will be propagated.</li>
 *  <li><b>Expiration mechanism:</b> In this case, information pieces never expire: they will not be discarded.</li>
 *  <li><b>Update mechanism:</b> Since information pieces remain "in the memory of the users" once they are received, if a piece
 *  is received again, the list of users will be updated to contain the whole lot of users that have propagated the piece to the
 *  corresponding user</li>
 *  <li><b>Propagation mechanism:</b> In order to maximize the spreading, the information reaches all the followers of the user that
 *  propagates the information</li>
 *  <li><b>Sight mechanism:</b> All users see all the received information pieces.</li>
 * </ul>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * <p>
 * <b>Reference:</b>  D. Kempe, J. Kleinberg, and E. Tardos. Maximizing the spread of influence through a social network, KDD 2003, pp. 137–146 (2003).
 * </p>
 *
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <F> Type of the parameters.
 */
public class ThresholdModelProtocol<U extends Serializable,I extends Serializable, F> extends Protocol<U,I, F>
{
    /**
     * Constructor.
     * @param numOwn    number of own pieces to propagate each iteration.
     * @param numRec    number of pieces to repropagate.
     * @param threshold threshold that has to be surpassed.
     */
    public ThresholdModelProtocol(int numOwn, int numRec, double threshold) 
    {
        super(new LimitedProportionThresholdSelectionMechanism<>(numOwn, numRec, threshold, EdgeOrientation.OUT),
              new AllNotPropagatedExpirationMechanism<>(),
              new OlderUpdateMechanism(),
              new AllNeighborsPropagationMechanism<>(EdgeOrientation.IN),
              new AllNotPropagatedSightMechanism<>());
    }
    
}
