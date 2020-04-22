/* 
 *  Copyright (C) 2015 Information Retrieval Group at Universidad AutÃ³noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph.multigraph;

/**
 * Interface for directed unweighted multigraphs
 * @author Javier Sanz-Cruzado Puig
 * @param <V> Type of the vertices.
 */
public interface DirectedUnweightedMultiGraph<V> extends UnweightedMultiGraph<V>, DirectedMultiGraph<V>
{   
}
