/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.selection;

import es.uam.eps.ir.socialranksys.diffusion.selections.PureBatchRecommenderSelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.io.Serializable;

/**
 * Configures a Pure Recommender Batch selection mechanism.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class PureRecommenderBatchSelectionConfigurator<U extends Serializable,I extends Serializable,P> implements SelectionConfigurator<U,I,P>
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
    public SelectionMechanism<U,I,P> configure(SelectionParamReader params)
    {
        int numOwn = params.getParams().getIntegerValue(NUMOWN);
        int numRec = params.getParams().getIntegerValue(NUMREC);
        double prob = params.getParams().getDoubleValue(PROB);
        EdgeOrientation orient = params.getParams().getOrientationValue(ORIENTATION);
        
        if(params.getParams().getIntegerValues().containsKey(NUMREPR))
        {
            int numRepr = params.getParams().getIntegerValue(NUMREPR);
            return new PureBatchRecommenderSelectionMechanism<>(numOwn, numRec, numRepr, prob, orient);
        }
        return new PureBatchRecommenderSelectionMechanism<>(numOwn, numRec, prob, orient);
    }
    
}
