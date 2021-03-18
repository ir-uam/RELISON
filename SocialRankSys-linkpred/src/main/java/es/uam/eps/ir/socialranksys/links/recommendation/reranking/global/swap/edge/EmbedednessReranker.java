/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.edge;


import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;

import java.util.function.Supplier;

/**
 * Swap reranker for optimizing the average embeddedness of the graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class EmbedednessReranker<U> extends AbstractNeighborOverlapReranker<U>
{
    /**
     * Constructor
     * @param cutOff    the maximum length of the definitive recommendation rankings.
     * @param lambda    trade-off between the average embeddedness and the original score.
     * @param norm      the normalization scheme.
     * @param graph     the original graph
     */
    public EmbedednessReranker(double lambda, int cutOff, Supplier<Normalizer<U>> norm, Graph<U> graph)
    {
        super(lambda, cutOff, norm, graph, true);
    }

}
