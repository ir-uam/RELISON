/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.knn.similarities.foaf;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.knn.similarities.GraphSimilarity;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Similarity based on the Most Common Neighbors link prediction algorithm
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 */
public class MostCommonNeighborsSimilarity extends GraphSimilarity
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
     * Constructor.
     * @param graph the training graph.
     * @param uSel selection for the target user neighborhood.
     * @param vSel selection for the candidate user neighborhood
     */
    public MostCommonNeighborsSimilarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
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
            }).sum();
    }

    @Override
    public Stream<Tuple2id> similarElems(int uidx)
    {
        Int2DoubleMap sims = new Int2DoubleOpenHashMap();
        this.graph.getNeighborhoodWeights(uidx, uSel).forEach(vidx ->
            this.graph.getNeighborhoodWeights(vidx.v1, vSel).filter(widx -> widx.v1 != uidx).forEach(widx ->
                sims.put(widx.v1, sims.getOrDefault(widx.v1, 0.0) + 1.0)));
        
        return sims.int2DoubleEntrySet().stream().map(x -> new Tuple2id(x.getIntKey(), x.getDoubleValue()));
    }
}
