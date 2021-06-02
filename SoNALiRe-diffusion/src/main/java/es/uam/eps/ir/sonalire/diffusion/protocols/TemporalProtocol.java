/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.protocols;

import es.uam.eps.ir.sonalire.diffusion.expiration.AllNotRealPropagatedTimestampExpirationMechanism;
import es.uam.eps.ir.sonalire.diffusion.propagation.AllNeighborsPropagationMechanism;
import es.uam.eps.ir.sonalire.diffusion.selections.LooseTimestampBasedSelectionMechanism;
import es.uam.eps.ir.sonalire.diffusion.selections.PureTimestampBasedSelectionMechanism;
import es.uam.eps.ir.sonalire.diffusion.sight.AllNotDiscardedNorPropagatedSightMechanism;
import es.uam.eps.ir.sonalire.diffusion.update.NewestUpdateMechanism;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;

import java.io.Serializable;

/**
 * Simulation protocol that considers the timestamps. This protocol aims at showing the true propagation
 * of information through the network: it only propagates new information when the simulation iteration
 * represents the timestamp of creation of the information piece, and a user propagates contents from
 * other users only if they had been previously propagated in real life.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class TemporalProtocol<U extends Serializable, I extends Serializable, F> extends Protocol<U,I, F>
{
    /**
     * Constructor.
     * @param pure true if information received from other users cannot be repropagated after the timestamp
     * of the repropagation has been visited, false if it can.
     */
    public TemporalProtocol(boolean pure)
    {
        super ((pure) ? new PureTimestampBasedSelectionMechanism<>() : new LooseTimestampBasedSelectionMechanism<>(),
               new AllNotRealPropagatedTimestampExpirationMechanism<>(),
               new NewestUpdateMechanism(),
               new AllNeighborsPropagationMechanism<>(EdgeOrientation.IN),
               new AllNotDiscardedNorPropagatedSightMechanism<>());
    }
}
