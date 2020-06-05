/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.knn.similarities.ir;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;

/**
 * Similarity based on the Extreme BM25 method from Information Retrieval.
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 */
public class ExtremeBM25Similarity extends BM25Similarity
{
    /**
     * Constructor.
     * @param graph the training graph.
     * @param uSel neighborhood selection for the target user.
     * @param vSel neighborhood selection for the candidate user.
     * @param dlSel neighborhood selection for the document length
     * @param b parameter that tunes the effect of the document length.
     */
    public ExtremeBM25Similarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel, EdgeOrientation dlSel, double b)
    {
        super(graph, uSel, vSel, dlSel, b, Double.POSITIVE_INFINITY);
    }
}
