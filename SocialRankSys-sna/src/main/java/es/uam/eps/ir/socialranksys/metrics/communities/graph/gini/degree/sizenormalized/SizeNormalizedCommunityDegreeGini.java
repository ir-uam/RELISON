/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.communities.graph.gini.degree.sizenormalized;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.UndirectedGraph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.multigraph.DirectedMultiGraph;
import es.uam.eps.ir.socialranksys.graph.multigraph.UndirectedMultiGraph;
import es.uam.eps.ir.socialranksys.metrics.CommunityMetric;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;

import java.util.stream.Stream;

/**
 * Computes the community degree Gini of the graph, i.e. the Gini coefficient for the
 * degree distribution of the communities in the graph.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class SizeNormalizedCommunityDegreeGini<U> implements CommunityMetric<U>
{
    /**
     * The degree to take in the community graph.
     */
    private final EdgeOrientation orientation;
    /**
     * The community graph generator.
     */
    private final CommunityGraphGenerator<U> cgg;


    /**
     * Constructor.
     *
     * @param orientation Orientation of the edges.
     * @param cgg         Community graph generator.
     */
    public SizeNormalizedCommunityDegreeGini(EdgeOrientation orientation, CommunityGraphGenerator<U> cgg)
    {
        this.orientation = orientation;
        this.cgg = cgg;
    }

    @Override
    public double compute(Graph<U> graph, Communities<U> comm)
    {
        Graph<Integer> commGraph = cgg.generate(graph, comm);


        if (comm.getNumCommunities() < 1.0)
        {
            return Double.NaN;
        }
        if (commGraph.getEdgeCount() == 0) // every node has equal degree
        {
            return 1.0;
        }
        if (graph.isDirected())
        {
            return computeDirected((DirectedGraph<U>) graph, (DirectedMultiGraph<Integer>) commGraph, comm);
        }
        return computeUndirected((UndirectedGraph<U>) graph, (UndirectedMultiGraph<Integer>) commGraph, comm);
    }

    /**
     * Obtains the denominator for a certain community.
     *
     * @param graph the original graph.
     * @param comm  the communities.
     * @param c     the community.
     *
     * @return the denominator.
     */
    protected abstract double getDenom(Graph<U> graph, Communities<U> comm, int c);

    /**
     * Gets the selected orientation for the metric.
     *
     * @return the selected orientation.
     */
    protected EdgeOrientation getOrientation()
    {
        return this.orientation;
    }

    /**
     * Compute the index for directed graphs.
     *
     * @param graph     the directed graph.
     * @param commGraph the directed community graph.
     * @param comm      the communities.
     *
     * @return the value of the metric.
     */
    private double computeDirected(DirectedGraph<U> graph, DirectedMultiGraph<Integer> commGraph, Communities<U> comm)
    {
        Stream<Double> degrees;
        if (this.orientation.equals(EdgeOrientation.IN))
        {
            degrees = comm.getCommunities().mapToDouble(c -> (commGraph.inDegree(c) + 0.0) / (this.getDenom(graph, comm, c))).boxed();
        }
        else if (this.orientation.equals(EdgeOrientation.OUT))
        {
            degrees = comm.getCommunities().mapToDouble(c -> (commGraph.outDegree(c) + 0.0) / (this.getDenom(graph, comm, c))).boxed();
        }
        else
        {
            degrees = comm.getCommunities().mapToDouble(c -> (commGraph.inDegree(c) + commGraph.outDegree(c) + 0.0) / (this.getDenom(graph, comm, c))).boxed();
        }
        GiniIndex gini = new GiniIndex();
        return 1.0 - gini.compute(degrees, true);
    }

    /**
     * Compute the index for undirected graphs.
     *
     * @param graph     the undirected graph.
     * @param commGraph the undirected community graph.
     * @param comm      the communities.
     *
     * @return the value of the metric.
     */
    private double computeUndirected(UndirectedGraph<U> graph, UndirectedMultiGraph<Integer> commGraph, Communities<U> comm)
    {
        Stream<Double> degrees = comm.getCommunities().mapToDouble(c -> (commGraph.degree(c) + 0.0) / (this.getDenom(graph, comm, c))).boxed();
        GiniIndex gini = new GiniIndex();
        return 1.0 - gini.compute(degrees, true);
    }
}
