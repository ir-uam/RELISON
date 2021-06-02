/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.knn.similarities.foaf;

import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.knn.similarities.GraphSimilarity;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.foaf.Cosine;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Similarity based on the Salton index (a.k.a.as cosine similarity).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see Cosine
 */
public class CosineSimilarity extends GraphSimilarity
{
    /**
     * Map containing the length of the target users.
     */
    private final Int2DoubleMap uMods;
    /**
     * Map containing the lenght of the candidate users.
     */
    private final Int2DoubleMap vMods;
    /**
     * Neighborhood selection for the target users.
     */
    private final EdgeOrientation uSel;
        /**
     * Neighborhood selection for the candidate users.
     */
    private final EdgeOrientation vSel;    
    
    /**
     * Constructor.
     * @param graph the training graph.
     * @param uSel  neighborhood selection for the first user.
     * @param vSel  neighborhood selection for the second user.
     */
    public CosineSimilarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        
        this.uMods = new Int2DoubleOpenHashMap();
        
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        
        this.graph.getAllNodesIds().forEach(uidx -> {
            double mod = this.graph.getNeighborhoodWeights(uidx, uSel).mapToDouble(x -> x.v2*x.v2).sum();
            this.uMods.put(uidx, mod);
        });
        
        if(vSel.equals(uSel) || !this.graph.isDirected())
        {
            this.vMods = this.uMods;
        }
        else
        {
            this.vMods = new Int2DoubleOpenHashMap();
            this.graph.getAllNodesIds().forEach(uidx -> 
            {
                double mod = this.graph.getNeighborhoodWeights(uidx, vSel).mapToDouble(x -> x.v2*x.v2).sum();
                this.vMods.put(uidx, mod);
            });
        }
    }
    
    @Override
    public IntToDoubleFunction similarity(int idx)
    {
        IntSet set = this.graph.getNeighborhood(idx, uSel).collect(Collectors.toCollection(IntOpenHashSet::new));
        return (int idx2) ->
            this.graph.getNeighborhood(idx, vSel.invertSelection()).mapToDouble(vidx ->
            {
                if(set.contains(vidx.intValue()))
                {
                    return 1.0;
                }
                return 0.0;
            }).sum()/ Math.sqrt(this.uMods.get(idx)*this.vMods.get(idx2));
    }

    @Override
    public Stream<Tuple2id> similarElems(int uidx)
    {
        Int2DoubleMap sims = new Int2DoubleOpenHashMap();
        double mod = this.uMods.get(uidx);
        this.graph.getNeighborhoodWeights(uidx, uSel).forEach(widx ->
            this.graph.getNeighborhoodWeights(widx.v1, vSel).filter(vidx -> vidx.v1 != uidx).forEach(vidx ->
                sims.put(vidx.v1, sims.getOrDefault(vidx.v1, 0.0) + vidx.v2*widx.v2)));
        
        return sims.int2DoubleEntrySet().stream().map(x -> new Tuple2id(x.getIntKey(), x.getDoubleValue()/ Math.sqrt(mod*vMods.get(x.getIntKey()))));
    }

}
