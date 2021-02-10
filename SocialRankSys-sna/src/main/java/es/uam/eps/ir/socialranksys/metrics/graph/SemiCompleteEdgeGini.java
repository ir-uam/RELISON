/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.graph;

/**
 * Computes the value for Gini for the different pairs of nodes. Self-loops are counted in a separate category.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SemiCompleteEdgeGini<U> extends EdgeGini<U>
{
    /**
     * Constructor.
     */
    public SemiCompleteEdgeGini()
    {
        super(EdgeGiniMode.SEMICOMPLETE);
    }
}
