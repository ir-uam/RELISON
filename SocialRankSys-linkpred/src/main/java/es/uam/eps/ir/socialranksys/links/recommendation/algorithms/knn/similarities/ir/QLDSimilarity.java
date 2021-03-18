/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
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
 * Similarity based on the Query Likelihood IR model with Dirichlet smoothing.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.ir.QLD
 */
public class QLDSimilarity extends GraphSimilarity
{
    /**
     * Selection of the neighbors of the target user.
     */
    private final EdgeOrientation uSel;
    /**
     * Selection of the neighbors of the candidate user.
     */
    private final EdgeOrientation vSel;
    /**
     * Sum of all the sizes of the user neighborhoods.
     */
    private final double fullSize;
    /**
     * Smoothing parameter.
     */
    private final double mu;
        /**
     * Neighborhood sizes for the target user
     */
    private final Int2DoubleMap uSize;
    /**
     * Neighborhood sizes for the candidate user
     */
    private final Int2DoubleMap vSize;
    /**
     * Sizes of the common neighbors.
     */
    private final Int2DoubleMap pc;
    
    /**
     * Constructor.
     * @param graph training graph.
     * @param uSel  neighborhood selection for the target user.
     * @param vSel  neighborhood selection for the candidate user.
     * @param mu    smoothing parameter.
     */
    public QLDSimilarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel, double mu)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel;
        this.mu = mu;
        this.uSize = new Int2DoubleOpenHashMap();
        
        EdgeOrientation wSel = vSel.invertSelection();
        
        if(!graph.isDirected() || (uSel.equals(vSel) && uSel.equals(EdgeOrientation.UND))) // Cases UND-UND
        {
            this.fullSize = this.graph.getAllNodesIds().mapToDouble(vidx -> 
            {
                double uS = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(Tuple2id::v2).sum();
                this.uSize.put(vidx, uS);
                return uS;
            }).sum();
            this.vSize = uSize;
            this.pc = uSize;
        }
        else if(uSel.equals(vSel)) //CASES IN-IN,OUT-OUT
        {
            this.pc = new Int2DoubleOpenHashMap();
            this.fullSize = this.graph.getAllNodesIds().mapToDouble(vidx -> 
            {
                double uS = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(Tuple2id::v2).sum();
                double wS = graph.getNeighborhoodWeights(vidx, wSel).mapToDouble(Tuple2id::v2).sum();
                this.uSize.put(vidx, uS);
                this.pc.put(vidx, wS);
                return uS;
            }).sum();
            this.vSize = uSize;
        }
        else if(uSel.equals(vSel.invertSelection())) // CASES IN-OUT,OUT-IN
        {
            this.vSize = new Int2DoubleOpenHashMap();
            this.fullSize = this.graph.getAllNodesIds().mapToDouble(vidx -> 
            {
                double uS = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(Tuple2id::v2).sum();
                double wS = graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(Tuple2id::v2).sum();
                this.uSize.put(vidx, uS);
                this.vSize.put(vidx, wS);
                return uS;
            }).sum();
            this.pc = uSize;
        }
        else if(vSel.equals(EdgeOrientation.UND)) // CASES IN-UND, OUT-UND
        {
            this.vSize = new Int2DoubleOpenHashMap();
            this.fullSize = this.graph.getAllNodesIds().mapToDouble(vidx -> 
            {
                double uS = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(Tuple2id::v2).sum();
                double vS = graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(Tuple2id::v2).sum();
                this.uSize.put(vidx, uS);
                this.vSize.put(vidx, vS);
                return uS;
            }).sum();
            this.pc = vSize;
        }
        else // CASES UND-IN, UND-OUT
        {
            this.vSize = new Int2DoubleOpenHashMap();
            this.pc = new Int2DoubleOpenHashMap();
            this.fullSize = this.graph.getAllNodesIds().mapToDouble(vidx -> 
            {
                double uS = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(Tuple2id::v2).sum();
                double vS = graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(Tuple2id::v2).sum();
                double wS = uS - vS; // Considering that weight(UND,x,y) = weight(x,y) + weight(y,x)
                this.uSize.put(vidx, uS);
                this.vSize.put(vidx, vS);
                this.pc.put(vidx, wS);
                return uS;
            }).sum();
        }
    }
    
    @Override
    public IntToDoubleFunction similarity(int uidx)
    {
        Int2DoubleMap uNeighs = new Int2DoubleOpenHashMap();
        this.graph.getNeighborhoodWeights(uidx, uSel).forEach(x -> uNeighs.put(x.v1, x.v2));
        
        double uS = this.uSize.get(uidx);
        
        return (int vidx) ->
        {
            double value = graph.getNeighborhoodWeights(vidx, vSel).filter(widx -> uNeighs.keySet().contains(widx.v1)).mapToDouble(widx -> 
            {
                double uWeight = uNeighs.get(widx.v1);
                double vWeight = widx.v2;
                
                double wPc = this.fullSize/(this.mu*this.pc.get(widx.v1));
                
                double val = uWeight* Math.log(vWeight*wPc + 1.0);
                return 0.0;
            }).sum();
            
            if(Double.isNaN(value) || Double.isInfinite(value)) return Double.NEGATIVE_INFINITY;
            return value - uS* Math.log(1.0+this.vSize.get(vidx)/mu);
        };
    }

    @Override
    public Stream<Tuple2id> similarElems(int uidx)
    {
        Int2DoubleOpenHashMap sims = new Int2DoubleOpenHashMap();
        sims.defaultReturnValue(0.0);
        
        double norm = this.uSize.get(uidx);
        graph.getNeighborhoodWeights(uidx, uSel).forEach(w -> 
        {
            double uWeight = w.v2;
            int widx = w.v1;
            double wPc = this.fullSize/(this.mu*this.pc.get(widx));
            
            graph.getNeighborhoodWeights(widx, vSel).filter(vidx -> vidx.v1 != uidx).forEach(v -> 
            {
                double vWeight = v.v2;
                
                double val = uWeight* Math.log(vWeight*wPc + 1.0);
                if(Double.isNaN(val) || Double.isInfinite(val)) sims.put(v.v1, Double.NEGATIVE_INFINITY);
                else sims.addTo(v.v1, val);
            });
        });
        
        return sims.int2DoubleEntrySet().stream().filter(x -> x.getDoubleValue() > Double.NEGATIVE_INFINITY).map(x -> new Tuple2id(x.getIntKey(), x.getDoubleValue() - norm* Math.log(1.0 + this.vSize.get(x.getIntKey())/mu)));
    }

}
