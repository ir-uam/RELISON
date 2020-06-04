/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.selections;

import java.io.Serializable;

/**
 * Selects the propagated pieces. This algorithm propagates a certain number of information pieces each
 * iteration, chosen randomly from the own pieces of information. Then, it repropagates every piece
 * of information that the user has repropagated in real life.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class AllRealPropagatedSelectionMechanism<U extends Serializable,I extends Serializable,P> extends CountRealPropagatedSelectionMechanism<U,I,P>
{
    /**
     * Constructor.
     * @param numOwn Number of own information pieces to propagate for each user and iteration.
     */
    public AllRealPropagatedSelectionMechanism(int numOwn)
    {
        super(numOwn, SelectionConstants.ALL);
    }
    
    /**
     * Constructor.
     * @param numOwn Number of own information pieces to propagate for each user and iteration.
     * @param numRepr Number of propagated information pieces to repropagate for each user and iteration.
     */
    public AllRealPropagatedSelectionMechanism(int numOwn, int numRepr)
    {
        super(numOwn, SelectionConstants.ALL, numRepr);
    }
}
