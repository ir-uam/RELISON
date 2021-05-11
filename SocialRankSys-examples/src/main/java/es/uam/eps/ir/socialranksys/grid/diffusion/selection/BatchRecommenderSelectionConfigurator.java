/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.selection;

import es.uam.eps.ir.socialranksys.diffusion.selections.BatchRecommenderSelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.grid.Parameters;

import java.io.Serializable;

/**
 * Configures a selection mechanism that propagates a fixed number of own information pieces, and repropagates
 * pieces which have been received through recommended links with a certain probability, and through not recommended
 * links with other.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see BatchRecommenderSelectionMechanism
 */
public class BatchRecommenderSelectionConfigurator<U extends Serializable,I extends Serializable, F> implements SelectionConfigurator<U,I, F>
{
    /**
     * Identifier for the number of own pieces of information to propagate.
     */
    private final static String NUMOWN = "numOwn";
    /**
     * Identifier for the number of received pieces to propagate.
     */
    private final static String NUMREC = "numRec";
    /**
     * Identifier for the probability of selecting a piece received by a recommended user.
     */
    private final static String PROB = "prob";
    /**
     * Identifier for the number of propagated pieces of information to repropagate.
     */
    private final static String NUMREPR = "numRepr";
    /**
     * Identifier for the direction propagated information pieces come from.
     */
    private final static String ORIENTATION = "orientation";
    
    @Override
    public SelectionMechanism<U,I, F> configure(Parameters params)
    {
        int numOwn = params.getIntegerValue(NUMOWN);
        int numRec = params.getIntegerValue(NUMREC);
        double prob = params.getDoubleValue(PROB);
        EdgeOrientation orient = params.getOrientationValue(ORIENTATION);
        
        if(params.getIntegerValues().containsKey(NUMREPR))
        {
            int numRepr = params.getIntegerValue(NUMREPR);
            return new BatchRecommenderSelectionMechanism<>(numOwn, numRec, numRepr, prob, orient);
        }
        return new BatchRecommenderSelectionMechanism<>(numOwn, numRec, prob, orient);
    }
    
}
