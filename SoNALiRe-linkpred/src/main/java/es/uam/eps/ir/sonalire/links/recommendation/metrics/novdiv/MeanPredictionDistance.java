/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.metrics.novdiv;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.distance.FastDistanceCalculator;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;

/**
 * Metric that finds the harmonic mean of the reciprocal distances between the different target user - candidate user pairs
 * of the recommendation.
 *
 * <p><b>References:</b>
 * <ol>
 *  <li>J. Sanz-Cruzado and P. Castells. Beyond Accuracy in Link Prediction. SoMePeaS 2019.</li>
 * </ol></p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MeanPredictionDistance<U> implements SystemMetric<U,U>
{
    /**
     * The training network.
     */
    private final Graph<U> graph;
    /**
     * The list of recommendations.
     */
    private final List<Recommendation<U,U>> recs;
    /**
     * The cutoff of the recommendation
     */
    private final int cutoff;
    /**
     * The distance calculator.
     */
    DistanceCalculator<U> calculator;

    /**
     * Constructor.
     *
     * @param graph     the training graph.
     * @param cutoff    recommendation cutoff.
     */
    public MeanPredictionDistance(Graph<U> graph, int cutoff)
    {
        this(graph, new FastDistanceCalculator<>(), cutoff);
    }

    /**
     * Constructor.
     *
     * @param graph     the training graph.
     * @param calc      distance calculator.
     * @param cutoff    recommendation cutoff.
     */
    public MeanPredictionDistance(Graph<U> graph, DistanceCalculator<U> calc, int cutoff)
    {
        this.cutoff = cutoff;
        this.graph = graph;
        this.recs = new ArrayList<>();
        this.calculator = calc;
        this.calculator.computeDistances(graph);
    }

    @Override
    public void add(Recommendation<U, U> recommendation)
    {
        recs.add(recommendation);
    }

    @Override
    public double evaluate()
    {
        double numEdges = 0.0;
        double value = 0.0;

        for(Recommendation<U,U> rec : recs)
        {
            U target = rec.getUser();
            List<Tuple2od<U>> items = rec.getItems();
            int n = Math.min(items.size(), cutoff);
            numEdges += n;
            for(int i = 0; i < n; ++i)
            {
                U candidate = items.get(i).v1;
                double dist = this.calculator.getDistances(target, candidate);
                if(!Double.isNaN(dist) && Double.isFinite(dist))
                {
                    value += 1.0/dist;
                }
            }
        }

        if(value > 0.0) return numEdges/value - 2.0;
        else if (numEdges > 0.0) return Double.POSITIVE_INFINITY; // All recommended links were at distance == INFINITY
        else return 0.0;
    }

    @Override
    public void combine(SystemMetric<U, U> systemMetric)
    {
        MeanPredictionDistance<U> other = (MeanPredictionDistance<U>) systemMetric;
        for(Recommendation<U,U> rec : other.recs)
            this.add(rec);
    }

    @Override
    public void reset()
    {
        this.recs.clear();
    }
}
