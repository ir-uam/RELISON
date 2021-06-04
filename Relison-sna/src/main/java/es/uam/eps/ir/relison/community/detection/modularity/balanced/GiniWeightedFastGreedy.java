/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.community.detection.modularity.balanced;

import es.uam.eps.ir.relison.community.Communities;
import es.uam.eps.ir.relison.community.Dendogram;
import es.uam.eps.ir.relison.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.community.detection.DendogramCommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.index.fast.FastIndex;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.doubles.Double2DoubleMap;
import it.unimi.dsi.fastutil.doubles.Double2DoubleOpenHashMap;
import org.jooq.lambda.tuple.Tuple3;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Alternative version of Fast Greedy algorithm for optimizing modularity, taking into account the Gini of the size
 * of communities.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class GiniWeightedFastGreedy<U> implements CommunityDetectionAlgorithm<U>, DendogramCommunityDetectionAlgorithm<U>
{
    /**
     * The weight for the Gini term
     */
    private final double lambda;
    /**
     * The optimal number of communities.
     */
    private int optimalNumComms;

    /**
     * Constructor.
     *
     * @param lambda the weight of the Gini term.
     */
    public GiniWeightedFastGreedy(double lambda)
    {
        this.lambda = lambda;
    }

    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        Dendogram<U> dendogram = this.detectCommunityDendogram(graph);
        return dendogram.getCommunitiesByNumber(this.optimalNumComms);
    }

    @Override
    public Dendogram<U> detectCommunityDendogram(Graph<U> graph)
    {
        double currentQ = 0.0;
        double maxQ = 0.0;
        double lastGini = 0.0;

        // list of triples to build the dendogram.
        List<Tuple3<Integer, Integer, Integer>> triples = new ArrayList<>();

        Communities<U> aux = new Communities<>();
        Map<Integer, Integer> currentClusters = new HashMap<>();
        Map<Integer, Integer> auxClusters;

        FastIndex<U> fastIndex = new FastIndex<>();

        List<Double> sizes = new ArrayList<>();
        Double2DoubleMap freqs = new Double2DoubleOpenHashMap();

        // We initialize each cluster in a separate community.
        List<U> users = graph.getAllNodes().collect(Collectors.toCollection(ArrayList::new));
        for (U u : users)
        {
            aux.addCommunity();
            aux.add(u, aux.getNumCommunities() - 1);
            currentClusters.put(aux.getNumCommunities() - 1, aux.getNumCommunities() - 1);
            fastIndex.addObject(u);
            sizes.add(1.0);
        }

        freqs.put(1.0, users.size() + 0.0);

        CompleteCommunityGraphGenerator<U> commGraphGen = new CompleteCommunityGraphGenerator<>();
        MultiGraph<Integer> commGraph;


        Pair<Integer> maxPair;
        List<Integer> comms;
        int currentJoint = users.size();
        this.optimalNumComms = users.size();

        int numiter = 0;
        long a = System.currentTimeMillis();
        while (aux.getNumCommunities() > 1)
        {
            comms = aux.getCommunities().boxed().collect(Collectors.toCollection(ArrayList::new));
            commGraph = commGraphGen.generate(graph, aux);
            double maxDeltaQ = Double.NEGATIVE_INFINITY;
            double maxNewGini = lastGini;
            maxPair = null;
            int numComm = aux.getNumCommunities();
            Map<Integer, Double> edgesFromComm = new HashMap<>();
            Map<Integer, Double> edgesToComm = new HashMap<>();

            // We find the maximum modularity increment
            if (graph.getEdgeCount() == 0) //In case the graph has no edges, the optimal division is all separate clusters, and there is no increment.
            {
                for (int i = 0; i < comms.size(); ++i)
                {
                    Double2DoubleMap auxFreqs = new Double2DoubleOpenHashMap(freqs);
                    double sizeI = sizes.get(i);
                    auxFreqs.put(sizeI, auxFreqs.getOrDefault(sizeI, 0.0) - 1.0);

                    for (int j = 0; j < comms.size(); ++j)
                    {
                        double sizeJ = sizes.get(j);
                        double sizeIJ = sizeI + sizeJ;
                        auxFreqs.put(sizeJ, auxFreqs.getOrDefault(sizeI, 0.0) - 1.0);
                        auxFreqs.put(sizeIJ, auxFreqs.getOrDefault(sizeJ, 0.0) + 1.0);

                        Set<Entry<Double, Double>> entries = new TreeSet<>(Entry.comparingByKey());

                        entries.addAll(auxFreqs.double2DoubleEntrySet());
                        double max = -1.0;
                        double min;
                        double newGini = 0.0;
                        double sum = 0.0;
                        for (Entry<Double, Double> value : entries)
                        {
                            double key = value.getKey();
                            double v = value.getValue();
                            min = max + 1;
                            max = min + v - 1.0;
                            newGini += ((max + min) * (max - min + 1) - v * (numComm + 1.0)) * key;
                            sum += v * key;
                        }

                        if (sum != 0 && numComm > 2)
                        {
                            newGini /= (sum * (numComm - 2.0));
                        }

                        auxFreqs.put(sizeJ, auxFreqs.getOrDefault(sizeJ, 0.0) + 1.0);
                        auxFreqs.put(sizeIJ, auxFreqs.getOrDefault(sizeIJ, 0.0) - 1.0);
                        if (auxFreqs.get(sizeIJ) == 0.0)
                        {
                            auxFreqs.remove(sizeIJ);
                        }
                        double wd = this.lambda * (newGini - lastGini);
                        double deltaQ = -wd;
                        if (maxDeltaQ < deltaQ)
                        {
                            maxDeltaQ = deltaQ;
                            maxPair = new Pair<>(i, j);
                            maxNewGini = newGini;
                        }


                    }
                }
            }
            else if (graph.isDirected()) //
            {
                /*
                 * In the case of directed graphs, the increment in modularity is computed as:
                 * \Delta Q = e_{ij} + e_{ji} - a_{i}^{out} * a_{j}^{in} - a_{i}^{in} * a_{j}^{out}
                 * where e_{ij} is the fraction of edges in the network that start in comm. i and go to comm. j,
                 * a_{i}^{in} = \sum_{c \in C} e_{ci}, and a_{j}^{out} = \sum_{c \in C} e_{ic}.

                 */
                for (int i = 0; i < comms.size(); ++i)
                {
                    Double2DoubleMap auxFreqs = new Double2DoubleOpenHashMap(freqs);
                    double sizeI = sizes.get(i);
                    auxFreqs.put(sizeI, auxFreqs.getOrDefault(sizeI, 0.0) - 1.0);
                    double edgesFromI = commGraph.getAdjacentEdgesCount(i) / (graph.getEdgeCount() + 0.0);
                    edgesFromComm.put(i, edgesFromI);
                    double edgesToI = commGraph.getIncidentEdgesCount(i) / (graph.getEdgeCount() + 0.0);
                    edgesToComm.put(i, edgesToI);
                    for (int j = 0; j < i; ++j)
                    {
                        double edgesFromJ = edgesFromComm.get(j);
                        double edgesToJ = edgesToComm.get(j);
                        double edgesIJ = 0.0;
                        double edgesJI = 0.0;
                        if (commGraph.containsEdge(i, j))
                        {
                            edgesIJ = commGraph.getEdgeWeights(i, j).size() / (graph.getEdgeCount() + 0.0);
                        }
                        if (commGraph.containsEdge(j, i))
                        {
                            edgesJI = commGraph.getEdgeWeights(j, i).size() / (graph.getEdgeCount() + 0.0);
                        }

                        double sizeJ = sizes.get(j);
                        double sizeIJ = sizeI + sizeJ;
                        auxFreqs.put(sizeJ, auxFreqs.getOrDefault(sizeI, 0.0) - 1.0);
                        auxFreqs.put(sizeIJ, auxFreqs.getOrDefault(sizeJ, 0.0) + 1.0);

                        Set<Entry<Double, Double>> entries = new TreeSet<>(Entry.comparingByKey());

                        entries.addAll(auxFreqs.double2DoubleEntrySet());
                        double max = -1.0;
                        double min;
                        double newGini = 0.0;
                        double sum = 0.0;
                        for (Entry<Double, Double> value : entries)
                        {
                            double key = value.getKey();
                            double v = value.getValue();
                            min = max + 1;
                            max = min + v - 1.0;
                            newGini += ((max + min) * (max - min + 1) - v * (numComm - 1.0)) * key;
                            sum += v * key;
                        }
                        if (sum != 0 && numComm > 2)
                        {
                            newGini /= (sum * (numComm - 2.0));
                        }
                        auxFreqs.put(sizeJ, auxFreqs.getOrDefault(sizeJ, 0.0) + 1.0);
                        auxFreqs.put(sizeIJ, auxFreqs.getOrDefault(sizeIJ, 0.0) - 1.0);
                        if (auxFreqs.get(sizeIJ) == 0.0)
                        {
                            auxFreqs.remove(sizeIJ);
                        }
                        double wd = this.lambda * (newGini - lastGini);
                        double deltaQ = edgesIJ + edgesJI - edgesFromI * edgesToJ - edgesFromJ * edgesToI - wd;
                        if (deltaQ >= maxDeltaQ)
                        {
                            maxDeltaQ = deltaQ;
                            maxPair = new Pair<>(i, j);
                            maxNewGini = newGini;
                        }
                    }
                }
            }
            else
            {
                /*
                 * In the case of undirected graphs, the increment in modularity is computed as:
                 * \Delta Q = 2(e_{ij} - a_{i} * a_{j}
                 * where e_{ij} is the fraction of edges in the network between comm. i and go comm. j,
                 * a_{i} = \sum_{c \in C} e_{ci} = \sum_{c \in C} e_{ic}.
                 */
                for (int i = 0; i < comms.size(); ++i)
                {
                    Double2DoubleMap auxFreqs = new Double2DoubleOpenHashMap(freqs);
                    double sizeI = sizes.get(i);
                    auxFreqs.put(sizeI, auxFreqs.getOrDefault(sizeI, 0.0) - 1.0);
                    double edgesI = commGraph.getNeighbourNodesCount(i);
                    edgesFromComm.put(i, edgesI);
                    for (int j = 0; j < i; ++j)
                    {
                        double edgesJ = edgesFromComm.get(j);
                        double edgesIJ = 0.0;
                        if (commGraph.containsEdge(i, j))
                        {
                            edgesIJ = commGraph.getEdgeWeights(i, j).size() / (graph.getEdgeCount() + 0.0);
                        }

                        double sizeJ = sizes.get(j);
                        double sizeIJ = sizeI + sizeJ;
                        auxFreqs.put(sizeJ, auxFreqs.getOrDefault(sizeI, 0.0) - 1.0);
                        auxFreqs.put(sizeIJ, auxFreqs.getOrDefault(sizeJ, 0.0) + 1.0);

                        Set<Entry<Double, Double>> entries = new TreeSet<>(Entry.comparingByKey());

                        entries.addAll(auxFreqs.double2DoubleEntrySet());
                        double max = -1.0;
                        double min;
                        double newGini = 0.0;
                        double sum = 0.0;
                        for (Entry<Double, Double> value : entries)
                        {
                            double key = value.getKey();
                            double v = value.getValue();
                            min = max + 1;
                            max = min + v - 1.0;
                            newGini += ((max + min) * (max - min + 1) - v * (numComm - 1.0)) * key;
                            sum += v * key;
                        }
                        if (sum != 0 && numComm > 2)
                        {
                            newGini /= (sum * (numComm - 2.0));
                        }
                        auxFreqs.put(sizeJ, auxFreqs.getOrDefault(sizeJ, 0.0) + 1.0);
                        auxFreqs.put(sizeIJ, auxFreqs.getOrDefault(sizeIJ, 0.0) - 1.0);
                        if (auxFreqs.get(sizeIJ) == 0.0)
                        {
                            auxFreqs.remove(sizeIJ);
                        }

                        double wd = this.lambda * (newGini - lastGini);
                        double deltaQ = 2 * edgesIJ + edgesI * edgesJ - wd;
                        if (deltaQ > maxDeltaQ)
                        {
                            maxDeltaQ = deltaQ;
                            maxPair = new Pair<>(i, j);
                            maxNewGini = newGini;
                        }
                    }
                }
            }

            // If there is no error, a new pair of nodes is joined in the dendogram.
            if (maxPair != null && maxPair.v1() != null && maxPair.v2() != null)
            {
                int firstComm = maxPair.v1();
                int secondComm = maxPair.v2();
                int firstCommJoint = currentClusters.get(firstComm);
                int secondCommJoint = currentClusters.get(secondComm);

                double sizeI = sizes.get(firstComm);
                double sizeJ = sizes.get(secondComm);

                freqs.put(sizeI, freqs.getOrDefault(sizeI, 0.0) - 1.0);
                if (freqs.get(sizeI) <= 0.0)
                {
                    freqs.remove(sizeI);
                }
                freqs.put(sizeJ, freqs.getOrDefault(sizeJ, 0.0) - 1.0);
                if (freqs.get(sizeJ) <= 0.0)
                {
                    freqs.remove(sizeJ);
                }

                freqs.put(sizeI + sizeJ, freqs.getOrDefault(sizeI + sizeJ, 0.0) + 1.0);

                lastGini = maxNewGini;
                Tuple3<Integer, Integer, Integer> tuple = new Tuple3<>(firstCommJoint, secondCommJoint, currentJoint);
                triples.add(0, tuple);
                currentQ += maxDeltaQ;

                if (currentQ > maxQ) // If we achieve a maximum value in modularity
                {
                    maxQ = currentQ;
                    this.optimalNumComms = aux.getNumCommunities();
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
}
