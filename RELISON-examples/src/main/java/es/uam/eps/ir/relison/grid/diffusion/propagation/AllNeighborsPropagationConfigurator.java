/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.propagation;

import es.uam.eps.ir.relison.diffusion.propagation.AllNeighborsPropagationMechanism;
import es.uam.eps.ir.relison.diffusion.propagation.PropagationMechanism;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.grid.Parameters;

import java.io.Serializable;

/**
 * Configures an propagation mechanism that distributes pieces to all the neighbors of the propagating user.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see AllNeighborsPropagationMechanism
 */
public class AllNeighborsPropagationConfigurator<U extends Serializable,I extends Serializable, F> implements PropagationConfigurator<U,I, F>
{
    /**
     * Identifier for the neighbors towards whom we want to propagate the information.
     */
    private static String ORIENTATION = "orientation";

    @Override
    public PropagationMechanism<U, I, F> configure(Parameters params)
    {
        EdgeOrientation orient = params.getOrientationValue(ORIENTATION);
        return new AllNeighborsPropagationMechanism<>(orient);
    }
}
