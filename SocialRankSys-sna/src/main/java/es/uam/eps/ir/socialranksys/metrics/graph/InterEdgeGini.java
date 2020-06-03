/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.graph;

/**
 * Computes the value for Gini for the different pairs of different nodes.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class InterEdgeGini<U> extends EdgeGini<U>
{
    /**
     * Constructor
     */
    public InterEdgeGini()
    {
        super(EdgeGiniMode.INTERLINKS);
    }
}
