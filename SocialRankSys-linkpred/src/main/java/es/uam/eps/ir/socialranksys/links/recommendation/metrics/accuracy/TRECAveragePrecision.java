/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.metrics.accuracy;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.metrics.AbstractRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.rel.IdealRelevanceModel;
import org.ranksys.core.util.tuples.Tuple2od;

/**
 * Average Precision: average of the precision at each recall level.
 *
 * @author Saúl Vargas (saul.vargas@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the items
 */
public class TRECAveragePrecision<U, I> extends AbstractRecommendationMetric<U, I>
{
    /**
     * An ideal relevance model.
     */
    private final IdealRelevanceModel<U, I> relModel;
    /**
     * The cutoff of the metric.
     */
    private final int cutoff;

    /**
     * Constructor.
     *
     * @param cutoff            cutoff of the metric
     * @param relevanceModel    relevance model
     */
    public TRECAveragePrecision(int cutoff, IdealRelevanceModel<U, I> relevanceModel)
    {
        super();
        this.relModel = relevanceModel;
        this.cutoff = cutoff;
    }

    /**
     * Returns a score for the recommendation list.
     *
     * @param recommendation recommendation list
     * @return score of the metric to the recommendation
     */
    @Override
    public double evaluate(Recommendation<U, I> recommendation)
    {
        IdealRelevanceModel.UserIdealRelevanceModel<U, I> userRelModel = relModel.getModel(recommendation.getUser());

        double ap = 0;
        int relCount = 0;
        int rank = 0;

        for (Tuple2od<I> pair : recommendation.getItems())
        {
            rank++;
            if (userRelModel.isRelevant(pair.v1))
            {
                relCount++;
                ap += relCount / (double) rank;
            }
            if (rank == cutoff)
            {
                break;
            }
        }

        if(userRelModel.getRelevantItems().isEmpty()) return 0.0;
        return ap / (double) userRelModel.getRelevantItems().size();
    }
}