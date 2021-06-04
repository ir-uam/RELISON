/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.community.detection.modularity;

import es.uam.eps.ir.relison.community.Communities;
import es.uam.eps.ir.relison.community.Dendogram;
import es.uam.eps.ir.relison.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.community.detection.DendogramCommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.index.fast.FastIndex;
import org.jooq.lambda.tuple.Tuple3;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract class for the implementation of Fast Greedy algorithm versions for optimizing modularity.
 *
 * <p>
 * <b>Reference:</b> M.E.J. Newman. Fast Algorithm for detecting community structure in networks. Physical Review E 69(6): 066133 (2004)
 * </p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class AbstractFastGreedy<U> implements CommunityDetectionAlgorithm<U>, DendogramCommunityDetectionAlgorithm<U>
{
    /**
     * The optimal number of communities.
     */
    private int optimalNumComms;
    /**
     * Random number generator.
     */
    protected Random rng;
    /**
     * Random number seed.
     */
    private final int rngSeed;

    /**
     * Constructor.
     */
    public AbstractFastGreedy()
    {
        rngSeed = 0;
        rng = new Random(rngSeed);
    }

    /**
     * Constructor.
     * @param rngSeed the random seed.
     */
    public AbstractFastGreedy(int rngSeed)
    {
        this.rngSeed = rngSeed;
        rng = new Random(rngSeed);
    }

    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        this.rng = new Random(rngSeed);
        Dendogram<U> dendogram = this.detectCommunityDendogram(graph);
        return dendogram.getCommunitiesByNumber(this.optimalNumComms);
    }

    @Override
    public Dendogram<U> detectCommunityDendogram(Graph<U> graph)
    {
        this.rng = new Random(rngSeed);

        double currentQ = 0;
        double maxQ = 0;

        // list of triples to build the dendogram.
        List<Tuple3<Integer, Integer, Integer>> triples = new ArrayList<>();

        Communities<U> aux = new Communities<>();
        Map<Integer, Integer> currentClusters = new HashMap<>();
        Map<Integer, Integer> auxClusters;

        FastIndex<U> fastIndex = new FastIndex<>();

        // We initialize each cluster in a separate community.
        List<U> users = graph.getAllNodes().collect(Collectors.toCollection(ArrayList::new));
        for (U u : users)
        {
            aux.addCommunity();
            aux.add(u, aux.getNumCommunities() - 1);
            currentClusters.put(aux.getNumCommunities() - 1, aux.getNumCommunities() - 1);
            fastIndex.addObject(u);
        }

        int currentJoint = users.size();
        this.optimalNumComms = users.size();
        long a = System.currentTimeMillis();
        int numiter = 0;
        // Execute the community detection algorithm
        while (aux.getNumCommunities() > 1)
        {
            // Find the two optimal communities to merge.
            Tuple3<Integer, Integer, Double> triple = this.findOptimalJoint(graph, aux);

            // If there is no error, a new pair of nodes is joined in the dendogram.
            if (triple != null && triple.v1() != null && triple.v2() != null)
            {
                int firstComm = triple.v1();
                int secondComm = triple.v2();
                int firstCommJoint = currentClusters.get(firstComm);
                int secondCommJoint = currentClusters.get(secondComm);

                Tuple3<Integer, Integer, Integer> tuple = new Tuple3<>(firstCommJoint, secondCommJoint, currentJoint);
                triples.add(0, tuple);
                currentQ += triple.v3();

                if (currentQ > maxQ) // If we achieve a maximum value in modularity
                {
                    maxQ = currentQ;
                    this.optimalNumComms = aux.getNumCommunities() - 1;
                }

                Communities<U> aux2 = new Communities<>();
                auxClusters = new HashMap<>();

                int minComm = Math.min(firstComm, secondComm);
                int maxComm = Math.max(firstComm, secondComm);

                // Update the clusters
                for (int i = 0; i < aux.getNumCommunities(); ++i)
                {
                    int j = (i > maxComm) ? i - 1 : i;
                    if (i == minComm)
                    {
                        aux2.addCommunity();
                        aux.getUsers(minComm).forEach(u -> aux2.add(u, j));
                        aux.getUsers(maxComm).forEach(u -> aux2.add(u, j));
                        auxClusters.put(j, currentJoint);
                    }
                    else if (i != maxComm)
                    {
                        aux2.addCommunity();
                        aux.getUsers(i).forEach(u -> aux2.add(u, j));
                        auxClusters.put(j, currentClusters.get(i));
                    }
                }

                aux = aux2;
                currentClusters = auxClusters;
                currentJoint++;

                if (numiter % 100 == 0)
                {
                    long b = System.currentTimeMillis();
                    System.out.println("Iteration " + numiter + " finished " + (b - a) + " ms.");
                    a = b;
                }
                numiter++;
            }
        }

        return new Dendogram<>(fastIndex, graph, triples.stream());
    }

    /**
     * Finds the optimal pair of communities to merge.
     *
     * @param graph the original graph.
     * @param comm  the communities.
     *
     * @return A triple containing: a) the first comm. to merge, b) the second one, c) the mod. increment.
     */
    protected abstract Tuple3<Integer, Integer, Double> findOptimalJoint(Graph<U> graph, Communities<U> comm);
}
