/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.updateable.knn.sim;

import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import it.unimi.dsi.fastutil.ints.*;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Updateable version of the cosine similarity.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class UpdateableGraphCosineSimilarity extends UpdateableGraphSimilarity
{
    /**
     * A map for storing the norms.
     */
    private final Int2DoubleOpenHashMap norm2map;

    /**
     * Constructor.
     *
     * @param graph the social network graph.
     */
    public UpdateableGraphCosineSimilarity(FastGraph<?> graph)
    {
        super(graph);
        //this.similarities = new Int2ObjectOpenHashMap<>();
        this.norm2map = new Int2DoubleOpenHashMap();
        graph.getAllNodesIds().forEach(uidx ->
        {
            Int2DoubleOpenHashMap uMap = new Int2DoubleOpenHashMap();
            uMap.defaultReturnValue(0.0);
            //similarities.put(uidx, uMap);
            norm2map.put(uidx, 0.0);
        });

        graph.getAllNodesIds().forEach(uidx ->
        {
            //Int2DoubleOpenHashMap uMap = this.similarities.get(uidx);
            double norm = graph.getNeighborhoodWeights(uidx, EdgeOrientation.OUT).mapToDouble(uWeight ->
            {
                int widx = uWeight.v1;
                double uW = uWeight.v2;
                return uW*uW;
            }).sum();

            this.norm2map.put(uidx, norm);
        });
    }


    @Override
    public IntList updateAdd(int idx1, int idx2, double val)
    {
        IntList list = new IntArrayList();
        this.norm2map.addTo(idx1, val*val);
        return list;
    }

    @Override
    public void updateDel(int idx1, int idx2)
    {

    }

    @Override
    public void updateAddElement()
    {
        int uidx = norm2map.size();
        Int2DoubleOpenHashMap uMap = new Int2DoubleOpenHashMap();
        uMap.defaultReturnValue(0.0);
        norm2map.put(uidx, 0.0);
    }

    @Override
    public IntToDoubleFunction similarity(int uidx)
    {
        double normU = this.norm2map.get(uidx);
        IntSet uNeighs = this.graph.getNeighborhood(uidx, EdgeOrientation.OUT).collect(Collectors.toCollection(IntOpenHashSet::new));
        return (vidx) ->
        {
            double score = 0.0;
            double normV = this.norm2map.get(vidx);
            double prod = normU*normV;
            if(prod > 0.0)
            {
                IntSet set = this.graph.getNeighborhood(vidx, EdgeOrientation.OUT).collect(Collectors.toCollection(IntOpenHashSet::new));
                set.retainAll(uNeighs);
                for(int aux : set)
                {
                    score += graph.getEdgeWeight(uidx,aux)*graph.getEdgeWeight(vidx, aux);
                }

                score /= Math.sqrt(prod);
                return score;

            }
            return 0.0;
        };
    }

    @Override
    public Stream<Tuple2id> similarElems(int uidx)
    {
        Int2DoubleOpenHashMap sims = new Int2DoubleOpenHashMap();
        sims.defaultReturnValue(0.0);
        double normU = this.norm2map.get(uidx);

        this.graph.getNeighborhoodWeights(uidx, EdgeOrientation.OUT).forEach(w ->
        {
            double uW = w.v2;
            int widx = w.v1;
            this.graph.getNeighborhoodWeights(widx, EdgeOrientation.IN).forEach(v -> sims.addTo(v.v1, uW*v.v2));
        });

        return sims.int2DoubleEntrySet().stream().map(entry ->
        {
            double normV = this.norm2map.get(entry.getIntKey());
            return new Tuple2id(entry.getIntKey(), entry.getDoubleValue()/Math.sqrt(normU*normV));
        });
    }
}
