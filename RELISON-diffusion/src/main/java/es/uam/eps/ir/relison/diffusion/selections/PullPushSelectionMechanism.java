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
 * Selection mechanism following the original one proposed by the push, pull and push-pull models: all the known information
 * (i.e. all the received and already propagated pieces) is shared with the receivers.
 *
 * <p>
 * <b>Reference:</b> A. Demers, D. Greene, C. Hauser, W. Irish, J. Larson. Epidemic algorithms for replicated database maintenance. ACM PODC 1987, pp. 1-12 (1987)
 * </p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 *
 */
public class PullPushSelectionMechanism<U extends Serializable,I extends Serializable,P> extends CountSelectionMechanism<U,I,P>
{
    /**
     * Constructor.
     * @param numOwn number of own pieces of information to propagate.
     */
    public PullPushSelectionMechanism(int numOwn)
    {
        super(numOwn, SelectionConstants.ALL, SelectionConstants.ALL);
    }    
}
