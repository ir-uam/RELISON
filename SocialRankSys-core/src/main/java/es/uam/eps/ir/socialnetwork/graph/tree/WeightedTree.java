/*
 *  Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph.tree;

import es.uam.eps.ir.socialnetwork.graph.WeightedGraph;

/**
 * Interface for managing and creating tree graphs.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the nodes.
 */
public interface WeightedTree<U> extends Tree<U>, WeightedGraph<U>
{
}
