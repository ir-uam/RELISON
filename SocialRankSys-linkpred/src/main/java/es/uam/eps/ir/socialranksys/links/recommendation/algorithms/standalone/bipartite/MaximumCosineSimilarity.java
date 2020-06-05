/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.bipartite;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.linalg.Algebra;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommender. This method computes all the similarities between the authorities,
 * and scores recommended contacts by the maximum similarity over the authorities
 * that the target user is currently following.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class MaximumCosineSimilarity<U> extends BipartiteRecommender<U>
{

    /**
     * Vectorial representation of the authorities
     */
    private final Map<U, DoubleMatrix1D> authVectors;
    /**
     * Norms of the vectors.
     */
    private final Map<U, Double> normVectors;
    /**
     * Identifiers of the hubs.
     */
    private final Map<Long, Integer> hubIdx;
    /**
     * Similarities between pairs of users.
     */
    private final Object2DoubleMap<Pair<U>> similarities;
    
    /**
     * Constructor
     * @param graph the graph.
     */    
    public MaximumCosineSimilarity(FastGraph<U> graph)
    {
        super(graph, true);
        this.authVectors = new HashMap<>();
        this.normVectors = new HashMap<>();
        this.hubIdx = new HashMap<>();
        int i = 0;
        for(Long hub : hubs.keySet())
        {
            hubIdx.put(hub, i);
            ++i;
        }
        
        this.authorities.forEach((key, value) ->
        {
            DoubleMatrix1D vector = new SparseDoubleMatrix1D(this.hubs.size());
            double norm = this.computeVector(key, vector);
            this.authVectors.put(value, vector);
            this.normVectors.put(value, norm);
        });
        
        this.similarities = new Object2DoubleOpenHashMap<>();
        this.similarities.defaultReturnValue(-1.0);
        Algebra alg = new Algebra();
        authVectors.forEach((u, uVec) ->
        {
            double uNorm = this.normVectors.get(u);
            authVectors.forEach((v, vVec) ->
            {
                double vNorm = this.normVectors.get(v);
                double cos = alg.mult(uVec, vVec);
                cos = cos / (uNorm * vNorm);
                this.similarities.put(new Pair<>(u, v), cos);
            });
        });
    }
    
    @Override
    public Int2DoubleMap getScoresMap(int i) 
    {
        Int2DoubleMap scores = new Int2DoubleOpenHashMap();
        scores.defaultReturnValue(-1.0);

        U u = this.uIndex.uidx2user(i);
        
        // Get the identity in the graph.     
        Optional<Map.Entry<Long, U>> optional = this.hubs.entrySet().stream().filter(entry -> entry.getValue().equals(u)).findFirst();
        long bIdx = optional.isPresent() ? optional.get().getKey() : -1L;

        if(bIdx == -1L) return null; // Something failed...

        this.authorities.forEach((key, v) ->
        {
            int vIdx = uIndex.user2uidx(v);

            List<Long> adjacent = this.bipartiteGraph.getAdjacentNodes(bIdx).collect(Collectors.toCollection(ArrayList::new));
            double maxScore = Double.NEGATIVE_INFINITY;

            for (Long adj : adjacent)
            {

                U w1 = this.authorities.get(adj);
                if (w1 != u)
                {
                    double sim = this.similarities.getDouble(new Pair<>(v, w1));
                    if (sim > maxScore)
                    {
                        maxScore = sim;
                    }
                }
            }

            scores.put(vIdx, maxScore);
        });
        
        return scores;
    }

    
    /**
     * Computes the vector for an authority
     * @param auth The identifier of the authority
     * @param vector The vector
     * @return The norm of the vector (L2 norm)
     */
    private double computeVector(long auth, DoubleMatrix1D vector)
    {
        double module;
        
        module = this.hubs.keySet().stream().mapToDouble(hub -> 
        {
            if(this.bipartiteGraph.containsEdge(hub, auth))
            {
                double w = this.bipartiteGraph.getEdgeWeight(hub, auth);
                vector.setQuick(hubIdx.get(hub), w);
                return w*w;
            }
            return 0.0;
        }).sum();
        
        return Math.sqrt(module);
    }    
}
