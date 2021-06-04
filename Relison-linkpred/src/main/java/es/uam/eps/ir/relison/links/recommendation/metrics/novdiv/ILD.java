/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.metrics.novdiv;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.novdiv.distance.ItemDistanceModel;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Global version of EILD.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the items.
 */
public class ILD<U,I> implements SystemMetric<U,I>
{
    /**
     * A list of recommendations.
     */
    private List<Recommendation<U,I>> list = new ArrayList<>();
    /**
     * The cutoff for each recommendation.
     */
    private final int cutoff;
    /**
     * A model for the distances between items.
     */
    private final ItemDistanceModel<I> distModel;

    /**
     * Constructor.
     * @param cutoff    maximum length of recommendation lists to evaluate
     * @param distModel item distance model
     */
    public ILD(int cutoff, ItemDistanceModel<I> distModel)
    {
        this.cutoff = cutoff;
        this.distModel = distModel;
    }

    @Override
    public void add(Recommendation<U, I> r)
    {
        this.list.add(r);
    }

    @Override
    public double evaluate()
    {
        double counter = 0.0;
        double ild = 0.0;

        for(Recommendation<U,I> rec : list)
        {
            List<Tuple2od<I>> items = rec.getItems();
            int n = Math.min(cutoff, items.size());
            counter += n;

            double auxIld = 0.0;

            for(int i = 0; i < n; ++i)
            {
                // We first find the distance between elements
                ToDoubleFunction<I> iDist = distModel.dist(items.get(i).v1());
                for(int j = 0; j < n; ++j)
                {
                    if(i == j) continue; // dist == 0
                    double dist = iDist.applyAsDouble(items.get(j).v1());
                    if(!Double.isNaN(dist))
                    {
                        auxIld += dist;
                    }
                }

            }

            if(n > 0) ild += auxIld / n;
        }

        return counter > 0 ? ild/counter : 0.0;
    }

    @Override
    public void combine(SystemMetric<U, I> sm)
    {
        ILD<U,I> otherM = (ILD<U,I>) sm;
        list.addAll(otherM.list);
    }

    @Override
    public void reset()
    {
        this.list = new ArrayList<>();
    }
}
