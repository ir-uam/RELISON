/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.globalranking.communities;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Implementation of a reranker which promotes the modularity complement of the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class ModularityComplement<U> extends CompleteCommunityReranker<U>
{
    /**
    * The number of edges of the user graph.
    */
    protected long numEdges;
    /**
     * The relation between the users and their in-degree.
     */
    protected Map<U, Integer> inDegree;
    /**
     * The relation between the users and their out-degree.
     */
    protected Map<U, Integer> outDegree;
    /**
     * K(G,C) = \sum_i,j |\Gamma_out(i)||\Gamma_in(j)|\delta(c_i, c_j)
     */
    protected double sum;

    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param communities   the relation between users and communities.
     */
    public ModularityComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities)
    {
        super(lambda, cutoff, norm, graph, communities, false);
        this.sum = 0.0;
        this.inDegree = new Object2IntOpenHashMap<>();
        this.outDegree = new Object2IntOpenHashMap<>();
        this.numEdges = graph.getEdgeCount();

        // Get the degrees for the different nodes in the graph.
        if(graph.isDirected())
        {
            DirectedGraph<U> directedGraph = (DirectedGraph<U>) graph;
            graph.getAllNodes().forEach((node) ->
            {
                inDegree.put(node, directedGraph.inDegree(node));
                outDegree.put(node, directedGraph.outDegree(node));
            });
        }
        else
        {
            graph.getAllNodes().forEach((node) ->
            {
                inDegree.put(node, graph.degree(node));
                outDegree.put(node, graph.degree(node));
            });
        }

        // Compute \sum_i,j |\Gamma_out(i)||\Gamma_in(j)|\delta(c_i, c_j)
        this.sum = graph.getAllNodes().map(orig ->
        {
            double userSum = 0.0;
            int commOrig = communities.getCommunity(orig);
            userSum = graph.getAllNodes().map(dest -> 
            {
                double value = 0.0;
                int commDest = communities.getCommunity(dest);
                if(commOrig == commDest)
                {
                    value = this.outDegree.get(orig)*this.inDegree.get(dest) + 0.0;
                }
                return value;
            }).reduce(userSum, Double::sum);
            return userSum;
        }).reduce(0.0, Double::sum);
    }

    @Override
    protected double nov(U user, Tuple2od<U> item)
    {
        U recomm = item.v1;
        int userComm = communities.getCommunity(user);
        int recommComm = communities.getCommunity(recomm);
        double add = ((userComm == recommComm)? 1.0 : 0.0);

        // Computes the updated \sum_i,j |\Gamma_out(i)||\Gamma_in(j)| \delta(c_i, c_j) = K(G \cup (u,v), C)
        double k = sum + 
        communities.getUsers(userComm).map(node -> inDegree.get(node) + 0.0).reduce(0.0, Double::sum) +
        communities.getUsers(recommComm).map(node -> outDegree.get(node) + 0.0).reduce(0.0, Double::sum) +
        add;

        // Computes \sum A_ij \delta(c_i,c_j)
        double numer = this.numEdges - this.communityGraph.getEdgeCount() + add;
        double updatedNumEdges = this.numEdges + 1;
        if(communityGraph.isDirected())
        {
            numer *= 2;
            updatedNumEdges *= 2;
        }

        // returns 1 - modularity (the ideal value for the modularity is 1, so 
        return (1.0 - (numer - k/updatedNumEdges)/(updatedNumEdges - k/updatedNumEdges))/2.0;
    }

    @Override
    protected void update(U user, Tuple2od<U> selectedItem)
    {
        U recomm = selectedItem.v1;

        int userComm = communities.getCommunity(user);
        int recommComm = communities.getCommunity(recomm);

        // If the recommended user and the new one belong to the same community, then, 1. If not, 0.
        double add = ((userComm == recommComm)? 1.0 : 0.0);

        // Computes the updated \sum_i,j |\Gamma_out(i)||\Gamma_in(j)| \delta(c_i, c_j) = K(G \cup (u,v), C)
        sum +=
            communities.getUsers(userComm).map(node -> inDegree.get(node) + 0.0).reduce(0.0, Double::sum) +
            communities.getUsers(recommComm).map(node -> outDegree.get(node) + 0.0).reduce(0.0, Double::sum) +
            add;

        // Compute the new number of edges, and add the edge to the community graph.
        this.numEdges++;
        if(userComm == recommComm)
            this.communityGraph.addEdge(userComm, recommComm);

        // Update the new degrees (only for the new recommendation)
        this.outDegree.put(user, this.outDegree.get(user)+1);
        this.inDegree.put(recomm, this.inDegree.get(recomm)+1);
        if(this.communityGraph.isDirected())
        {
            this.outDegree.put(recomm, this.outDegree.get(recomm) + 1);
            this.inDegree.put(user, this.inDegree.get(user) + 1);
        }
    }
}
