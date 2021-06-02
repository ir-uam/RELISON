/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.community.detection.modularity.balanced;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.community.detection.modularity.AbstractFastGreedy;
import es.uam.eps.ir.sonalire.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.multigraph.MultiGraph;
import es.uam.eps.ir.sonalire.utils.datatypes.Pair;
import org.jooq.lambda.tuple.Tuple3;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Alternative version of the Balanced Fast Greedy algorithm for optimizing modularity, and the size of communities, that
 * computes the whole dendogram for communities.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SizeWeightedFastGreedy<U> extends AbstractFastGreedy<U>
{
    @Override
    protected Tuple3<Integer, Integer, Double> findOptimalJoint(Graph<U> graph, Communities<U> comm)
    {
        int numComm = comm.getNumCommunities();

        CompleteCommunityGraphGenerator<U> commGraphGen = new CompleteCommunityGraphGenerator<>();
        MultiGraph<Integer> commGraph = commGraphGen.generate(graph, comm);

        Map<Integer, Double> edgesFromComm = new HashMap<>();
        Map<Integer, Double> edgesToComm = new HashMap<>();
        Map<Integer, Double> commSizes = new HashMap<>();

        double maxDeltaQ = Double.NEGATIVE_INFINITY;
        Pair<Integer> maxPair = null;

        double sum = 0.0;
        for (int i = 0; i < numComm; ++i)
        {
            double size = comm.getCommunitySize(i);
            commSizes.put(i, size);
            sum += size;
        }

        for (int i = 0; i < numComm && graph.getEdgeCount() > 0; ++i)
        {
            double sizeI = commSizes.get(i) + 0.0;
            commSizes.put(i, sizeI);
            double edgesFromI = commGraph.getAdjacentEdgesCount(i) / (graph.getEdgeCount() + 0.0);
            edgesFromComm.put(i, edgesFromI);
            double edgesToI = commGraph.getIncidentEdgesCount(i) / (graph.getEdgeCount() + 0.0);
            edgesToComm.put(i, edgesToI);

            for (int j = 0; j < i; ++j)
            {
                double sizeJ = commSizes.get(j) + 0.0;
                double edgesFromJ = edgesFromComm.get(j);
                double edgesToJ = edgesToComm.get(j);
                double edgesIJ = 0.0;
                double edgesJI = 0.0;

                if (commGraph.containsEdge(i, j))
                {
                    edgesIJ = commGraph.getEdgeWeights(i, j).size() / (graph.getEdgeCount() + 0.0);
                }
                if (commGraph.containsEdge(j, i))
                {
                    edgesJI = commGraph.getEdgeWeights(j, i).size() / (graph.getEdgeCount() + 0.0);
                }

                double deltaQ = edgesIJ + edgesJI - edgesFromI * edgesToJ - edgesFromJ * edgesToI;
                double weight = Math.abs(sizeI + sizeJ - sum / (numComm - 1.0)) - 0.5 * Math.abs(sizeI - sum / (numComm + 0.0)) - 0.5 * Math.abs(sizeJ - sum / (numComm + 0.0));
                weight /= (graph.getVertexCount() + 0.0);
                deltaQ -= weight;

                if (deltaQ > maxDeltaQ)
                {
                    maxDeltaQ = deltaQ;
                    maxPair = new Pair<>(i, j);
                }
            }
        }

        // If all increments are equal to -Inf, choose a pair at random.
        if (maxPair == null)
        {
            Random rnd = new Random();
            int first = rnd.nextInt(numComm);
            int second = first;
            while (second == first)
            {
                second = rnd.nextInt(numComm);
            }
            maxPair = new Pair<>(first, second);
        }

        return new Tuple3<>(maxPair.v1(), maxPair.v2(), maxDeltaQ);
    }
}
