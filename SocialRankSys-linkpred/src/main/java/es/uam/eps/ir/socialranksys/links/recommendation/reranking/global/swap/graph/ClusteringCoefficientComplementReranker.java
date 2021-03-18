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
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;

import java.util.function.Supplier;

/**
 * Swap reranker that demotes the global clustering coefficient of the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 *
 * @see es.uam.eps.ir.socialranksys.metrics.graph.ClusteringCoefficient
 */
public class ClusteringCoefficientComplementReranker<U> extends AbstractClusteringCoefficientReranker<U>
{

    /**
     * Constructor
     * @param lambda    trade-off between the original and novelty score (clustering coefficient)
     * @param cutoff    maximum length of the recommendation ranking
     * @param norm      the normalization scheme.
     * @param graph     the original graph.
     */
    public ClusteringCoefficientComplementReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph)
    {
        super(lambda, cutoff, norm, graph, false);
    }

    
}
