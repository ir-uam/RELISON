/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialranksys.links.recommendation.knn.similarities.foaf;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.knn.similarities.GraphSimilarity;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Similarity based on the Adamic-Adar link prediction approach.
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 */
public class AdamicSimilarity extends GraphSimilarity
{
    /**
     * Map containing the length of the common neighborhoods between target and candidate users.
     */
    private final Int2DoubleMap wSizes;
    /**
     * Neighborhood selection for the target users.
     */
    private final EdgeOrientation uSel;
        /**
     * Neighborhood selection for the candidate users.
     */
    private final EdgeOrientation vSel;
    /**
     * Neighborhood selection for the intermediate users
     */
    private final EdgeOrientation wSel;
    
    
    public AdamicSimilarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel, EdgeOrientation wSel)
    {
        super(graph);
        
        wSizes = new Int2DoubleOpenHashMap();
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        this.wSel = wSel;
        
        this.graph.getAllNodesIds().forEach(widx -> wSizes.put(widx,graph.getNeighborhood(widx, wSel).count() + 0.0));
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
                    return 1.0/ Math.log(wSizes.get(vidx.intValue()) + 2.0);
                }
                return 0.0;
            }).sum();
    }

    @Override
    public Stream<Tuple2id> similarElems(int uidx)
    {
        Int2DoubleMap sims = new Int2DoubleOpenHashMap();
        this.graph.getNeighborhood(uidx, uSel).forEach(widx -> {
            double val = Math.log(wSizes.get(widx.intValue())+2.0);
            this.graph.getNeighborhood(widx, vSel).filter(vidx -> vidx != uidx).forEach(vidx ->
                sims.put(vidx.intValue(), sims.getOrDefault(vidx.intValue(), 0.0) + 1.0/val));
        });
        
        return sims.int2DoubleEntrySet().stream().map(x -> new Tuple2id(x.getIntKey(), x.getDoubleValue()));
    }

}
