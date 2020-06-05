/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.knn.similarities.ir;


import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.knn.similarities.GraphSimilarity;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.function.IntToDoubleFunction;
import java.util.stream.Stream;

/**
 * Similarity based on the Query Likelihood IR model with Jelinek-Mercer smoothing.
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 */
public class QLJMSimilarity extends GraphSimilarity
{
    /**
     * Parameter which controls the trade-off between the regularization term and the original term
     * in the formula.
     */
    private final double lambda;
    /**
     * Neighborhood sizes
     */
    private final Int2DoubleMap size;
    /**
     * Sum of the neighborhood sizes
     */
    private final double fullSize;
    /**
     * Neighborhood selection for the target users.
     */
    private final EdgeOrientation uSel;
        /**
     * Neighborhood selection for the candidate users.
     */
    private final EdgeOrientation vSel;
    /**
     * For each user, computes the proportion of neighbors it has, in comparison with the sum of all neighborhood sizes.
     */
    private final Int2DoubleMap pc;
    
    /**
     * Constructor.
     * @param graph training graph.
     * @param uSel neighborhood selection for the target user.
     * @param vSel neighborhood selection for the candidate user.
     * @param lambda parameter which controls the trade-off between the regularization term and the original probability.
     */
    public QLJMSimilarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel, double lambda)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel;
        this.lambda = lambda/(1-lambda);
        
        this.size = new Int2DoubleOpenHashMap();
        EdgeOrientation wSel = vSel.invertSelection();
        if(!graph.isDirected() || vSel.equals(EdgeOrientation.UND))
        {
            this.fullSize = this.graph.getAllNodesIds().mapToDouble(vidx -> 
            {
                double vS = this.graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(Tuple2id::v2).sum();
                this.size.put(vidx, vS);
                return vS;
            }).sum();
            this.pc = size;
        }
        else
        {
            this.pc = new Int2DoubleOpenHashMap();
            this.fullSize = this.graph.getAllNodesIds().mapToDouble(vidx -> 
            {
               double vS = this.graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(Tuple2id::v2).sum();
               double wS = this.graph.getNeighborhoodWeights(vidx, wSel).mapToDouble(Tuple2id::v2).sum();
               this.size.put(vidx, vS);
               this.pc.put(vidx, wS);
               return vS;
            }).sum();
        }
        
    }
    
    @Override
    public IntToDoubleFunction similarity(int uidx)
    {
        Int2DoubleMap uNeigh = new Int2DoubleOpenHashMap();
        graph.getNeighborhoodWeights(uidx, uSel).forEach(v -> uNeigh.put(v.v1, v.v2));
        
        return (int vidx) -> 
        {
            double vS = this.size.get(vidx);
            
            return graph.getNeighborhoodWeights(vidx, vSel).filter(x -> uNeigh.keySet().contains(x.v1)).mapToDouble(w -> 
            {
                double wPc = this.fullSize/this.pc.get(w.v1);
                double val = lambda*(w.v2/vS)*wPc;
                if(Double.isNaN(val) || Double.isInfinite(val)) return Double.NEGATIVE_INFINITY;
                return uNeigh.get(w.v1)* Math.log(1.0+val);
            }).sum();
        };
    }

    @Override
    public Stream<Tuple2id> similarElems(int uidx)
    {
        Int2DoubleOpenHashMap sims = new Int2DoubleOpenHashMap();
        sims.defaultReturnValue(0.0);
        
        graph.getNeighborhoodWeights(uidx, uSel).forEach(w -> 
        {
            int widx = w.v1;
            double uW = w.v2;
            double wPc = this.fullSize/(this.pc.get(widx));
            graph.getNeighborhoodWeights(widx, vSel.invertSelection()).filter(v -> v.v1 != uidx).forEach(v -> 
            {
                double s = this.size.getOrDefault(v.v1, 0.0);
                double val = lambda*wPc*(v.v2/s);
                if(Double.isNaN(val) || Double.isInfinite(val)) sims.put(v.v1, Double.NEGATIVE_INFINITY);
                else sims.addTo(v.v1, uW* Math.log(val + 1.0));
            });
        });
        
        return sims.int2DoubleEntrySet().stream().filter(x -> x.getDoubleValue() > Double.NEGATIVE_INFINITY).map(x -> new Tuple2id(x.getIntKey(), x.getDoubleValue()));
    }

}
