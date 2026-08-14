/*
 *  Copyright (C) 2024 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.seed.selector;

import es.uam.eps.ir.relison.graph.Graph;

import java.util.Set;

/**
 * Selects the subset of the users of a network that a {@link es.uam.eps.ir.relison.diffusion.seed.PieceSeeder seeder}
 * gives information pieces to. Every node of the network still takes part in the diffusion (as a potential receiver /
 * repropagator); the selection only decides <em>who creates content</em>.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 */
public interface UserSelector<U>
{
    /**
     * Selects the users to be seeded with information pieces.
     * @param graph the social network.
     * @return the set of users to seed.
     */
    Set<U> select(Graph<U> graph);
}
