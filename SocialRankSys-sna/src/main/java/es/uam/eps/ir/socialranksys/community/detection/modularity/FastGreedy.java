/* 
 * Copyright (C) 2018 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.community.detection.modularity;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import org.jooq.lambda.tuple.Tuple3;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Fast Greedy algorithm for optimizing modularity
 * 
 * M.E.J. Newman. Fast Algorithm for detecting community structure in networks. Physical Review E 69(6): 066133 (2004)

 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class FastGreedy<U> extends AbstractFastGreedy<U>
{
    /**
     * Finds the optimal pair of communities to merge. It just optimizes the modularity of the network.
     * @param graph The original graph.
     * @param comm The communities.
     * @return A triple containing: a) the first comm. to merge, b) the second one, c) the mod. increment.
     */
    @Override
    protected Tuple3<Integer, Integer, Double> findOptimalJoint(Graph<U> graph, Communities<U> comm)
    {
        int numComm = comm.getNumCommunities();
        
        CompleteCommunityGraphGenerator<U> commGraphGen = new CompleteCommunityGraphGenerator<>();
        MultiGraph<Integer> commGraph = commGraphGen.generate(graph, comm);
        
        Map<Integer, Double> edgesFromComm = new HashMap<>();
        Map<Integer, Double> edgesToComm = new HashMap<>();
        
        double maxDeltaQ = Double.NEGATIVE_INFINITY;
        Pair<Integer> maxPair = null;
        for(int i = 0; i < numComm && graph.getEdgeCount() > 0; ++i)
        {
            double edgesFromI = commGraph.getAdjacentEdgesCount(i) / (graph.getEdgeCount() + 0.0);
            edgesFromComm.put(i, edgesFromI);
            double edgesToI = commGraph.getIncidentEdgesCount(i) / (graph.getEdgeCount() + 0.0);
            edgesToComm.put(i, edgesToI);
            
            for(int j = 0; j < i; ++j)
            {
                double edgesFromJ = edgesFromComm.get(j);
                double edgesToJ = edgesToComm.get(j);
                double edgesIJ = 0.0;
                double edgesJI = 0.0;
                
                if(commGraph.containsEdge(i, j))
                {
                    edgesIJ = commGraph.getEdgeWeights(i,j).size() / (graph.getEdgeCount() + 0.0);
                }
                if(commGraph.containsEdge(j, i))
                {
                    edgesJI = commGraph.getEdgeWeights(j,i).size() / (graph.getEdgeCount() + 0.0);
                }
                
                double deltaQ = edgesIJ + edgesJI - edgesFromI*edgesToJ - edgesFromJ*edgesToI;
                if(deltaQ > maxDeltaQ)
                {
                    maxDeltaQ = deltaQ;
                    maxPair = new Pair<>(i, j);
                }
            }
        }
        
        // If all increments are equal to -Inf, choose a pair at random.
        if(maxPair == null)
        {
            Random rnd = new Random();
            int first = rnd.nextInt(numComm);
            int second = first;
            while(second == first)
            {
                second = rnd.nextInt(numComm);
            }
            maxPair = new Pair<>(first, second);
        }
        
        return new Tuple3<>(maxPair.v1(),maxPair.v2(), maxDeltaQ);
    }
}
