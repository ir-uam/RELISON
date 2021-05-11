/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.selection;

import es.uam.eps.ir.socialranksys.diffusion.selections.ProportionThresholdSelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.grid.Parameters;

import java.io.Serializable;

/**
 * Configures a selection mechanism that propagates a fixed number of own information pieces, and repropagates
 * pieces which have been received from (at least) a given proportion of the user neighbors.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see ProportionThresholdSelectionMechanism
 */
public class ProportionThresholdSelectionConfigurator<U extends Serializable,I extends Serializable, F> implements SelectionConfigurator<U,I, F>
{
    /**
     * Identifier for the number of own elements to propagate.
     */
    private final static String NUMOWN = "numOwn";
    /**
     * Identifier for the number of elements needed for repropagating an individual piece.
     */
    private final static String THRESHOLD = "threshold";
    /**
     * Identifier for the number of propagated pieces of information to repropagate.
     */
    private final static String NUMREPR = "numRepr";
    /**
     * Identifier for the direction propagated information pieces come from.
     */
    private final static String ORIENTATION = "orientation";
    @Override
    public SelectionMechanism<U, I, F> configure(Parameters params)
    {
        int numOwn = params.getIntegerValue(NUMOWN);
        double threshold = params.getDoubleValue(THRESHOLD);
        EdgeOrientation orient = params.getOrientationValue(ORIENTATION);
        if(params.getIntegerValues().containsKey(NUMREPR))
        {
            int numRepr = params.getIntegerValue(NUMREPR);
            return new ProportionThresholdSelectionMechanism<>(numOwn, threshold, orient, numRepr);
        }
        else
        {
            return new ProportionThresholdSelectionMechanism<>(numOwn, threshold, orient);
        }
    }
    
    
}
