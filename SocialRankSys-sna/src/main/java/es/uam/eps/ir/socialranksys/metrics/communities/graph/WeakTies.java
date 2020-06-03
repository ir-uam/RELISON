/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.communities.graph;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.InterCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.metrics.CommunityMetric;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the number of edges between communities
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * 
 * De Meo et al. On Facebook, most ties are weak. Communications of the ACM 57(11), pp. 78-84 (2012)
 */
public class WeakTies<U> implements CommunityMetric<U>
{
    @Override
    public double compute(Graph<U> graph, Communities<U> comm) {
        CommunityGraphGenerator<U> cgg = new InterCommunityGraphGenerator<>();
        MultiGraph<Integer> commGraph = cgg.generate(graph, comm);
        List<Double> degrees = new ArrayList<>();

        commGraph.getAllNodes().forEach((orig)-> commGraph.getAllNodes().forEach((dest)->
        {
           if(!orig.equals(dest))
           {
               degrees.add(commGraph.getNumEdges(orig, dest)+0.0);
           }
        }));

        return degrees.stream().mapToDouble(u -> u).sum();
    }
    
}
