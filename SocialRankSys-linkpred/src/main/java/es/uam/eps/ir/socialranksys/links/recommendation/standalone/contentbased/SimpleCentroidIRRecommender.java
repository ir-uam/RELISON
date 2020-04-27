/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.standalone.contentbased;

import es.uam.eps.ir.socialranksys.content.index.ContentIndex;
import es.uam.eps.ir.socialranksys.content.index.weighting.WeightingScheme;
import es.uam.eps.ir.socialranksys.content.similarities.ContentSimilarity;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;

/**
 * Content-based contact recommendation algorithm, based on a TF-IDF scheme.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the content identifiers.
 */
public class SimpleCentroidIRRecommender<U,I> extends CentroidIRRecommender<U,I> 
{
    /**
     * Constructor. Takes some of the neighborhoods
     * @param graph Graph.
     * @param cIndex Content index.
     * @param weighting Weighting scheme
     * @param sim Similarity function for comparing items.
     */
    public SimpleCentroidIRRecommender(FastGraph<U> graph, ContentIndex<U,I> cIndex, WeightingScheme weighting, ContentSimilarity sim)
    {
        super(graph, cIndex, weighting, sim);
    }
    
    
}
