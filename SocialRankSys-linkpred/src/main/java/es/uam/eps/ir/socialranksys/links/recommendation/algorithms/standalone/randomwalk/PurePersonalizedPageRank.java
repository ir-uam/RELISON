/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.randomwalk;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recommender algorithm based in a modified Personalized PageRank.
 * @author Sofía Marina Pepa
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class PurePersonalizedPageRank<U> extends UserFastRankingRecommender<U>
{
    /**
     * In case it is true, the target node (or root/origin node) can only be accessed
     * via teleport by a random walker. In other case, with some probability, the node
     * can be accessed by its incoming neighbors.
     */
    private final boolean simple;
    /**
     * Indicates if teleports always go to the origin node. In case it is false,
     * when the random walker arrives to a sink, it might teleport to the any of the
     * other nodes.
     */
    private final boolean S2U;
    /**
     * The teleport rate
     */
    private final double lambda;
    /**
     * Convergence threshold.
     */
    private final static double THRESHOLD = 0.001;
    /**
     * Maximum number of iterations.
     */
    private final static int MAXITER = 50;

    /**
     * Constructor
     * @param graph the original graph.
     * @param lambda the teleport rate.
     * @param simple indicates if the target node can only be accessed via teleport or not.
     * @param S2U Indicates if teleports always go to the origin node.
     */
    public PurePersonalizedPageRank(FastGraph<U> graph, double lambda, boolean simple, boolean S2U) {
        super(graph);
        this.lambda = lambda;
        this.simple = simple;
        this.S2U = S2U;
    }
    
    
    @Override
    public Int2DoubleMap getScoresMap(int i) 
    {
        U u = this.uIndex.uidx2user(i);
        Int2DoubleMap scores = new Int2DoubleOpenHashMap();
        Map<U, Double> pageRanks = calculatePagerank(u);
        pageRanks.forEach((key, value) -> scores.put(uIndex.user2uidx(key), value.doubleValue()));
        
        return scores;
    }
    
    
    
    
    /**
     * Calculates the PageRank value
     * @param uId Root node.
     * @return The PageRank value for all the nodes.
     */
    protected Map<U, Double> calculatePagerank(U uId) {
        if (uId == null) {
            return null;
        }

        Set<U> users = uIndex.getAllUsers().collect(Collectors.toCollection(HashSet::new));
        Map<U, Integer> ids = new HashMap<>();

        int N = users.size();

        // data
        Map<Integer, Set<U>> in = new HashMap<>();
        Integer[] out = new Integer[N];
        Double[] pr = new Double[N];
        Double[] prAux = new Double[N];

        // ini
        int index = 0;
        int u = -1;
        Set<Integer> sinks = new HashSet<>();
        for (U user : users) {
            if (user.equals(uId)) {
                u = index;
            }

            ids.put(user, index);

            Set<U> inUsers = this.getGraph().getIncidentNodes(user).collect(Collectors.toCollection(HashSet::new));
            in.put(index, inUsers);

            Set<U> outUsers = this.getGraph().getAdjacentNodes(user).collect(Collectors.toCollection(HashSet::new));
            out[index] = outUsers.size();

            if (simple && outUsers.contains(uId)) {
                out[index]--;
            }
            if (out[index] == 0) {
                sinks.add(index);
            }

            pr[index] = (1 - lambda) / (N - 1);

            index++;
        }
        if (u < 0) {
            return null;
        }
        pr[u] = lambda;


        // iterations
        boolean hasConverged = false;
        for (int i = 0; i < MAXITER && !hasConverged; i++) {

            // sum sinks
            double sum = 0;
            for (int s : sinks) {
                sum += pr[s];
            }

            // manage the sink's teleportation
            if (S2U) {
                for (int w = 0; w < N; w++) {
                    prAux[w] = 0.0;
                }
                prAux[u] = lambda + (1 - lambda) * sum;
            } else {
                for (int w = 0; w < N; w++) {
                    prAux[w] = (1 - lambda) * sum / (N - (simple ? 1 : 0));
                }
                prAux[u] = lambda + (!simple ? (1 - lambda) * sum / N : 0);
            }

            // calculate
            for (int v : in.keySet()) {
                if (simple && v == u) {
                    continue;
                }
                for (U wId : in.get(v)) {
                    int w = ids.get(wId);
                    prAux[v] += (v == u ? lambda : 1) * (1 - lambda) * (pr[w] / (out[w] + (!simple && in.get(u).contains(wId) ? lambda - 1 : 0)));
                }
            }

            // make sum
            sum = 0.0;
            for (int w = 0; w < N; w++) {
                sum += prAux[w];
            }

//            System.out.println("sum = " + sum + ", iteration " + i + " for user " + uId + " and lambda = " + lambda);

            // check convergency
            hasConverged = true;
            for (int z = 0; z < N; z++) {
                hasConverged = hasConverged && (Math.abs(prAux[z] - pr[z]) < THRESHOLD);
                pr[z] = prAux[z];
            }
        }

        Map<U, Double> pagerank = new HashMap<>();
        for (U w : users) {
            pagerank.put(w, pr[ids.get(w)]);
        }

        return pagerank;
    }



   
}
