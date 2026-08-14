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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Selects every user of the network. This is the default selector, reproducing the behaviour of seeding all users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 */
public class AllUsersSelector<U> implements UserSelector<U>
{
    @Override
    public Set<U> select(Graph<U> graph)
    {
        Set<U> set = new LinkedHashSet<>();
        graph.getAllNodes().forEach(set::add);
        return set;
    }
}
