/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.communities.graph.gini.edges.sizenormalized;


import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityNoSelfLoopsGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.metrics.CommunityMetric;
import es.uam.eps.ir.socialranksys.metrics.graph.EdgeGiniMode;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes the size normalized community edge Gini of the graph, i.e. the Gini coefficient
 * for the number of edges in the graph, divided by the maximum number of possible links between
 * the endpoint communities.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class SizeNormalizedCommunityEdgeGini<U> implements CommunityMetric<U>
{
    /**
     * PairGini execution mode
     *
     * @see EdgeGiniMode
     */
    private final EdgeGiniMode mode;

    /**
     * Indicates if autoloops between nodes are allowed.
     */
    private final boolean selfloops;

    /**
     * Community graph generator.
     */
    private final CommunityGraphGenerator<U> cgg;

    /**
     * Constructor.
     *
     * @param mode      PairGini execution mode.
     * @param selfloops True if self-loops between the original network nodes are allowed, false if not.
     */
    public SizeNormalizedCommunityEdgeGini(EdgeGiniMode mode, boolean selfloops)
    {
        this.mode = mode;
        this.selfloops = selfloops;
        this.cgg = selfloops ? new CompleteCommunityGraphGenerator<>() : new CompleteCommunityNoSelfLoopsGraphGenerator<>();
    }

    @Override
    public double compute(Graph<U> graph, Communities<U> comm)
    {
        if (graph.isDirected())
        {
            return this.computeDirected(graph, comm);
        }
        else
        {
            return this.computeUndirected(graph, comm);
        }
    }

    /**
     * Computes the metric for directed graphs.
     *
     * @param graph The directed graph.
     * @param comm  the community partition of the graph.
     *
     * @return the value of the metric.
     */
    public double computeDirected(Graph<U> graph, Communities<U> comm)
    {
        MultiGraph<Integer> commGraph = cgg.generate(graph, comm);
        Map<Integer, Double> sizes = new HashMap<>();

        for (int i = 0; i < comm.getNumCommunities(); ++i)
        {
            sizes.put(i, comm.getCommunitySize(i) + 0.0);
        }

        List<Double> degrees = new ArrayList<>();

        double sum = 0.0;
        double sumAutoloops = 0.0;
        for (int i = 0; i < comm.getNumCommunities(); ++i)
        {
            for (int j = 0; j < comm.getNumCommunities(); ++j)
            {
                double numEdges = commGraph.getNumEdges(i, j);
                if (i == j && numEdges > 0.0)
                {
                    if (this.selfloops)
                    {
                        numEdges /= (sizes.get(i) * sizes.get(i));
                    }
                    else
                    {
                        numEdges /= (sizes.get(i) * (sizes.get(i) - 1));
                    }

                    sumAutoloops += numEdges;

                    if (this.mode.equals(EdgeGiniMode.COMPLETE))
                    {
                        degrees.add(numEdges);
                    }
                }
                else
                {
                    numEdges /= (sizes.get(i) * sizes.get(j));
                    degrees.add(numEdges);
                }


                sum += numEdges;
            }
        }

        if (this.mode.equals(EdgeGiniMode.SEMICOMPLETE))
        {
            degrees.add(sumAutoloops);
        }
        else if (this.mode.equals(EdgeGiniMode.INTERLINKS))
        {
            sum -= sumAutoloops;
        }

        GiniIndex gini = new GiniIndex();
        return 1.0 - gini.compute(degrees, true, degrees.size(), sum);
    }

    /**
     * Computes the metric for undirected graphs.
     *
     * @param graph The directed graph.
     * @param comm  the community partition of the graph.
     *
     * @return the value of the metric.
     */
    public double computeUndirected(Graph<U> graph, Communities<U> comm)
    {
        MultiGraph<Integer> commGraph = cgg.generate(graph, comm);
        Map<Integer, Double> sizes = new HashMap<>();

        for (int i = 0; i < comm.getNumCommunities(); ++i)
        {
            sizes.put(i, comm.getCommunitySize(i) + 0.0);
        }

        List<Double> degrees = new ArrayList<>();

        double sum = 0.0;
        double sumAutoloops = 0.0;
        for (int i = 0; i < comm.getNumCommunities(); ++i)
        {
            for (int j = 0; j <= i; ++j)
            {
                double numEdges = commGraph.getNumEdges(i, j);
                if (i == j && numEdges > 0.0)
                {
                    if (this.selfloops)
                    {
                        numEdges /= (sizes.get(i) * (sizes.get(i) + 1)) / 2;
                    }
                    else
                    {
                        numEdges /= (sizes.get(i) * (sizes.get(i) - 1)) / 2;
                    }

                    sumAutoloops += numEdges;

                    if (this.mode.equals(EdgeGiniMode.COMPLETE))
                    {
                        degrees.add(numEdges);
                    }
                }
                else
                {
                    numEdges /= (sizes.get(i) * sizes.get(j));
                }

                sum += numEdges;
            }
        }

        if (this.mode.equals(EdgeGiniMode.SEMICOMPLETE))
        {
            degrees.add(sumAutoloops);
        }
        else if (this.mode.equals(EdgeGiniMode.INTERLINKS))
        {
            sum -= sumAutoloops;
        }

        GiniIndex gini = new GiniIndex();
        return 1.0 - gini.compute(degrees, true, degrees.size(), sum);
    }
}
