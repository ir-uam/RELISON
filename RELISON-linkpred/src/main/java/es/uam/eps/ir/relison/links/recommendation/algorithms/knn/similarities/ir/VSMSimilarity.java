/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.knn.similarities.ir;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.recommendation.algorithms.knn.similarities.GraphSimilarity;
import es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.ir.VSM;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.function.IntToDoubleFunction;
import java.util.stream.Stream;

/**
 * Similarity based on the vector space model from Information Retrieval.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see VSM
 */
public class VSMSimilarity extends GraphSimilarity
{
    /**
     * Neighborhood selection for the target user.
     */
    private final EdgeOrientation uSel;
    /**
     * Neighborhood selection for the candidate user.
     */
    private final EdgeOrientation vSel;
    /**
     * IDF values for the target users.
     */
    private final Int2DoubleMap uIdf;
    /**
     * IDF values for the candidate users.
     */
    private final Int2DoubleMap vIdf;

    /**
     * TF-IDF modules for the target users.
     */
    private final Int2DoubleMap uMod;
    /**
     * TF-IDF modules for the candidate users.
     */
    private final Int2DoubleMap vMod;
    
    /**
     * Constructor.
     * @param graph training graph.
     * @param uSel  Selection of the target user neighborhood.
     * @param vSel  Selection of the candidate user neighborhood.
     */
    public VSMSimilarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel;
        
        this.uIdf = new Int2DoubleOpenHashMap();
        this.uMod = new Int2DoubleOpenHashMap();
        
        if(!graph.isDirected() || uSel.equals(vSel))
        {
            this.graph.getAllNodesIds().forEach(uidx ->
                    this.uIdf.put(uidx, this.calculateIdf(uidx, uSel.invertSelection())));
            
            this.graph.getAllNodesIds().forEach(uidx -> 
            {
                double mod = this.graph.getNeighborhoodWeights(uidx, uSel).mapToDouble(widx -> 
                {
                    double value = this.calculateTf(widx.v2)*this.uIdf.get(widx.v1);
                    return value*value;
                }).sum();
                this.uMod.put(uidx, mod);
            });
            
            this.vIdf = uIdf;
            this.vMod = vIdf;
        }
        else
        {
            this.vIdf = new Int2DoubleOpenHashMap();
            this.vMod = new Int2DoubleOpenHashMap();
            this.graph.getAllNodesIds().forEach(uidx -> 
            {
                this.uIdf.put(uidx, this.calculateIdf(uidx, uSel.invertSelection()));
                this.vIdf.put(uidx, this.calculateIdf(uidx, vSel.invertSelection()));
            });
            
            this.graph.getAllNodesIds().forEach(uidx -> 
            {
                double mod = this.graph.getNeighborhoodWeights(uidx, uSel).mapToDouble(widx -> 
                {
                    double value = this.calculateTf(widx.v2)*this.uIdf.get(widx.v1);
                    return value*value;
                }).sum();
                this.uMod.put(uidx, mod);
                
                double mod2 = this.graph.getNeighborhoodWeights(uidx, vSel).mapToDouble(widx -> 
                {
                    double value = this.calculateTf(widx.v2)*this.vIdf.get(widx.v1);
                    return value*value;
                }).sum();
                this.vMod.put(uidx, mod2);
            });
        }
    }

    /**
     * Given the weight of an edge, it computes the term frequency.
     * @param weight the weight
     * @return the term frequency (tf) value.
     */
    private double calculateTf(double weight)
    {
        return 1.0 + Math.log(weight)/ Math.log(2.0);
    }
    
    /**
     * Compute the inverse document frequency of a node
     * @param uidx  the node
     * @param s     the orientation of the neighbors
     * @return the value of the idf
     */
    private double calculateIdf(int uidx, EdgeOrientation s)
    {
        double num = this.graph.getNeighborhood(uidx, s).count() + 0.0;
        return Math.log(1.0 + (this.graph.getVertexCount() + 0.0)/(num + 1.0))/ Math.log(2.0);
    }
    
    @Override
    public IntToDoubleFunction similarity(int idx)
    {
        Int2DoubleMap uNeighs = new Int2DoubleOpenHashMap();
        this.graph.getNeighborhoodWeights(idx, uSel).forEach(x -> uNeighs.put(x.v1, this.calculateTf(x.v2)*this.uIdf.get(x.v1)));
        
        return (int idx2) -> 
        {
            double mods = this.uMod.get(idx)*this.vMod.get(idx);
            if(mods == 0.0) return 0.0;
            double prod = this.graph.getNeighborhoodWeights(idx, vSel).filter(widx -> uNeighs.containsKey(widx.v1))
                .mapToDouble(widx -> 
                {
                    double idf = this.vIdf.get(widx.v1);
                    return this.calculateTf(widx.v2)*idf*uNeighs.get(widx.v1);
                }).sum();
            return prod/ Math.sqrt(mods);
        };
    }

    @Override
    public Stream<Tuple2id> similarElems(int idx)
    {
        Int2DoubleOpenHashMap sims = new Int2DoubleOpenHashMap();
        sims.defaultReturnValue(0.0);
        double uModule = this.uMod.get(idx);
        this.graph.getNeighborhoodWeights(idx, uSel).forEach(widx -> {
            double uIdfW = this.uIdf.get(widx.v1);
            double vIdfW = this.vIdf.get(widx.v1);
            
            this.graph.getNeighborhoodWeights(widx.v1, vSel.invertSelection()).forEach(vidx -> 
            {
                double prod  = this.calculateTf(widx.v2)*uIdfW*this.calculateTf(vidx.v2)*vIdfW;
                sims.addTo(vidx.v1, prod);
            });
        });
        
        return sims.int2DoubleEntrySet().stream().map(x -> new Tuple2id(x.getIntKey(), x.getDoubleValue()/ Math.sqrt(uModule*this.vMod.get(x.getIntKey()))));
    }
    
}
