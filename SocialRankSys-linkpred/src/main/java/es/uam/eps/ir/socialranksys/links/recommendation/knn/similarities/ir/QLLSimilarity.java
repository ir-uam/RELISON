/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialranksys.links.recommendation.knn.similarities.ir;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.knn.similarities.GraphSimilarity;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.function.IntToDoubleFunction;
import java.util.stream.Stream;

/**
 * Similarity based on the Query Likelihood IR model with Laplace smoothing.
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 */
public class QLLSimilarity extends GraphSimilarity
{
    /**
     * Smoothing parameter.
     */
    private final double gamma;
    /**
     * Size of the target user neighborhoods
     */
    private final Int2DoubleMap uSize;
    /**
     * Size of the candidate user neighborhoods
     */
    private final Int2DoubleMap vSize;
    /**
     * Neighborhood selection for the target user.
     */
    private final EdgeOrientation uSel;
    /**
     * Neighborhood selection for the candidate user.
     */
    private final EdgeOrientation vSel;
    
    /**
     * Constructor.
     * @param graph the training graph.
     * @param uSel neighborhood selection for the target user.
     * @param vSel neighborhood selection for the candidate user.
     * @param gamma smoothing parameter.
     */
    public QLLSimilarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel, double gamma)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel;
        this.gamma = gamma;
        
        uSize = new Int2DoubleOpenHashMap();
        if(!graph.isDirected() || uSel.equals(vSel))
        {
            this.graph.getAllNodesIds().forEach(vidx -> 
            {
                double s = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(x -> x.v2).sum();
                this.uSize.put(vidx, s);
            });
            vSize = uSize;
        }
        else
        {
            this.vSize = new Int2DoubleOpenHashMap();
            this.graph.getAllNodesIds().forEach(vidx -> 
            {
                double uS = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(x -> x.v2).sum();
                double vS = graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(x -> x.v2).sum();
                this.uSize.put(vidx, uS);
                this.vSize.put(vidx, vS);
            });
        }
        
    }
    
    @Override
    public IntToDoubleFunction similarity(int uidx)
    {
        Int2DoubleMap uNeigh = new Int2DoubleOpenHashMap();
        graph.getNeighborhoodWeights(uidx, uSel).forEach(v -> uNeigh.put(v.v1, v.v2));
        double uS = this.uSize.get(uidx);
        long numUsers = this.graph.getVertexCount();

        return (int vidx) -> 
        {
            double vS = this.vSize.get(vidx);
            return this.graph.getNeighborhoodWeights(vidx, vSel).filter(w -> uNeigh.containsKey(w.v1)).mapToDouble(w -> 
            {
                double uW = uNeigh.get(w.v1);
                return uW* Math.log((w.v2 + gamma)/gamma);
            }).sum() - uS* Math.log(numUsers +  vS/gamma);
        };
    }

    @Override
    public Stream<Tuple2id> similarElems(int uidx)
    {
        Int2DoubleOpenHashMap sims = new Int2DoubleOpenHashMap();
        double uS = this.uSize.get(uidx);
        long numUsers = this.graph.getVertexCount();
        graph.getNeighborhoodWeights(uidx, uSel).forEach(w -> {
            int widx = w.v1;
            double uW = w.v2;
            
            graph.getNeighborhoodWeights(widx, vSel.invertSelection()).filter(v -> v.v1 != uidx).forEach(v -> {
                double val = uW* Math.log((v.v2 + this.gamma)/(this.gamma));
                sims.addTo(v.v1, val);
            });
        });
        
        return sims.int2DoubleEntrySet().stream().map(x -> new Tuple2id(x.getIntKey(), x.getDoubleValue() - uS* Math.log(numUsers + this.vSize.get(x.getIntKey())/this.gamma)));
    }

}
