/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.standalone.distance;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.GraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.pair.Distance;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.openide.util.Exceptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Recommends users by computing the distance between two of them
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
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
     * @param graph Graph 
     * @param dir Direction of the paths to take.
     */
    public ShortestDistance(FastGraph<U> graph, EdgeOrientation dir)
    {
        super(graph);
        PairMetric<U> pairMetric = new Distance<>();
        Map<Pair<U>, Double> values = new HashMap<>();
        if(dir != EdgeOrientation.UND || !graph.isDirected())
        {
            values = pairMetric.compute(graph);
        }
        else
        {
            try
            {
                GraphGenerator<U> graphGen = new EmptyGraphGenerator<>();
                graphGen.configure(true, graph.isWeighted());
                Graph<U> aux = graphGen.generate();
                
                graph.getAllNodes().forEach(aux::addNode);
                
                graph.getAllNodes().forEach(u -> graph.getAdjacentNodes(u).forEach(v ->
                {
                    aux.addEdge(u, v, 1.0);
                    aux.addEdge(v, u, 1.0);
                }));
                
                values = pairMetric.compute(aux);
            } catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex) {
                Exceptions.printStackTrace(ex);
            }
            
        }
        
        this.orientation = dir;
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
     * @param graph Graph
     * @param orientation Direction of the paths for computing the distance
     * @param distances Distance map
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
        Random r = new Random();
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
