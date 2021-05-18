/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction.metrics;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.linkprediction.Prediction;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Implementation of the area under the receiver-operating characteristic curve
 * for link prediction.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AUC<U> implements LinkPredictionMetric<U>
{
    @Override
    public double evaluate(Graph<U> graph, Prediction<U> pred)
    {
        ROCCurve<U> rocCurve = new ROCCurve<>(graph);
        return this.computeAUC(rocCurve.evaluate(pred));
    }

    @Override
    public double evaluate(Graph<U> graph, Prediction<U> pred, Predicate<Pair<U>> filter)
    {
        ROCCurve<U> rocCurve = new ROCCurve<>(graph);
        return this.computeAUC(rocCurve.evaluate(pred, filter));
    }

    @Override
    public double evaluate(Graph<U> graph, Prediction<U> pred, Stream<Pair<U>> users)
    {
        ROCCurve<U> rocCurve = new ROCCurve<>(graph);
        return this.computeAUC(rocCurve.evaluate(pred, users));
    }

    /**
     * Given a ROC curve, computes its area.
     * @param roc the ROC curve.
     * @return the area under the ROC curve.
     */
    private double computeAUC(List<Pair<Double>> roc)
    {
        int size = roc.size();
        double auc = 0.0;
        for (int i = 1; i < size; ++i)
        {
            double currentX = roc.get(i).v2();
            double previousX = roc.get(i - 1).v2();

            double currentY = roc.get(i).v1();
            double previousY = roc.get(i - 1).v1();

            auc += (currentX - previousX) * (currentY + previousY) / 2.0;
        }
        return auc;
    }
}