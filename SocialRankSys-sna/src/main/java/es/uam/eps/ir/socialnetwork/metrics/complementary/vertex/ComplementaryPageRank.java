/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.complementary.vertex;

import es.uam.eps.ir.socialnetwork.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.graph.Graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes the PageRank values in the complementary graph for the different nodes in the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the nodes.
 */
public class ComplementaryPageRank<U> implements VertexMetric<U> 
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
    public ComplementaryPageRank(double r)
    {
        this.r = r;
        this.u = null;
    }
    
    /**
     * Constructor (for personalized PageRank)
     * @param r the teleport parameter.
     * @param u the original user.
     */
    public ComplementaryPageRank(double r, U u)
    {
        this.r = r;
        this.u = u;
    }
    
    @Override
    public double compute(Graph<U> graph, U user) {
        return this.compute(graph).get(user);
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph) 
    {
        Set<U> users = graph.getAllNodes().collect(Collectors.toCollection(HashSet::new));
        Map<U, Integer> ids = new HashMap<>();
        
        int N = users.size();

        // data
        Map<Integer, Set<U>> in = new HashMap<>();
        Double[] out = new Double[N];
        Double[] pr = new Double[N];
        Double[] prAux = new Double[N];

        // initialize
        int index = 0;
        for (U v : users) 
        {
            ids.put(v, index);

            Set<U> inWeights = null;
            double outSum = 0.0;

            Set<U> inUsers = graph.getIncidentNodes(v).collect(Collectors.toCollection(HashSet::new));
            if (!inUsers.isEmpty())
            {
                inWeights = new HashSet<>(inUsers);
            }
            Set<U> outUsers = graph.getAdjacentNodes(v).collect(Collectors.toCollection(HashSet::new));
            if (!outUsers.isEmpty())
            {
                outSum = outUsers.size() * 1.0;
            }

            in.put(index, inWeights);
            out[index] = outSum;

            pr[index] = 1.0/(N+0.0);

            index++;
        }

        // iterations
        boolean hasConverged = false;
        for (int i = 0; i < MAXITER && !hasConverged; i++) 
        {           
            double aux = 0.0;

            // Compute the full sum
            for(int j = 0; j < out.length; ++j)
            {
                aux += pr[j]/(N - out[j] + 0.0);
            }
            
            if (this.u == null || !ids.containsKey(this.u)) {
                for (int w = 0; w < N; w++) {
                    prAux[w] = r/N;
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
                double aux2 = 0.0;
                for (U vId : in.get(u)) {
                    int v = ids.get(vId);
                    if(out[v] < N)
                    {
                        aux2 +=  pr[v] / (N - out[v] + 0.0);
                    }
                }
                prAux[u] += (1 - r) *(aux - aux2);
                if(prAux[u] < 0.0)
                {
                    System.err.println("EYY");
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
        for (U us : users) 
        {
            pagerank.put(us, pr[ids.get(us)]);
        }

        return pagerank;
    }

    @Override
    public double averageValue(Graph<U> graph) 
    {
        return 1.0/(graph.getVertexCount()+0.0);
    }
}
