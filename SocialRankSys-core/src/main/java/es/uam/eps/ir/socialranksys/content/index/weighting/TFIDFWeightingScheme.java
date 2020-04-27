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
 * Weighting scheme that applies the classical TF-IDF model
 * @author Javier Sanz-Cruzado Puig
 */
public class TFIDFWeightingScheme implements WeightingScheme
{
    @Override
    public double computeWeight(double tf, double df, double docLength, double avgDocLength, double numDocs) 
    {
        double auxTF = 0.0;
        double auxIDF = 0.0;
        if(tf > 0.0)
        {
            auxTF = Math.log(1 + tf)/Math.log(2.0);
        }
        if(df > 0.0)
        {
           auxIDF = Math.log(numDocs/df)/Math.log(2.0);
        }
        return auxTF*auxIDF;
    }
    
}
