/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.global.swap.comm.gini.edge.sizenormalized;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;

import java.util.function.Supplier;

/**
 * Swap reranker for promoting the balance in the degree distribution for the different
 * communities. Both edges between different communities and links inside of communities are considered,
 * but links between communities are all stored in a single group when computing the Gini
 * coefficient.
 *
 * The number of edges between pairs of communities are normalized by the maximum possible number of edges
 * between each pair of communities.
 *
 * The reranker does not consider improvements of the metric when the candidate user belongs
 * to the same community as the target user.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 */
public class OuterSizeNormalizedSemiCompleteCommunityEdgeGiniComplement<U> extends SizeNormalizedSemiCompleteCommunityEdgeGiniComplement<U>
{
    /**
     * Constructor.
     * @param lambda        establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff        the maximum number of contacts in the definitive rankings.
     * @param norm          the normalization scheme.
     * @param graph         the original graph.
     * @param communities   the relation between communities and users in the graph.
     * @param selfloops     true if self-loops are allowed, false if they are not.
     */
    public OuterSizeNormalizedSemiCompleteCommunityEdgeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean selfloops)
    {
        super(lambda, cutoff, norm, graph, communities, selfloops, true);
    }
}
