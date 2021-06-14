/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.selection;

import es.uam.eps.ir.relison.diffusion.selections.LimitedProportionThresholdSelectionMechanism;
import es.uam.eps.ir.relison.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.grid.Parameters;

import java.io.Serializable;

/**
 * Configures a selection mechanism that propagates a fixed number of own information pieces, and repropagates
 * a fixed number of pieces, chosen from those who have been received (at least) from a fraction of his neighbors.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see LimitedProportionThresholdSelectionMechanism
 */
public class LimitedProportionThresholdSelectionConfigurator<U extends Serializable,I extends Serializable, F> implements SelectionConfigurator<U,I, F>
{
    /**
     * Identifier for the number of own elements to propagate.
     */
    private final static String NUMOWN = "numOwn";
    /**
     * Identifier for the number of elements to repropagate.
     */
    private final static String NUMREC = "numRec";
    /**
     * Identifier for the proportion of elements needed for repropagating an individual piece.
     */
    private final static String THRESHOLD = "threshold";
    /**
     * Identifier for the direction propagated information pieces come from.
     */
    private final static String ORIENTATION = "orientation";
    /**
     * Identifier for the number of propagated pieces of information to repropagate.
     */
    private final static String NUMREPR = "numRepr";
    
    @Override
    public SelectionMechanism<U, I, F> configure(Parameters params)
    {
        int numOwn = params.getIntegerValue(NUMOWN);
        int numRec = params.getIntegerValue(NUMREC);
        EdgeOrientation orient = params.getOrientationValue(ORIENTATION);
        double threshold = params.getDoubleValue(THRESHOLD);

        if(params.getIntegerValues().containsKey(NUMREPR))
        {
            int numRepr = params.getIntegerValue(NUMREPR);
            return new LimitedProportionThresholdSelectionMechanism<>(numOwn, numRec, threshold, orient, numRepr);
        }
        return new LimitedProportionThresholdSelectionMechanism<>(numOwn, numRec, threshold, orient);
    }
    
    
}
