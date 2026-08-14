/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.community.detection.modularity;

import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.sna.community.graph.SimpleCommunityGraphGenerator;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.UndirectedGraph;
import es.uam.eps.ir.relison.graph.Weight;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedWeightedGraph;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.relison.sna.metrics.communities.graph.Modularity;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.io.Serializable;
import java.util.*;


/**
 * Class for computing the Louvain community detection algorithm.
 * <p>
 * <b>Reference:</b>  V. Blondel, J. Guillaume, R. Lambiotte, E. Lefebvre, Fast unfolding of communities in large networks. Journal of Statistical Mechanics 10 (2008)
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Louvain<U extends Serializable> implements CommunityDetectionAlgorithm<U>
{
    /**
     * Seed for a random number generator.
     */
    private final int rngSeed;
    /**
     * The minimum variation of modularity for another phase 1 round.
     */
    private final double threshold;

    /**
     * Constructor.
     * @param rngSeed random number generator seed.
     * @param threshold the minimum variation for another round in phase 1.
     */
    public Louvain(int rngSeed, double threshold)
    {
        this.rngSeed = rngSeed;
        this.threshold = threshold;
    }

    /**
     * Constructor.
     * @param threshold the minimum variation for another round in phase 1.
     */
    public Louvain(double threshold)
    {
        this.rngSeed = 0;
        this.threshold = threshold;
    }

    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        Random rng = new Random(rngSeed);
        // Step 1: We transform the graph into an undirected graph (unless it is already undirected)

        List<U> users = new ArrayList<>();
        Map<U,Double> degrees = new Object2DoubleOpenHashMap<>();
        Map<U, Integer> userToComm = new HashMap<>();
        Map<Integer, Set<U>> commToUser = new HashMap<>();
        // sumTot[c] = total degree of the nodes currently in community c. Together with the weight of the edges from a
        // node to a community, it is all the modularity-gain of a local move depends on (the internal-edge sums the
        // classic derivation also carries cancel out of the gain, so they are not tracked here).
        Map<Integer, Double> sumTot = new HashMap<>();

        // Step 2: Community assignments:
        double m = graph.getAllNodes().mapToDouble(u ->
        {
            users.add(u);
            // Assign the node each own community
            int commIndex = userToComm.size();
            userToComm.put(u, commIndex);
            commToUser.put(commIndex, new HashSet<>());
            commToUser.get(commIndex).add(u);

            // We do obtain the degree of the user:
            double degreeU = graph.getNeighbourhoodWeights(u, EdgeOrientation.UND).mapToDouble(Weight::getValue).sum();
            degrees.put(u, degreeU);

            // Each node starts in its own community, whose total degree is just the node's degree.
            sumTot.put(commIndex, degreeU);
            return degreeU;
        }).sum();

        Collections.shuffle(users, rng);

        // Phase 1: local moving. We repeatedly sweep over the nodes; for each node u we (conceptually) remove it from
        // its community and reinsert it into the neighbouring community with the largest modularity gain — its own
        // community included, so "do not move" is always an option. Isolating u first and then scoring EVERY candidate
        // (the original community and the neighbouring ones) with the SAME insertion formula is what makes the gain
        // correct and symmetric: computing the "leave" cost on the community while u is still counted in it double-counts
        // u and yields a gain that is not the true modularity change, which makes the moves oscillate and never converge.
        int maxSweeps = 500;   // safety cap: local moving always converges, but never let a numeric edge case hang the loop.
        double m2 = m * m;
        boolean changed = true;
        for (int sweep = 0; changed && sweep < maxSweeps; ++sweep)
        {
            // Cooperative cancellation: when run on a worker thread that the GUI's Stop button interrupts, abandon the
            // (potentially long) local-moving loop instead of running it to convergence. Harmless outside that context:
            // an ordinary caller never interrupts the computing thread, so the flag is never set.
            if (Thread.currentThread().isInterrupted())
                throw new java.util.concurrent.CancellationException("Louvain community detection cancelled.");
            changed = false;
            double variation = 0.0;
            for (U u : users)
            {
                int actualComm = userToComm.get(u);
                double degU = degrees.get(u);

                // Weight of the edges from u to each neighbouring community (self-loops excluded: a self-loop stays
                // with u whatever community it joins, so it is constant across candidates and irrelevant to the choice).
                Int2DoubleOpenHashMap degreeToComm = new Int2DoubleOpenHashMap();
                degreeToComm.defaultReturnValue(0.0);
                Set<Integer> comms = new HashSet<>();
                // Exclude the self-loop: compare the neighbour node (v.getIdx()), not the Weight object, against u —
                // Weight does not override equals, so "v.equals(u)" is always false and would leak the self-loop into
                // the edges-to-own-community count. This only matters when self-loops exist (notably the condensed
                // graphs built during the phase-2 recursion), where a self-loop must stay neutral to the move choice.
                graph.getNeighbourhoodWeights(u, EdgeOrientation.UND).filter(v -> !v.getIdx().equals(u)).forEach(v ->
                {
                    int comm = userToComm.get(v.getIdx());
                    degreeToComm.addTo(comm, v.getValue());
                    comms.add(comm);
                });

                // Isolate u from its current community so that community's stats no longer include it.
                sumTot.put(actualComm, sumTot.get(actualComm) - degU);
                commToUser.get(actualComm).remove(u);

                // Community-dependent part of the insertion gain: 2*k(u,C)/m - 2*deg(u)*sumTot[C]/m². The remaining
                // terms of the gain (u's self-loop, deg(u)²) are the same for every candidate and drop out of the argmax.
                double baseGain = 2.0 * degreeToComm.get(actualComm) / m - 2.0 * degU * sumTot.get(actualComm) / m2;
                int bestComm = actualComm;
                double bestGain = baseGain;
                for (int comm : comms)
                {
                    if (comm == actualComm) continue;
                    double gain = 2.0 * degreeToComm.get(comm) / m - 2.0 * degU * sumTot.get(comm) / m2;
                    if (gain > bestGain)
                    {
                        bestGain = gain;
                        bestComm = comm;
                    }
                }

                // Reinsert u into the best community found (its own if nothing was strictly better).
                sumTot.put(bestComm, sumTot.get(bestComm) + degU);
                commToUser.get(bestComm).add(u);
                userToComm.put(u, bestComm);

                if (bestComm != actualComm)
                {
                    changed = true;
                    variation += bestGain - baseGain;   // the true modularity increase produced by this move.
                }
            }
            if (variation < threshold) break;   // this sweep improved modularity by less than the threshold: converged.
        }

        Communities<U> initComms = new Communities<>();
        // Phase 2: Build the community graph
        commToUser.values().stream().filter(set -> !set.isEmpty()).forEach(comm ->
        {
            initComms.addCommunity();
            int c = initComms.getNumCommunities()-1;
            for(U user : comm) initComms.add(user, c);
        });

        SimpleCommunityGraphGenerator<Integer> cgraphgen = new SimpleCommunityGraphGenerator<>();
        cgraphgen.configure(graph, initComms, false);

        try
        {
            Graph<Integer> condensed = cgraphgen.generate();
            Louvain<Integer> louvain = new Louvain<>(rngSeed, threshold);

            if(initComms.getNumCommunities() == graph.getVertexCount())
            {
                return initComms;
            }

            Communities<Integer> comms = louvain.detectCommunities(condensed);

            if(comms.getNumCommunities() == initComms.getNumCommunities())
            {
                return initComms;
            }
            else
            {
                Communities<U> defComms = new Communities<>();
                for(int i = 0; i < comms.getNumCommunities(); ++i)
                {
                    defComms.addCommunity();
                    int finalI = i;
                    comms.getUsers(i).forEach(auxComm -> initComms.getUsers(auxComm).forEach(u -> defComms.add(u, finalI)));
                }

                Modularity<U> modularity = new Modularity<>();
                double actual = modularity.compute(graph, initComms);
                double commmod = modularity.compute(graph, defComms);

                if(actual > commmod) return initComms;
                else return defComms;
            }


        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException e)
        {
            return null;
        }
    }
}