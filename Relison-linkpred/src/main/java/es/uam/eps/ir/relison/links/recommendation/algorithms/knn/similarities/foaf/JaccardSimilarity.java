/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.knn.similarities.foaf;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.recommendation.algorithms.knn.similarities.GraphSimilarity;
import es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.foaf.Jaccard;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Similarity based on the Jaccard similarity link prediction algorithm
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see Jaccard
 */
public class JaccardSimilarity extends GraphSimilarity
{
    /**
     * Orientation for the selection of neighbors for the target user.
     */
    private final EdgeOrientation uSel;
    /**
     * Orientation for the selection of neighbors for the candidate user.
     */
    private final EdgeOrientation vSel;

    /**
     * Number of neighbors of the target users.
     */
    private final Int2DoubleMap uSizes;
    /**
     * Number of neighbors of the candidate users.
     */
    private final Int2DoubleMap vSizes;
    
    /**
     * Constructor.
     * @param graph the training graph.
     * @param uSel  selection for the target user neighborhood.
     * @param vSel  selection for the candidate user neighborhood
     */
    public JaccardSimilarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        this.uSizes = new Int2DoubleOpenHashMap();
        this.graph.getAllNodesIds().forEach(uidx ->
                uSizes.put(uidx, graph.getNeighborhood(uidx, uSel).count() + 0.0));
        
        if(uSel.equals(vSel) || !graph.isDirected())
        {
            vSizes = uSizes;
        }
        else
        {
            this.vSizes = new Int2DoubleOpenHashMap();
            this.graph.getAllNodesIds().forEach(uidx ->
                vSizes.put(uidx, graph.getNeighborhood(uidx, vSel).count() + 0.0));
        }
    }
    
    @Override
    public IntToDoubleFunction similarity(int idx)
    {
        IntSet set = this.graph.getNeighborhood(idx, uSel).collect(Collectors.toCollection(IntOpenHashSet::new));
        return (int idx2) -> 
        {
            if(this.vSizes.get(idx2) == 0 || set.isEmpty()) return 0.0;
            double inter = this.graph.getNeighborhood(idx, vSel.invertSelection()).mapToDouble(vidx -> 
            {
                if(set.contains(vidx.intValue()))
                {
                    return 1.0;
                }
                return 0.0;
            }).sum();
            
            return inter/(this.uSizes.get(idx) + this.vSizes.get(idx2) - inter);
        };
    }

    @Override
    public Stream<Tuple2id> similarElems(int uidx)
    {
        Int2DoubleMap sims = new Int2DoubleOpenHashMap();
        this.graph.getNeighborhoodWeights(uidx, uSel).forEach(vidx ->
                this.graph.getNeighborhoodWeights(vidx.v1, vSel).filter(widx -> widx.v1 != uidx).forEach(widx ->
                        sims.put(widx.v1, sims.getOrDefault(widx.v1, 0.0) + 1.0)));
        
        return sims.int2DoubleEntrySet().stream().map(x -> new Tuple2id(x.getIntKey(), x.getDoubleValue()/(this.uSizes.get(uidx)+this.vSizes.get(x.getIntKey())-x.getDoubleValue())));
    }
}
