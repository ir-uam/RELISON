/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index;

/**
 * Algorithms for computing the average shortest path length of a graph (in case the graph is not strongly connected).
 * <ul>
 *  <li><b>NONE:</b> Index is not configured </li>
 *  <li><b>READ:</b> Read mode</li>
 *  <li><b>WRITE:</b> Write mode</li>
 * </ul>
 * @author Javier Sanz-Cruzado Puig
 */
public enum ContentIndexMode {
    NONE, 
    READ,
    WRITE
}
