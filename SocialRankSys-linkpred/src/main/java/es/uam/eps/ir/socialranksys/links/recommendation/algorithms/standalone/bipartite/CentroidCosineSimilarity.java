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
import cern.jet.math.Functions;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Recommender. Builds a centroid for each user in the network, using the vectors of the 
 * followed users. The score is then computed as the cosine similarity of those centroids.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class CentroidCosineSimilarity<U> extends BipartiteRecommender<U>
{

    /**
     * Vectorial representation of the authorities.
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
     * Constructor
     * @param graph the graph.
     */
    public CentroidCosineSimilarity(FastGraph<U> graph)
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
    }
    
    @Override
    public Int2DoubleMap getScoresMap(int i) 
    {
        Int2DoubleMap scores = new Int2DoubleOpenHashMap();
        U u = this.uIndex.uidx2user(i);
        // Get the identity in the graph.
        Algebra algebra = new Algebra();
        
        Optional<Map.Entry<Long, U>> optional  = this.hubs.entrySet().stream().filter(entry -> entry.getValue().equals(u)).findFirst();
        long bIdx = optional.isPresent() ? optional.get().getKey() : -1L;

        if(bIdx == -1L) return null; // an error ocurred.
        
        DoubleMatrix1D centroid = new SparseDoubleMatrix1D(this.hubs.size());
        double norm = this.computeCentroid(bIdx, centroid);
        
        this.authorities.forEach((key, authUser) ->
        {
            DoubleMatrix1D authVector = this.authVectors.get(authUser);
            double normVector = this.normVectors.get(authUser);

            int vIdx = this.uIndex.user2uidx(authUser);
            double score = algebra.mult(authVector, centroid) / (normVector * norm);
            scores.put(vIdx, score);
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
    
    /**
     * Computes a centroid for a hub
     * @param hub The identifier of the hub
     * @param centroid The vector
     * @return The norm of the vector (L2 norm)
     */
    private double computeCentroid(long hub, DoubleMatrix1D centroid) 
    {
        Algebra alg = new Algebra();
        
        this.bipartiteGraph.getAdjacentNodes(hub).forEach(auth -> 
        {
            U v = this.authorities.get(auth);
            centroid.assign(this.authVectors.get(v),Functions.plus);
        });
        
        int adj = this.bipartiteGraph.getAdjacentNodesCount(hub);
        if(adj > 0)
            centroid.assign(Functions.div(adj+0.0));
        
        return alg.norm2(centroid);
    }
    
}
