/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.community.detection.modularity.balanced;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.relison.community.Communities;
import es.uam.eps.ir.relison.community.Dendogram;
import es.uam.eps.ir.relison.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.community.detection.DendogramCommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.index.fast.FastIndex;
import it.unimi.dsi.fastutil.doubles.Double2DoubleMap;
import it.unimi.dsi.fastutil.doubles.Double2DoubleOpenHashMap;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;

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
public class AlternativeGiniFastGreedy<U> implements CommunityDetectionAlgorithm<U>, DendogramCommunityDetectionAlgorithm<U>
{
    /**
     * Balance between modularity and the Gini of the size of communities
     */
    private final double lambda;
    /**
     * The optimal number of communities.
     */
    private int optimalNumComms;

    /**
     * Constructor.
     *
     * @param lambda balance between modularity and the Gini of the size of communities
     */
    public AlternativeGiniFastGreedy(double lambda)
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

        int currentJoint = users.size();
        this.optimalNumComms = users.size();

        // While the dendogram is not finished
        while (aux.getNumCommunities() > 1)
        {
            // Find the optimal pair of communities to join.
            Tuple4<Integer, Integer, Double, Double> maxTuple = this.findOptimalJoint(graph, aux, freqs, sizes, lastGini);

            // Update the dendogram.
            if (maxTuple != null && maxTuple.v1() != null && maxTuple.v2() != null)
            {
                // Get the communities
                int firstComm = maxTuple.v1();
                int secondComm = maxTuple.v2();

                // Get the nodes to join
                int firstCommJoint = currentClusters.get(firstComm);
                int secondCommJoint = currentClusters.get(secondComm);

                // Get the sizes of the communities
                double sizeI = sizes.get(firstComm);
                double sizeJ = sizes.get(secondComm);
                double sizeIJ = sizeI + sizeJ;

                // Update the size counter
                freqs.put(sizeI, freqs.getOrDefault(sizeI, 0.0) - 1.0);
                freqs.put(sizeJ, freqs.getOrDefault(sizeJ, 0.0) - 1.0);
                freqs.put(sizeIJ, freqs.getOrDefault(sizeIJ, 0.0) + 1.0);

                if (sizeI == sizeJ && freqs.get(sizeI) == 0)
                {
                    freqs.remove(sizeI);
                }
                else if (sizeI != sizeJ)
                {
                    if (freqs.get(sizeI) == 0)
                    {
                        freqs.remove(sizeI);
                    }
                    if (freqs.get(sizeJ) == 0)
                    {
                        freqs.remove(sizeJ);
                    }
                }

                // Update both modularity and the Gini coefficient of the distribution
                currentQ += maxTuple.v3();
                lastGini += maxTuple.v4();

                Tuple3<Integer, Integer, Integer> tuple = new Tuple3<>(firstCommJoint, secondCommJoint, currentJoint);
                triples.add(0, tuple);

                if (currentQ > maxQ)
                {
                    maxQ = currentQ;
                    this.optimalNumComms = aux.getNumCommunities() - 1;
                }

                Communities<U> aux2 = new Communities<>();
                auxClusters = new HashMap<>();

                int minComm = Math.min(firstComm, secondComm);
                int maxComm = Math.max(firstComm, secondComm);

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
                    else
                    {
                        aux2.addCommunity();
                        aux.getUsers(i).forEach(u -> aux2.add(u, j));
                        auxClusters.put(j, currentClusters.get(i));
                    }
                }

                aux = aux2;
                currentClusters = auxClusters;
                currentJoint++;
            }

        }

        return new Dendogram<>(fastIndex, graph, triples.stream());
    }

    /**
     * Finds the optimal two communities to join.
     *
     * @param graph   The graph
     * @param comm    The current community division
     * @param freqs   map containing the sizes of communities as keys, and the number of communities with that size as value (thus allowing fast Gini computing)
     * @param sizes   community sizes.
     * @param oldGini Gini for the previous iteration.
     *
     * @return A 4-tuple containing: the id for the first comm. to join, the identifier for the second,
     *         the modularity increment and the Gini increment.
     */
    private Tuple4<Integer, Integer, Double, Double> findOptimalJoint(Graph<U> graph, Communities<U> comm, Double2DoubleMap freqs, List<Double> sizes, double oldGini)
    {
        Stats modStats = new Stats();
        Stats giniStats = new Stats();
        int numComm = comm.getNumCommunities();

        CompleteCommunityGraphGenerator<U> commGraphGen = new CompleteCommunityGraphGenerator<>();
        MultiGraph<Integer> commGraph = commGraphGen.generate(graph, comm);


        DoubleMatrix2D modIncr = new SparseDoubleMatrix2D(comm.getNumCommunities(), comm.getNumCommunities());
        DoubleMatrix2D giniIncr = new SparseDoubleMatrix2D(comm.getNumCommunities(), comm.getNumCommunities());

        Map<Integer, Double> edgesFromComm = new HashMap<>();
        Map<Integer, Double> edgesToComm = new HashMap<>();


        for (int i = 0; i < comm.getNumCommunities(); ++i)
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
                double sizeJ = sizes.get(j);
                double sizeIJ = sizeI + sizeJ;
                auxFreqs.put(sizeJ, auxFreqs.getOrDefault(sizeJ, 0.0) - 1.0);
                auxFreqs.put(sizeIJ, auxFreqs.getOrDefault(sizeIJ, 0.0) + 1.0);

                double edgesFromJ = edgesFromComm.get(j);
                double edgesToJ = edgesToComm.get(j);
                double edgesIJ = 0.0;
                double edgesJI = 0.0;

                // Find deltaQ
                if (commGraph.containsEdge(i, j))
                {
                    edgesIJ = (commGraph.getEdgeWeights(i, j).size() + 0.0) / (graph.getEdgeCount() + 0.0);
                }
                if (commGraph.containsEdge(j, i))
                {
                    edgesJI = (commGraph.getEdgeWeights(j, i).size() + 0.0) / (graph.getEdgeCount() + 0.0);
                }

                double deltaQ = edgesIJ + edgesJI - edgesFromI * edgesToJ - edgesFromJ * edgesToI;

                modStats.accept(deltaQ);
                modIncr.setQuick(i, j, deltaQ);

                // Find deltaG
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
                    newGini += ((max + min) * (max - min + 1) - v * (numComm + 1.0)) * key;
                    sum += v * key;
                }

                if (sum != 0 && numComm > 2)
                {
                    newGini /= (sum * (numComm - 2.0));
                }

                auxFreqs.put(sizeJ, auxFreqs.getOrDefault(sizeJ, 0.0) + 1.0);
                auxFreqs.put(sizeIJ, auxFreqs.getOrDefault(sizeIJ, 0.0) + 1.0);
                if (auxFreqs.get(sizeIJ) == 0)
                {
                    auxFreqs.remove(sizeIJ);
                }

                giniStats.accept(newGini - oldGini);
                giniIncr.setQuick(i, j, deltaQ);
            }
        }

        double maxScore = Double.NEGATIVE_INFINITY;
        Random rng = new Random();
        int first = rng.nextInt(numComm);
        int second = first;
        while (second == first)
        {
            second = rng.nextInt(numComm);
        }

        Tuple4<Integer, Integer, Double, Double> maxTuple = new Tuple4<>(first, second, modIncr.getQuick(first, second), giniIncr.getQuick(first, second));
        for (int i = 0; i < numComm; ++i)
        {
            for (int j = 0; j < i; ++j)
            {
                double normMod = this.norm(modIncr.getQuick(i, j), modStats);
                double normGini = this.norm(giniIncr.getQuick(i, j), giniStats);
                double score = lambda * normGini + (1 - lambda) * normMod;
                if (score > maxScore)
                {
                    maxTuple = new Tuple4<>(i, j, modIncr.getQuick(i, j), giniIncr.getQuick(i, j));
                }
            }
        }
        return maxTuple;

    }

    /**
     * Normalizes scores, following a Min-Max convention.
     *
     * @param value The original value
     * @param stats The statistics of the distribution
     *
     * @return the normalized value.
     */
    private double norm(double value, Stats stats)
    {
        if (stats.getMin() == stats.getMax())
        {
            return 0.0;
        }
        return (value - stats.getMin()) / (stats.getMax() - stats.getMin());
    }
}
