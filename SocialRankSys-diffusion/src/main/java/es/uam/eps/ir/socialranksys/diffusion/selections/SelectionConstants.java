/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.selections;

/**
 * Constants for the selection mechanisms.
 * @author Javier Sanz-Cruzado Puig
 */
public class SelectionConstants 
{
    /**
     * Value for selecting that no information pieces of the corresponding type will be propagated.
     */
    public final static int NONE = 0;
    /**
     * Value for selecting that all available information pieces of the corresponding type will be propagated.
     */
    public final static int ALL = -1;
}
