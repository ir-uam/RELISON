/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.global.swap.graph;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.sonalire.metrics.graph.ClusteringCoefficient;

import java.util.function.Supplier;


/**
 * Swap reranker that promotes the global clustering coefficient of the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 *
 * @see ClusteringCoefficient
 */
public class ClusteringCoefficientReranker<U> extends AbstractClusteringCoefficientReranker<U>
{

    /**
     * Constructor
     * @param lambda    trade-off between the original and novelty score (clustering coefficient)
     * @param cutoff    maximum length of the recommendation ranking
     * @param norm      the normalization scheme.
     * @param graph     the original graph.
     */
    public ClusteringCoefficientReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph)
    {
        super(lambda, cutoff, norm, graph, true);
    }

}
