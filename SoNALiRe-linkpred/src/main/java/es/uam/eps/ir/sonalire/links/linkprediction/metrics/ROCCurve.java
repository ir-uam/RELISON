/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.linkprediction.metrics;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.linkprediction.Prediction;
import es.uam.eps.ir.sonalire.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Given a list, finds the receiver operating characteristic (ROC) curve.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ROCCurve<U>
{
    /**
     * The network.
     */
    private final Graph<U> graph;

    /**
     * Constructor.
     * @param graph the network for checking the relevance of the links.
     */
    public ROCCurve(Graph<U> graph)
    {
        this.graph = graph;
    }

    /**
     * Computes the ROC curve for a prediction. It considers that the prediction is applied over all possible
     * edges in the network (regardless of whether they previously existed or not).
     * @param pred the prediction.
     * @return the value of the metric.
     */
    public List<Pair<Double>> evaluate(Prediction<U> pred)
    {
        List<Pair<Double>> curve = new ArrayList<>();
        List<Tuple2od<Pair<U>>> prediction = pred.getPrediction();

        long size = graph.getVertexCount()*(graph.getVertexCount()-1);
        long numPos = graph.getEdgeCount();
        long numNeg = size - numPos;

        double lastValue = Double.NaN;
        double numRels = 0;
        double numNotRels = 0;

        curve.add(new Pair<>(0.0, 0.0));

        for(Tuple2od<Pair<U>> value : prediction)
        {
            double val = value.v2;
            if(val != lastValue)
            {
                if(!Double.isNaN(lastValue))
                {
                    Pair<Double> pair = new Pair<>((numRels + 0.0) / (numPos + 0.0), (numNotRels + 0.0)/(numNeg + 0.0));
                    curve.add(pair);
                }
                else
                {
                    lastValue = val;
                }
            }

            if(graph.containsEdge(value.v1.v1(), value.v1.v2()))
            {
                numRels++;
            }
            else
            {
                numNotRels++;
            }
        }

        curve.add(new Pair<>((numRels + 0.0) / (numPos + 0.0), (numNotRels + 0.0) / (numNeg + 0.0)));
        if(numRels != numPos || numNotRels != numNeg)
        {
            curve.add(new Pair<>(1.0, 1.0));
        }
        return curve;
    }

    /**
     * Computes the ROC curve for a prediction. It considers that the prediction is applied over all possible
     * edges in the network (regardless of whether they previously existed or not).
     * @param pred the prediction.
     * @return the value of the metric.
     */
    public List<Pair<Double>> evaluate(Prediction<U> pred, Predicate<Pair<U>> filter)
    {
        List<Pair<Double>> curve = new ArrayList<>();
        List<Tuple2od<Pair<U>>> prediction = pred.getPrediction();

        long size = graph.getAllNodes().mapToLong(u -> graph.getAllNodes().filter(v -> filter.test(new Pair<>(u,v))).count()).sum();
        long numPos = graph.getAllNodes().mapToLong(u -> graph.getAdjacentNodes(u).filter(v -> filter.test(new Pair<>(u,v))).count()).sum();
        long numNeg = size - numPos;

        double lastValue = Double.NaN;
        double numRels = 0;
        double numNotRels = 0;

        curve.add(new Pair<>(0.0, 0.0));

        for(Tuple2od<Pair<U>> value : prediction)
        {
            double val = value.v2;
            if(val != lastValue)
            {
                if(!Double.isNaN(lastValue))
                {
                    Pair<Double> pair = new Pair<>((numRels + 0.0) / (numPos + 0.0), (numNotRels + 0.0)/(numNeg + 0.0));
                    curve.add(pair);
                }
                else
                {
                    lastValue = val;
                }
            }

            if(!filter.test(value.v1))
            {
                continue;
            }

            if(graph.containsEdge(value.v1.v1(), value.v1.v2()))
            {
                numRels++;
            }
            else
            {
                numNotRels++;
            }
        }

        curve.add(new Pair<>((numRels + 0.0) / (numPos + 0.0), (numNotRels + 0.0) / (numNeg + 0.0)));
        if(numRels != numPos || numNotRels != numNeg)
        {
            curve.add(new Pair<>(1.0, 1.0));
        }
        return curve;
    }

    /**
     * Computes the ROC curve for a prediction. It considers that the prediction is applied over all possible
     * edges in the network (regardless of whether they previously existed or not).
     * @param pred the prediction.
     * @return the value of the metric.
     */
    public List<Pair<Double>> evaluate(Prediction<U> pred, Stream<Pair<U>> candidates)
    {
        Set<Pair<U>> set = candidates.collect(Collectors.toSet());
        List<Pair<Double>> curve = new ArrayList<>();
        List<Tuple2od<Pair<U>>> prediction = pred.getPrediction();

        long size = candidates.count();
        long numPos = candidates.filter(p -> graph.containsEdge(p.v1(), p.v2())).count();
        long numNeg = size - numPos;

        double lastValue = Double.NaN;
        double numRels = 0;
        double numNotRels = 0;

        curve.add(new Pair<>(0.0, 0.0));

        for(Tuple2od<Pair<U>> value : prediction)
        {
            double val = value.v2;
            if(val != lastValue)
            {
                if(!Double.isNaN(lastValue))
                {
                    Pair<Double> pair = new Pair<>((numRels + 0.0) / (numPos + 0.0), (numNotRels + 0.0)/(numNeg + 0.0));
                    curve.add(pair);
                }
                else
                {
                    lastValue = val;
                }
            }

            if(!set.contains(value.v1))
            {
                continue;
            }

            if(graph.containsEdge(value.v1.v1(), value.v1.v2()))
            {
                numRels++;
            }
            else
            {
                numNotRels++;
            }
        }

        curve.add(new Pair<>((numRels + 0.0) / (numPos + 0.0), (numNotRels + 0.0) / (numNeg + 0.0)));
        if(numRels != numPos || numNotRels != numNeg)
        {
            curve.add(new Pair<>(1.0, 1.0));
        }
        return curve;
    }
}