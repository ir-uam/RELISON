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
 * Interface for computing the similarity between two different text contents.
 * @author Javier Sanz-Cruzado Puig
 */
public interface ContentSimilarity
{
    /**
     * Computes the similarity between two different contents.
     * @param c1 A text vector representing the first content.
     * @param c2 A text vector representing the second content.
     * @return the similarity value.
     */
    double similarity(TextVector c1, TextVector c2);
}
