/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.edge;


import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Class that tries to maximize the average embededness of the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class EmbedednessReranker<U> extends AbstractNeighborOverlapReranker<U>
{
    /**
     * Constructor
     * @param cutOff maximum number of edges to consider
     * @param lambda trade-off between the average embeddedness and the original score
     * @param norm indicates if the elements have to be normalized
     * @param rank indicates if the normalization is done by ranking (true) or by score (false)
     * @param graph the original graph
     */
    public EmbedednessReranker(double lambda, int cutOff, boolean norm, boolean rank, Graph<U> graph)
    {
        super(lambda, cutOff, norm, rank, graph, true);
    }

}
