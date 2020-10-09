/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.selections;

import java.io.Serializable;

/**
 * Selection mechanism that only selects a set of own pieces of information.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users
 * @param <I> Type of the information pieces
 * @param <P> Type of the parameters
 */
public class OnlyOwnInformationSelectionMechanism<U extends Serializable, I extends Serializable, P> extends CountSelectionMechanism<U,I,P> 
{  
    /**
     * Constructor.
     * @param numOwn number of own information pieces to propagate.
     */
    public OnlyOwnInformationSelectionMechanism(int numOwn)
    {
        super(numOwn,SelectionConstants.NONE);
    }
    
    /**
     * Constructor
     * @param numOwn number of own information pieces to propagate.
     * @param numRepr number of information pieces to repropagate.
     */
    public OnlyOwnInformationSelectionMechanism(int numOwn, int numRepr)
    {
        super(numOwn, SelectionConstants.NONE, numRepr);
    }
}
