/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.global.swap.edge;


import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;

import java.util.function.Supplier;

/**
 * Swap reranker for maximizing the average weakness of the graph, i.e. for minimizing the
 * average embeddedness of the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class WeaknessReranker<U> extends AbstractNeighborOverlapReranker<U>
{
    /**
     * Constructor
     * @param cutOff    maximum number of edges to consider
     * @param lambda    trade-off between the average embeddedness and the original score
     * @param norm      normalization strategy.
     * @param graph     the original graph
     */
    public WeaknessReranker(double lambda, int cutOff, Supplier<Normalizer<U>> norm, Graph<U> graph)
    {
        super(lambda, cutOff, norm, graph, false);
    }

}
