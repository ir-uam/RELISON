/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.community.detection.modularity;

import es.uam.eps.ir.relison.sna.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Implementation of the label propagation algorithm. This community detection approach starts with all nodes
 * having different communities, and ends when each nodes shares the same label that the majority of his neighbors.
 * <p>
 *     <b>Reference: </b> U.N. Raghavan, R. Albert, S. Kumara. Near linear time algorithm to detect communities in large-scale networks. Physical Review E 76: 036106 (2007).
 * </p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class LabelPropagation<U> implements CommunityDetectionAlgorithm<U>
{
    /**
     * The seed for a random number generator.
     */
    private final int rngSeed;

    /**
     * Constructor.
     */
    public LabelPropagation()
    {
        rngSeed = 0;
    }

    /**
     * Constructor.
     * @param rngSeed a seed for a random number generator.
     */
    public LabelPropagation(int rngSeed)
    {
        this.rngSeed = rngSeed;
    }

    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        Random rng = new Random(rngSeed);
        List<U> nodes = graph.getAllNodes().collect(Collectors.toCollection(ArrayList::new));

        Object2IntMap<U> labels = new Object2IntOpenHashMap<>();
        nodes.forEach(u -> labels.put(u, labels.size()-1));

        boolean changed = true;
        while(changed)
        {
            // We first shuffle the nodes
            Collections.shuffle(nodes);

            // Then:
            for(U node : nodes)
            {
                int currentLabel = labels.getInt(node);
                Int2IntOpenHashMap counter = new Int2IntOpenHashMap();

                graph.getNeighbourNodes(node).forEach(u -> counter.addTo(labels.getInt(u), 1));
                IntList top = new IntArrayList();
                int max = Integer.MIN_VALUE;
                for(int label : counter.keySet())
                {
                    int value = counter.get(label);
                    if(max < value)
                    {
                        top = new IntArrayList();
                        top.add(label);
                        max = value;
                    }
                    else if(max == value)
                    {
                        top.add(label);
                    }
                }

                int newLabel;
                if(top.isEmpty())
                {
                    newLabel = currentLabel;
                }
                else if(top.size() == 1)
                {
                    newLabel = top.getInt(0);
                }
                else
                {
                    newLabel = top.getInt(rng.nextInt(top.size()));
                }

                labels.put(node, newLabel);
            }

            changed = false;
            for(U node : nodes)
            {
                int currentLabel = labels.getInt(node);
                Int2IntOpenHashMap counter = new Int2IntOpenHashMap();

                graph.getNeighbourNodes(node).forEach(u -> counter.addTo(labels.getInt(u), 1));
                IntSet top = new IntOpenHashSet();
                int max = Integer.MIN_VALUE;
                for(int label : counter.keySet())
                {
                    int value = counter.get(label);
                    if(max < value)
                    {
                        top = new IntOpenHashSet();
                        top.add(label);
                        max = value;
                    }
                    else if(max == value)
                    {
                        top.add(label);
                    }
                }

                if(!top.isEmpty() && !top.contains(currentLabel))
                {
                    changed = true;
                    break;
                }
            }
        }

        // Now, the labels have been propagated. So we have to build the communities:
        Int2IntMap help = new Int2IntOpenHashMap();
        Communities<U> comms = new Communities<>();
        labels.forEach((user, label) ->
        {
            if(!help.containsKey(label.intValue()))
            {
                comms.addCommunity();
                help.put(label.intValue(), comms.getNumCommunities()-1);
            }

            int comm = help.get(label.intValue());
            comms.add(user, comm);
        });

        return comms;
    }
}