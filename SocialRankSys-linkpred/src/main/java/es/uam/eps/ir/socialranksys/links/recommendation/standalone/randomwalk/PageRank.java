/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.standalone.randomwalk;


import es.uam.eps.ir.socialranksys.graph.Graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes the PageRank values of the different nodes in the grah.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the nodes.
 */
public class PageRank<U>
{
    /**
     * Maximum number of iterations
     */
    private final int MAXITER = 50;
    /**
     * Threshold
     */
    private final double THRESHOLD = 0.00001;
    /**
     * Teleport parameter.
     */
    private final double r;
    /**
     * Original node (if PageRank is personalized)
     */
    private final U u;
    
    /**
     * Constructor (for not personalized PageRank).
     * @param r the teleport parameter.
     */
    public PageRank(double r)
    {
        this.r = r;
        this.u = null;
    }
    
    /**
     * Constructor (for personalized PageRank)
     * @param r the teleport parameter.
     * @param u the original user.
     */
    public PageRank(double r, U u)
    {
        this.r = r;
        this.u = u;
    }

    public double compute(Graph<U> graph, U user) {
        return this.compute(graph).get(user);
    }

    public Map<U, Double> compute(Graph<U> graph)
    {
        Set<U> users = graph.getAllNodes().collect(Collectors.toCollection(HashSet::new));
        Map<U, Integer> ids = new HashMap<>();
        
        int N = users.size();

        // data
        Map<Integer, Map<U, Double>> in = new HashMap<>();
        double[] out = new double[N];
        double[] pr = new double[N];
        double[] prAux = new double[N];

        // ini
        int index = 0;
        for (U v : users) 
        {
            ids.put(v, index);

            Map<U, Double> inWeights;
            double outSum;

            Set<U> inUsers = graph.getIncidentNodes(v).collect(Collectors.toCollection(HashSet::new));
            inWeights = new HashMap<>();
            for (U w : inUsers) {
                inWeights.put(w, 1.0);
            }

            Set<U> outUsers = graph.getAdjacentNodes(v).collect(Collectors.toCollection(HashSet::new));
            outSum = outUsers.size() * 1.0;

            in.put(index, inWeights);
            out[index] = outSum;

            pr[index] = 1.0/(N+0.0);

            index++;
        }

        // iterations
        boolean hasConverged = false;
        for (int i = 0; i < MAXITER && !hasConverged; i++) 
        {           
            if (this.u == null || !ids.containsKey(this.u)) {
                for (int w = 0; w < N; w++) {
                    prAux[w] = r/(N+0.0);
                }
                // perso
            } else {
                for (int w = 0; w < N; w++) {
                    prAux[w] = 0.0;
                }
                index = ids.get(u);
                prAux[index] = r;
            }
           
            // calculate
            for (int u : in.keySet()) {
                for (U vId : in.get(u).keySet()) {
                    int v = ids.get(vId);
                    prAux[u] += (1 - r) * pr[v] * in.get(u).get(vId) / out[v];
                }
            }

            // make sum
            double sum = 0.0;
            for (int x = 0; x < N; x++) {
                sum += prAux[x];
            }

            // handle sinks
            for (int y = 0; y < N; y++) 
            {
                prAux[y] += ((1.0-sum) / (N+0.0));
            }

            // check convergency
            hasConverged = true;
            for (int z = 0; z < N; z++) {
                hasConverged = hasConverged && (Math.abs(prAux[z] - pr[z]) < THRESHOLD);
                pr[z] = prAux[z];
            }
        }

        Map<U, Double> pagerank = new HashMap<>();
        for (U u : users) 
        {
            pagerank.put(u, pr[ids.get(u)]);
        }

        return pagerank;
    }
    
    public Map<U, Double> compute(Graph<U> graph, Stream<U> users)
    {
        Map<U, Double> full = this.compute(graph);
        
        Map<U, Double> res = new ConcurrentHashMap<>();
        users.filter(graph::containsVertex).forEach(x -> res.put(x, full.get(x)));
        return res;
    }

    public double averageValue(Graph<U> graph)
    {
        return 1.0/(graph.getVertexCount()+0.0);
    }
}
