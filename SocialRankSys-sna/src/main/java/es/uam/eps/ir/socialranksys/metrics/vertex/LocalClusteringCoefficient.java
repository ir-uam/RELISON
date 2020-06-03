/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.vertex;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes the local clustering coefficient of a node.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class LocalClusteringCoefficient<U> implements VertexMetric<U>
{
    /**
     * Selection of the first neighborhood of the target user.
     */
    private final EdgeOrientation vSel;
    /**
     * Selection of the second neighborhood of the target user.
     */
    private final EdgeOrientation wSel;
    
    /**
     * Constructor.
     * @param vSel Selection of the first neighborhood of the target user.
     * @param wSel Selection of the second neighborhood of the target user.
     */
    public LocalClusteringCoefficient(EdgeOrientation vSel, EdgeOrientation wSel)
    {
        this.vSel = vSel;
        this.wSel = wSel;
    }
    
    /**
     * Default constructor. Relates the incoming neighbourhood and the outgoing one.
     */
    public LocalClusteringCoefficient()
    {
        this(EdgeOrientation.IN, EdgeOrientation.OUT);
    }
    
    @Override
    public double compute(Graph<U> graph, U user)
    {
        Set<U> vSelSet = graph.getNeighbourhood(user, this.vSel).collect(Collectors.toCollection(HashSet::new));
        Set<U> wSelSet = graph.getNeighbourhood(user, this.wSel).collect(Collectors.toCollection(HashSet::new));
        
        vSelSet.retainAll(wSelSet);
        int intersize = vSelSet.size();

        long vSelSize = graph.getNeighbourhoodSize(user, this.vSel);
        long wSelSize = graph.getNeighbourhoodSize(user, this.wSel);
        
        if(vSelSize == 0 || wSelSize == 0)
            return 0.0;
        
        Long counter = graph.getNeighbourhood(user, this.vSel).map(v -> graph.getNeighbourhood(user, wSel).filter(w -> graph.containsEdge(v, w)).count()).reduce(0L, Long::sum);
        
        if((vSelSize*wSelSize - intersize) == 0) return 0.0;
        
        return (counter + 0.0)/(graph.getNeighbourhoodSize(user, vSel)*graph.getNeighbourhoodSize(user,wSel) - intersize + 0.0);
    }
}
