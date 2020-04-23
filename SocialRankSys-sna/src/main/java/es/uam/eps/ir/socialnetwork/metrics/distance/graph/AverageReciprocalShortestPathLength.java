/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.distance.graph;

import es.uam.eps.ir.socialnetwork.metrics.GraphMetric;
import es.uam.eps.ir.socialnetwork.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Computes the Average Reciprocal Shortest path Length of graphs.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class AverageReciprocalShortestPathLength<U> implements GraphMetric<U>
{

    /**
     * Calculates the distances
     */
    private final DistanceCalculator<U> dc;
    
    /**
     * Constructor
     * @param dc distance calculator.
     */
    public AverageReciprocalShortestPathLength(DistanceCalculator<U> dc)
    {

        this.dc = dc;
    }
    
    /**
     * Default constructor.
     */
    public AverageReciprocalShortestPathLength()
    {
        this(new DistanceCalculator<>());
    }
        
    @Override
    public double compute(Graph<U> graph)
    {
        if(graph.getVertexCount() <= 1L || graph.getEdgeCount() == 0L)
            return 0.0;
        this.dc.computeDistances(graph);

        double sum = graph.getAllNodes().mapToDouble(u -> graph.getAllNodes().mapToDouble(v ->
        {
            double dist = dc.getDistances(u, v);
            if(u.equals(v)) return 0.0;
            else if(Double.isInfinite(dist)) return 0.0;
            else return 1/dist;
        }).sum()).sum();
        
        double numN = graph.getVertexCount() + 0.0;
        return sum/(numN*(numN-1));
    }
    
}
