/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.selections;

import java.io.Serializable;

/**
 * Selection for the pure Push-Pull Model selection mechanism: every item which has been received or
 * propagated is repropagated in the next iteration.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class PullPushSelectionMechanism<U extends Serializable,I extends Serializable,P> extends CountSelectionMechanism<U,I,P>
{
    /**
     * Constructor.
     * @param numOwn Number of own pieces of information to propagate.
     */
    public PullPushSelectionMechanism(int numOwn)
    {
        super(numOwn, SelectionConstants.ALL, SelectionConstants.ALL);
    }    
}
