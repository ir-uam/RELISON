/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.distance;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import es.uam.eps.ir.relison.graph.Adapters;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.recommendation.UserFastRankingRecommender;
import es.uam.eps.ir.relison.metrics.PairMetric;
import es.uam.eps.ir.relison.metrics.distance.pair.Distance;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.Map;
import java.util.Random;

/**
 * Recommends users by computing the distance between two of them.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ShortestDistance<U> extends UserFastRankingRecommender<U>
{
    /**
     * Distance map.
     */
    private final DoubleMatrix2D distances;
    /**
     * Direction of the paths for computing the distance
     */
    private final EdgeOrientation orientation;
    
    /**
     * Constructor.
     * @param graph       the training graph.
     * @param orientation direction of the paths for computing the distance.
     */
    public ShortestDistance(FastGraph<U> graph, EdgeOrientation orientation)
    {
        super(graph);
        PairMetric<U> pairMetric = new Distance<>();
        Map<Pair<U>, Double> values;
        if(orientation != EdgeOrientation.UND || !graph.isDirected())
        {
            values = pairMetric.compute(graph);
        }
        else
        {
            Graph<U> aux = Adapters.undirected(graph);
            values = pairMetric.compute(aux);
        }
        
        this.orientation = orientation;
        this.distances = new SparseDoubleMatrix2D(uIndex.numUsers(), uIndex.numUsers());
        values.forEach((key, value) ->
        {
            int uIdx = uIndex.user2uidx(key.v1());
            int vIdx = uIndex.user2uidx(key.v2());
            distances.setQuick(uIdx, vIdx, value);
        });
    }
    
    /**
     * Constructor.
     * @param graph       the training graph.
     * @param orientation direction of the paths for computing the distance.
     * @param distances   distance map.
     */
    public ShortestDistance(FastGraph<U> graph, EdgeOrientation orientation, DoubleMatrix2D distances)
    {
        super(graph);
        this.distances = distances;
        this.orientation = orientation;
            
    }
    
    @Override
    public Int2DoubleMap getScoresMap(int i) 
    {
        Random r = new Random(0);
        Int2DoubleMap scores = new Int2DoubleOpenHashMap();
        for(int j = 0; j < iIndex.numItems(); ++j)
        {
            if(!this.orientation.equals(EdgeOrientation.IN))
                scores.put(j, -distances.get(i,j) - r.nextDouble());
            else
                scores.put(j, -distances.get(j,i) - r.nextDouble());
        }
        return scores;
    }
}
