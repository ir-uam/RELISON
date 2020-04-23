/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.complementary;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.UndirectedGraph;
import es.uam.eps.ir.socialranksys.graph.UnweightedGraph;

/**
 * Undirected unweighted complementary graph
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the vertices.
 */
public class UndirectedUnweightedComplementaryGraph<U> extends ComplementaryGraph<U> implements UndirectedGraph<U>, UnweightedGraph<U>
{

    /**
     * Constructor.
     * @param graph Original graph. 
     */
    public UndirectedUnweightedComplementaryGraph(Graph<U> graph)
    {
        super(graph);
    }
    
}
