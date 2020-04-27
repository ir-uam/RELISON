/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index.weighting;

/**
 * According to the different possible IR models, each term has a different weight. This interface allows
 * retrieving objects from inverted indexes using different weighting schemes.
 * @author Javier Sanz-Cruzado Puig
 */
public interface WeightingScheme 
{
    /**
     * Computes the value of a term in a document.
     * @param tf Term frequency
     * @param df Document frequency
     * @param docLength Document length
     * @param avgDocLength Average document length
     * @param numDocs Number of documents
     * @return the value
     */
    double computeWeight(double tf, double df, double docLength, double avgDocLength, double numDocs);
}
