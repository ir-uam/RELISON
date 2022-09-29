/*
 *  Copyright (C) 2022-2022 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es / Terrier Team at University of Glasgow,
 *  http://http://terrierteam.dcs.gla.ac.uk
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.relison.sna.edgegroups.simple;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

/**
 * Simple edge partition based on pairs of communities.
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzadopuig@glasgow.ac.uk)
 */
public class InterCommunityEdgePartition<U> extends SimpleEdgePartition<U>
{
    /**
     * Total number of communities.
     */
    private final int numComms;
    /**
     * Special index for links between communities.
     */
    public static final int INTRACOMMLINKS = -2;

    /**
     * Constructor.
     * @param directed true if the edges are directed, false otherwise.
     * @param numComms the total number of communities.
     */
    public InterCommunityEdgePartition(boolean directed, int numComms)
    {
        super(directed);
        this.numComms = numComms;

        if(directed)
        {
            super.addGroup(INTRACOMMLINKS);
            for(int i = 0; i < numComms*numComms; ++i)
            {
                if(!(Math.floor(Math.sqrt(i)) == Math.sqrt(i)))
                    super.addGroup(i);
            }
        }
        else
        {
            super.addGroup(INTRACOMMLINKS);
            int currentComm = 0;
            int currentCommStart = 0;
            for(int i = 0; i < numComms*(numComms+1)/2; ++i) {
                if (i != currentCommStart)
                    super.addGroup(i);
                else
                {
                    ++currentComm;
                    currentCommStart = (currentComm*(2*numComms+1) - currentComm*currentComm)/2;
                }
            }
        }
    }

    /**
     * Given an edge partition on different community pairs, and a community pair,
     * returns the partition number for the edges travelling between them.
     * @param comm1 the first community identifier.
     * @param comm2 the second community identifier.
     * @return the slot number for the pair of communities, -1 if it does not exist.
     */
    public int getSlot(int comm1, int comm2)
    {
        if(comm1 < 0 || comm1 >= numComms || comm2 < 0 || comm2 >= numComms)
            return -1;

        if(comm1 == comm2)
            return INTRACOMMLINKS;

        if (this.isDirected())
        {
            return comm1*numComms + comm2;
        }
        else
        {
            return (comm1*(2*numComms - comm1*comm1))/2 + comm2;
        }
    }

    /**
     * Given an edge partition on different community pairs, and a partition number, returns
     * the origin and destination communities.
     * @param slot the partition number.
     * @return a pair containing the two communities if the slot exists, null otherwise. In the special case that
     * we target the slot -2 (intra-community links) we also return null.
     */
    public Pair<Integer> getCommunityNumbers(int slot)
    {
        if(slot < 0 || slot >= this.getNumGroups())
            return null;

        if(this.isDirected())
        {
            int comm1 = slot/numComms;
            int comm2 = slot%numComms;
            return new Pair<>(comm1, comm2);
        }
        else
        {
            int mid = numComms/2;
            int start = 0;
            int end = numComms - 1;

            int comm1 = -1;

            while (mid >= start && mid <= end && comm1 == -1)
            {
                int a = (mid*(2*numComms-1) - mid*mid)/2;
                int b = ((mid+1)*(2*numComms-1) - (mid+1)*(mid+1))/2;
                if(a <= slot && b > slot)
                {
                    comm1 = mid;
                }
                else if(a > slot)
                {
                    start = mid+1;
                    mid = (end - start)/2;
                }
                else
                {
                    end = mid-1;
                    start = (end-start)/2;
                }
            }

            int comm2 = slot - (comm1*(2*numComms-1) - comm1*comm1)/2;
            return new Pair<>(comm1, comm2);
        }
    }

    @Override
    public boolean addGroup(int id)
    {
        return false;
    }
}
