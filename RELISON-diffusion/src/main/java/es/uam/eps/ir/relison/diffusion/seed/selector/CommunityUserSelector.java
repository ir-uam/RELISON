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
import es.uam.eps.ir.relison.sna.community.Communities;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Selects the users belonging to one or several communities of a given partition of the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 */
public class CommunityUserSelector<U> implements UserSelector<U>
{
    /**
     * The community partition of the network.
     */
    private final Communities<U> communities;
    /**
     * The identifiers of the communities whose users are selected.
     */
    private final Set<Integer> targetCommunities;

    /**
     * Selects the users of a single community.
     * @param communities the community partition.
     * @param community   the identifier of the community whose users are selected.
     */
    public CommunityUserSelector(Communities<U> communities, int community)
    {
        this.communities = communities;
        this.targetCommunities = new LinkedHashSet<>();
        this.targetCommunities.add(community);
    }

    /**
     * Selects the users of several communities.
     * @param communities the community partition.
     * @param targets     the identifiers of the communities whose users are selected.
     */
    public CommunityUserSelector(Communities<U> communities, Collection<Integer> targets)
    {
        this.communities = communities;
        this.targetCommunities = new LinkedHashSet<>(targets);
    }

    @Override
    public Set<U> select(Graph<U> graph)
    {
        Set<U> set = new LinkedHashSet<>();
        for (int community : this.targetCommunities)
        {
            this.communities.getUsers(community).forEach(set::add);
        }
        return set;
    }
}
