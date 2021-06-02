/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.community;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class that relates the nodes of a graph with communities.
 *
 * @param <U> type of the nodes.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Communities<U>
{
    /**
     * Indicates the community the user belongs to.
     */
    private final Object2IntMap<U> userComm;
    /**
     * Indicates the list of users of the communities.
     */
    private final List<List<U>> commUsers;

    /**
     * Constructor.
     */
    public Communities()
    {
        userComm = new Object2IntOpenHashMap<>();
        commUsers = new ArrayList<>();
        userComm.defaultReturnValue(-1);
    }

    /**
     * Obtains the number of communities.
     *
     * @return The number of communities.
     */
    public int getNumCommunities()
    {
        return this.commUsers.size();
    }

    /**
     * Obtains the different communities of the graph.
     *
     * @return an int stream containing the diferent communities of the graph.
     */
    public IntStream getCommunities()
    {
        return IntStream.range(0, this.getNumCommunities());
    }

    /**
     * Gets the community a given user belongs to.
     *
     * @param user The user.
     *
     * @return The community if the user exists, -1 if it does not exist.
     */
    public int getCommunity(U user)
    {
        return userComm.getInt(user);
    }

    /**
     * Gets the users inside a community.
     *
     * @param community The community.
     *
     * @return a stream containing the users in the community if exists, an empty stream if not.
     */
    public Stream<U> getUsers(int community)
    {
        if (community >= 0 && community < this.getNumCommunities())
        {
            return commUsers.get(community).stream();
        }
        else
        {
            return Stream.empty();
        }
    }

    /**
     * Adds a new community to the list.
     */
    public void addCommunity()
    {
        this.commUsers.add(new ArrayList<>());
    }

    /**
     * Adds a pair user/community.
     *
     * @param user The new user. It must not be already in the object.
     * @param comm The associated community. The community has to previously exist.
     *
     * @return true if everything goes OK, false if not.
     */
    public boolean add(U user, int comm)
    {
        if (comm >= 0 && comm < this.getNumCommunities() && !userComm.containsKey(user))
        {
            this.commUsers.get(comm).add(user);
            this.userComm.put(user, comm);
        }
        return false;
    }

    /**
     * Obtains the size of a community.
     *
     * @param community The community whose size we want to obtain.
     *
     * @return the size of the community if it exists, 0 if it does not.
     */
    public int getCommunitySize(int community)
    {
        if (community >= 0 && community < this.getNumCommunities())
        {
            return commUsers.get(community).size();
        }
        else
        {
            return 0;
        }
    }
}
