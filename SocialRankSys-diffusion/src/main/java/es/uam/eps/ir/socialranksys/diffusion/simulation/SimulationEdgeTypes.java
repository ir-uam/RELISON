/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.simulation;

/**
 * Constants for edge types.
 * @author Javier Sanz-Cruzado Puig
 */
public class SimulationEdgeTypes
{
    /**
     * Identifier for the links in the training graph.
     */
    public static final int TRAINING = 0;
    /**
     * Identifier for the recommended links.
     */
    public static final int RECOMMEND = 1;
}
