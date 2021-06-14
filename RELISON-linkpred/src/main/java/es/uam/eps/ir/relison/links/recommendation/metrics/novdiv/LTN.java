/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.metrics.novdiv;

import es.uam.eps.ir.ranksys.novelty.longtail.PCItemNovelty;

/**
 * Global version of the long tail novelty.
 *
 * <br><b>References:</b>
 * <ol>
 *  <li>S. Vargas. Novelty and diversity evaluation and enhancement in Recommender
 * Systems. PhD Thesis (2015)</li>
 *  <li>S. Vargas and P. Castells. Rank and relevance in novelty and diversity for
 *  * Recommender Systems. RecSys 2011.</li>
 *  <li>J. Sanz-Cruzado and P. Castells. Beyond Accuracy in Link Prediction. SoMePeaS 2019.</li>
 * </ol>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 */
public class LTN<U,I> extends ItemNoveltyMetric<U,I>
{
    /**
     * Constructor.
     * @param cutoff    maximum length of recommendation lists to evaluate.
     * @param distModel item distance model.
     */
    public LTN(int cutoff, PCItemNovelty<U,I> distModel)
    {
        super(cutoff, distModel);
    }
}
