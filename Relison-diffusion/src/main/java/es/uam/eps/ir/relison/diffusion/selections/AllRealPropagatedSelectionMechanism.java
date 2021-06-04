/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.selections;

import java.io.Serializable;

/**
 * Algorithm that chooses randomly some pieces from the own set of information pieces, and it only propagates
 * those received information pieces from other users that the user has propagated during the actual diffusion
 * procedure.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class AllRealPropagatedSelectionMechanism<U extends Serializable,I extends Serializable,P> extends CountRealPropagatedSelectionMechanism<U,I,P>
{
    /**
     * Constructor.
     * @param numOwn number of own information pieces to propagate for each user and iteration.
     */
    public AllRealPropagatedSelectionMechanism(int numOwn)
    {
        super(numOwn, SelectionConstants.ALL);
    }
    
    /**
     * Constructor.
     * @param numOwn    number of own information pieces to propagate for each user and iteration.
     * @param numRepr   number of propagated information pieces to repropagate for each user and iteration.
     */
    public AllRealPropagatedSelectionMechanism(int numOwn, int numRepr)
    {
        super(numOwn, SelectionConstants.ALL, numRepr);
    }
}
