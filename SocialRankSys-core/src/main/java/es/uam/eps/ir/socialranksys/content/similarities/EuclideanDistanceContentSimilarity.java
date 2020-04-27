/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.similarities;


import es.uam.eps.ir.socialranksys.content.TextVector;

/**
 * Euclidean distance similarity.
 * @author Javier Sanz-Cruzado Puig
 */
public class EuclideanDistanceContentSimilarity implements ContentSimilarity 
{

    @Override
    public double similarity(TextVector c1, TextVector c2)
    {
        TextVector c3 = c2.multiply(-1);
        c3 = c1.sum(c3);
        return c3.getModule();
    }
    
}
