/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.distance.modes;

/**
 * Algorithms for computing the average shortest path length of a graph (in case the graph is not strongly connected).
 * <ul>
 *  <li><b>COMPONENTS:</b> The average distance is computed as the average ASL of the </li>
 *  <li><b>NOTINFINITEDISTANCE:</b> All not infinite distances are averaged</li>
 * </ul>
 *
 * @author Javier Sanz-Cruzado Puig
 */
public enum ASLMode
{
    /**
     * The average distance is computed as the average ASL over the communities.
     */
    COMPONENTS,
    /**
     * The average is computed over all the non infinite distances.
     */
    NONINFINITEDISTANCES
}
