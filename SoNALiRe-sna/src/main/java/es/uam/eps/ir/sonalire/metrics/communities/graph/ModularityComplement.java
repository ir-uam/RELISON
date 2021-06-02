/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.communities.graph;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.DirectedGraph;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.UndirectedGraph;
import es.uam.eps.ir.sonalire.graph.multigraph.MultiGraph;
import es.uam.eps.ir.sonalire.metrics.CommunityMetric;

/**
 * Computes the modularity complement of a graph, given the communities.
 * <p>
 * <b>References: </b></p>
 *     <ol>
 *         <li>J. Sanz-Cruzado, P. Castells. Beyond accuracy in link prediction. 3rd Workshop on Social Media for Personalization and Search (SoMePEaS 2019).</li>
 *         <li>J. Sanz-Cruzado, S.M. Pepa, P. Castells. Structural novelty and diversity in link prediction. 0th International Workshop on Modeling Social Media (MSM 2018) at The Web Conference (WWW 2018)</li>
 *     </ol>
 *
 *
 * @param <U> The type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ModularityComplement<U> implements CommunityMetric<U>
{
    /**
     * Modularity calculator.
     */
    private final Modularity<U> modularity;

    /**
     * Constructor.
     */
    public ModularityComplement()
    {
        this.modularity = new Modularity<>();
    }

    @Override
    public double compute(Graph<U> graph, Communities<U> comm)
    {
        double mod = modularity.compute(graph, comm);
        return (1.0 - mod) / 2.0;
    }

    /**
     * Computes the value of the modularity for a directed graph.
     *
     * @param graph     The directed graph.
     * @param comm      The communities.
     * @param commGraph The community graph.
     *
     * @return The value of the modularity.
     */
    private double computeDirected(DirectedGraph<U> graph, Communities<U> comm, MultiGraph<Integer> commGraph)
    {
        double modularity;
        // Compute \sum_i,j |\Gamma_out(i)||\Gamma_in(j)|\delta(c_i, c_j)
        double k = graph.getAllNodes().map(orig ->
        {
            double userSum = 0.0;
            int commOrig = comm.getCommunity(orig);
            userSum = graph.getAllNodes().map(dest ->
            {
                double value = 0.0;
                int commDest = comm.getCommunity(dest);
                if (commOrig == commDest)
                {
                    value = graph.outDegree(orig) * graph.inDegree(dest) + 0.0;
                }
                return value;
            }).reduce(userSum, Double::sum);
            return userSum;
        }).reduce(0.0, Double::sum);

        long numEdges = graph.getEdgeCount();
        modularity = numEdges - commGraph.getEdgeCount();
        modularity -= k / (numEdges + 0.0);
        modularity /= (numEdges - k / (numEdges + 0.0) + 0.0);
        return modularity;
    }

    /**
     * Computes the value of the modularity for an undirected graph.
     *
     * @param graph     The undirected graph.
     * @param comm      The communities.
     * @param commGraph The community graph.
     *
     * @return The value of the modularity of the graph.
     */
    private double computeUndirected(UndirectedGraph<U> graph, Communities<U> comm, MultiGraph<Integer> commGraph)
    {
        double modularity;
        // Compute \sum_i,j |\Gamma_out(i)||\Gamma_in(j)|\delta(c_i, c_j)
        double k = graph.getAllNodes().map(orig ->
        {
            double userSum = 0.0;
            int commOrig = comm.getCommunity(orig);
            userSum = graph.getAllNodes().map(dest ->
            {
                double value = 0.0;
                int commDest = comm.getCommunity(dest);
                if (commOrig == commDest)
                {
                    value = graph.degree(orig) * graph.degree(dest) + 0.0;
                }
                return value;
            }).reduce(userSum, Double::sum);
            return userSum;
        }).reduce(0.0, Double::sum);

        long numEdges = 2 * graph.getEdgeCount();
        modularity = numEdges - commGraph.getEdgeCount();
        modularity -= k / (numEdges + 0.0);
        modularity /= (numEdges - k / (numEdges + 0.0) + 0.0);
        return modularity;
    }
}
