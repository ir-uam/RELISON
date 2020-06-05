/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.graph;

import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Reranker that tries to promote the opposite of the clustering coefficient of
 * the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class ClusteringCoefficientComplementReranker<U> extends AbstractClusteringCoefficientReranker<U>
{

    /**
     * Constructor
     * @param lambda Trade-off between the original and novelty score (clustering coefficient)
     * @param cutoff Maximum length of the recommendation ranking
     * @param norm true if the scores have to be normalized, false if not.
     * @param rank true if the normalization is by ranking position, false if it is by score
     * @param graph The original graph.
     */
    public ClusteringCoefficientComplementReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph)
    {
        super(lambda, cutoff, norm, rank, graph, false);
    }

    
}
