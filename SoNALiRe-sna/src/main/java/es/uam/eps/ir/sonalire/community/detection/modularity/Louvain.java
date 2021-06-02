/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.community.detection.modularity;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.sonalire.community.graph.SimpleCommunityGraphGenerator;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.UndirectedGraph;
import es.uam.eps.ir.sonalire.graph.Weight;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastUndirectedWeightedGraph;
import es.uam.eps.ir.sonalire.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.sonalire.graph.generator.exception.GeneratorNotConfiguredException;
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
        UndirectedGraph<U> auxGraph;
        if(graph.isDirected())
        {
            auxGraph = new FastUndirectedWeightedGraph<>();
            // To undirected graph
            graph.getAllNodes().forEach(auxGraph::addNode);
            Set<U> visited = new HashSet<>();
            graph.getAllNodes().forEach(node ->
            {
                visited.add(node);
                graph.getNeighbourhoodWeights(node, EdgeOrientation.UND).filter(neigh -> !visited.contains(neigh.getIdx())).forEach(neigh -> auxGraph.addEdge(node, neigh.getIdx(), neigh.getValue()));
            });
        }
        else
        {
            auxGraph = (UndirectedGraph<U>) graph;
        }

        List<U> users = new ArrayList<>();
        Map<U,Double> degrees = new Object2DoubleOpenHashMap<>();
        Map<U, Integer> userToComm = new HashMap<>();
        Map<Integer, Set<U>> commToUser = new HashMap<>();
        Map<Integer, Double> sumIn = new HashMap<>();
        Map<Integer, Double> sumTot = new HashMap<>();
        // Step 2: Community assignments:
        double m = auxGraph.getAllNodes().mapToDouble(u ->
        {
            users.add(u);
            int commIndex = userToComm.size();
            userToComm.put(u, commIndex);
            commToUser.put(commIndex, new HashSet<>());
            commToUser.get(commIndex).add(u);
            double degreeU = auxGraph.getNeighbourhoodWeights(u, EdgeOrientation.UND).mapToDouble(Weight::getValue).sum();
            degrees.put(u, degreeU);
            sumIn.put(commIndex, 0.0);
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
                    degreeToInterior.addTo(comm, v.getValue());
                    comms.add(comm);
                });

                double Qminus = Math.pow((sumTot.get(actualComm) + degrees.get(u))/m, 2.0);
                Qminus -= (sumIn.get(actualComm) + 2*degreeToInterior.getOrDefault(actualComm, 0.0))/m;
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
                    Qsum += (sumIn.get(comm) + 2*degreeToInterior.getOrDefault(comm, 0.0))/m;
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
                    sumIn.put(nextComm, sumIn.get(nextComm) + degreeToInterior.get(nextComm));
                    sumTot.put(nextComm, sumTot.get(nextComm) + degrees.get(u));
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
        cgraphgen.configure(auxGraph, initComms, false);

        try
        {
            Graph<Integer> condensed = cgraphgen.generate();
            Louvain<Integer> louvain = new Louvain<>(rngSeed, threshold);
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
                return defComms;
            }


        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException e)
        {
            return null;
        }
    }
}