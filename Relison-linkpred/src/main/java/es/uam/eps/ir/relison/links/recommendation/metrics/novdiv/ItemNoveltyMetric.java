/* 
 * Copyright (C) 2015 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.metrics.novdiv;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.metrics.AbstractSystemMetric;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.novdiv.itemnovelty.ItemNovelty;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;

/**
 * Item novelty metric.
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
 * @author Saúl Vargas (saul.vargas@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * 
 * @param <U> type of the users
 * @param <I> type of the items
 */
public abstract class ItemNoveltyMetric<U, I> extends AbstractSystemMetric<U, I>
{
    /**
     * The cutoff for each recommendation.
     */
    private final int cutoff;
    /**
     * The list of recommendations.
     */
    private List<Recommendation<U,I>> list = new ArrayList<>();

    /**
     * item novelty model.
     */
    protected final ItemNovelty<U, I> novelty;

    /**
     * Constructor.
     *
     * @param cutoff maximum size of the recommendation list that is evaluated
     * @param novelty novelty model
     */
    public ItemNoveltyMetric(int cutoff, ItemNovelty<U, I> novelty) {
        super();
        this.cutoff = cutoff;
        this.novelty = novelty;
    }

    @Override
    public double evaluate()
    {
        double nov = 0.0;
        double counter = 0.0;

        for(Recommendation<U,I> rec : list)
        {
            List<Tuple2od<I>> items = rec.getItems();
            int n = Math.min(cutoff, items.size());
            counter += n;

            ItemNovelty.UserItemNoveltyModel<U,I> userModel = novelty.getModel(rec.getUser());

            for(int i = 0; i < n; ++i)
            {
                nov += userModel.novelty(items.get(i).v1());
            }
        }

        return (counter > 0.0) ? nov/counter : 0.0;
    }

    @Override
    public void add(Recommendation<U, I> recommendation)
    {
        this.list.add(recommendation);
    }

    @Override
    public void combine(SystemMetric<U, I> systemMetric)
    {
        ItemNoveltyMetric<U,I> other = (ItemNoveltyMetric<U,I>) systemMetric;
        for(Recommendation<U,I> rec : other.list)
        {
            this.add(rec);
        }
    }

    @Override
    public void reset()
    {
        this.list.clear();
    }
}
