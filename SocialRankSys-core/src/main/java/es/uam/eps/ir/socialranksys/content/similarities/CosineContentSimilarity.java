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
 * Cosine similarity.
 * @author Javier Sanz-Cruzado Puig
 */
public class CosineContentSimilarity implements ContentSimilarity 
{

    @Override
    public double similarity(TextVector c1, TextVector c2)
    {
        if(c1.getModule() == 0.0 || c2.getModule() == 0.0)
        {
            return 0.0;
        }
        else
        {
            return c1.scalarProduct(c2)/(c1.getModule()*c2.getModule());
        }
    }
    
}
