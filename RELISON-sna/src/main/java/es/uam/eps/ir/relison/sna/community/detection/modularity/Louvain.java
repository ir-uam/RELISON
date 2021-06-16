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
        Map<Integer, Double> sumIn = new HashMap<>();
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

            // Then, the weight of the links inside the community is just the weight of a node to itself:
            sumIn.put(commIndex, graph.containsEdge(u,u) ? graph.getEdgeWeight(u,u) : 0.0);

            // and the total number of links towards the community is this:
            sumTot.put(commIndex, degreeU);
            return degreeU;
        }).sum();

        Collections.shuffle(users, rng);
        double variation = Double.POSITIVE_INFINITY;

        // Phase 1 of the procedure:
        while(variation >= threshold)
        {
            variation = 0.0;
            for(U u : users)
            {
                Set<Integer> comms = new HashSet<>();
                int actualComm = userToComm.get(u);
                Int2DoubleOpenHashMap degreeToInterior = new Int2DoubleOpenHashMap();
                degreeToInterior.defaultReturnValue(0.0);
                graph.getNeighbourhoodWeights(u, EdgeOrientation.UND).filter(v -> !v.equals(u)).forEach(v ->
                {
                    int comm = userToComm.get(v.getIdx());
                    double val = degreeToInterior.get(comm);
                    degreeToInterior.put(comm, val + v.getValue());
                    comms.add(comm);
                });

                double Qminus = Math.pow((sumTot.get(actualComm) + degrees.get(u))/m, 2.0);
                Qminus -= (sumIn.get(actualComm) + 2*(degreeToInterior.containsKey(actualComm) ? degreeToInterior.get(actualComm) : 0.0))/m;
                Qminus += sumIn.get(actualComm)/m;
                Qminus -= (Math.pow(sumTot.get(actualComm)/m, 2.0));

                double increment = 0.0;
                int nextComm = actualComm;

                for(int comm : comms)
                {
                    if(comm == actualComm)
                    {
                        continue;
                    }

                    double Qsum = -Math.pow((sumTot.get(comm) + degrees.get(u))/m, 2.0);
                    Qsum += (sumIn.get(comm) + 2*(degreeToInterior.containsKey(comm) ? degreeToInterior.get(comm) : 0.0))/m;
                    Qsum -= sumIn.get(comm)/m;
                    Qsum += (Math.pow(sumTot.get(comm)/m, 2.0));

                    double score = Qsum - Qminus;
                    if(score > increment)
                    {
                        nextComm = comm;
                        increment = score;
                    }
                    else if(score == increment && increment > 0.0)
                    {
                        if(rng.nextBoolean())
                        {
                            nextComm = comm;
                        }
                    }
                }

                // Swap communities:
                if(nextComm != actualComm)
                {
                    // We first update the number of edges inside nextComm and actualComm
                    sumIn.put(nextComm, sumIn.get(nextComm) + degreeToInterior.get(nextComm) + graph.getEdgeWeight(u,u));
                    sumIn.put(actualComm, sumIn.get(actualComm) - degreeToInterior.get(actualComm) - graph.getEdgeWeight(u,u));
                    // We then update the sum of the weights of the edges of the community.
                    sumTot.put(nextComm, sumTot.get(nextComm) + degrees.get(u));
                    sumTot.put(actualComm, sumTot.get(actualComm) - degrees.get(u));

                    userToComm.put(u, nextComm);
                    commToUser.get(actualComm).remove(u);
                    commToUser.get(nextComm).add(u);
                    variation += increment;
                }
            }
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