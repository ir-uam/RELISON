/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.metrics.novdiv;

import es.uam.eps.ir.ranksys.novelty.unexp.PDItemNovelty;

/**
 * Global version of the unexpectedness (expected profile distance).
 *
 * <p><b>References:</b>
 * <ol>
 *  <li>S. Vargas. Novelty and diversity evaluation and enhancement in Recommender
 * Systems. PhD Thesis (2015)</li>
 *  <li>S. Vargas and P. Castells. Rank and relevance in novelty and diversity for
 *  * Recommender Systems. RecSys 2011.</li>
 *  <li>J. Sanz-Cruzado and P. Castells. Beyond Accuracy in Link Prediction. SoMePeaS 2019.</li>
 * </ol></p>
 *
 * @param <U> type of the users.
 * @param <I> type of the items.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Unexpectedness<U,I> extends ItemNoveltyMetric<U,I>
{
    /**
     * Constructor.
     * @param cutoff    maximum length of recommendation lists to evaluate
     * @param distModel item distance model
     */
    public Unexpectedness(int cutoff, PDItemNovelty<U,I> distModel)
    {
        super(cutoff, distModel);
    }
}
