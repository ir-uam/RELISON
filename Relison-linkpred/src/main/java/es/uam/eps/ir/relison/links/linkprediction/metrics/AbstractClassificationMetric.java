/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.linkprediction.metrics;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.links.linkprediction.Prediction;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
public abstract class AbstractClassificationMetric<U> implements LinkPredictionMetric<U>
{
    /**
     * The number of items in the ranking to consider.
     */
    private final int cutoff;
    /**
     * The classification threshold (values below this will be classified as negative).
     */
    private final double threshold;
    /**
     * True if we use the cutoff of the prediction to differentiate between positive and negative examples, false if
     * we use a threshold value.
     */
    private final boolean useCutoff;
    /**
     * Constructor.
     * @param cutoff the number of items in the ranking to consider. If cutoff is smaller or equal than 0, we consider that the links in the ranking are all
     *               the seen objects.
     */
    public AbstractClassificationMetric(int cutoff)
    {
        this.cutoff = cutoff;
        this.threshold = Double.NaN;
        this.useCutoff = true;
    }

    /**
     * Constructor.
     * @param threshold the minimum threshold. Pairs given a value greater or equal than the threshold will be considered as if they
     *                  were positively classified. The rest, as negative.
     */
    public AbstractClassificationMetric(double threshold)
    {
        this.cutoff = 0;
        this.threshold = threshold;
        this.useCutoff = false;
    }

    @Override
    public double evaluate(Graph<U> graph, Prediction<U> pred)
    {
        long truePos = 0L;
        long falsePos = 0L;
        long trueNeg;
        long falseNeg;

        long size = graph.getVertexCount()*(graph.getVertexCount()-1);
        long numPos = graph.getEdgeCount();
        long numNeg = size - numPos;

        List<Tuple2od<Pair<U>>> ranking = pred.getPrediction();

        if(useCutoff)
        {
            for(int i = 0; i < size; ++i)
            {
                Pair<U> p = ranking.get(i).v1;
                if(graph.containsEdge(p.v1(), p.v2()))
                {
                    truePos++;
                }
                else
                {
                    falsePos++;
                }
            }
        }
        else
        {
            for(Tuple2od<Pair<U>> link : ranking)
            {
                if(link.v2 < threshold) break;

                if(graph.containsEdge(link.v1.v1(), link.v1.v2()))
                {
                    truePos++;
                }
                else
                {
                    falsePos++;
                }
            }
        }

        falseNeg = numPos - truePos;
        trueNeg = numNeg - falseNeg;

        return this.compute(size, truePos, trueNeg, falsePos, falseNeg);
    }

    @Override
    public double evaluate(Graph<U> graph, Prediction<U> pred, Predicate<Pair<U>> filter)
    {
        long truePos = 0L;
        long falsePos = 0L;
        long trueNeg;
        long falseNeg;

        long size = graph.getAllNodes().mapToLong(u -> graph.getAllNodes().filter(v -> filter.test(new Pair<>(u,v))).count()).sum();
        long numPos = graph.getAllNodes().mapToLong(u -> graph.getAdjacentNodes(u).filter(v -> filter.test(new Pair<>(u,v))).count()).sum();
        long numNeg = size - numPos;

        List<Tuple2od<Pair<U>>> ranking = pred.getPrediction();

        if(useCutoff)
        {
            int i = 0;
            for(Tuple2od<Pair<U>> link : ranking)
            {
                if(i >= cutoff) break;

                Pair<U> p = link.v1;
                if(!filter.test(p))
                {
                    continue;
                }

                if(graph.containsEdge(p.v1(), p.v2()))
                {
                    truePos++;
                }
                else
                {
                    falsePos++;
                }
                ++i;
            }
        }
        else
        {
            for(Tuple2od<Pair<U>> link : ranking)
            {
                if(link.v2 < threshold) break;
                if(!filter.test(link.v1)) continue;

                if(graph.containsEdge(link.v1.v1(), link.v1.v2()))
                {
                    truePos++;
                }
                else
                {
                    falsePos++;
                }
            }
        }

        falseNeg = numPos - truePos;
        trueNeg = numNeg - falseNeg;

        return this.compute(size, truePos, trueNeg, falsePos, falseNeg);
    }

    @Override
    public double evaluate(Graph<U> graph, Prediction<U> pred, Stream<Pair<U>> pairs)
    {
        Set<Pair<U>> set = pairs.collect(Collectors.toSet());

        long size = pairs.count();
        long numPos = pairs.filter(p -> graph.containsEdge(p.v1(), p.v2())).count();
        long numNeg = size - numPos;

        List<Tuple2od<Pair<U>>> ranking = pred.getPrediction();
        long truePos = 0L;
        long falsePos = 0L;
        long trueNeg;
        long falseNeg;

        if(useCutoff)
        {
            int i = 0;
            for(Tuple2od<Pair<U>> link : ranking)
            {
                if(i >= cutoff) break;

                Pair<U> p = link.v1;
                if(!set.contains(p))
                {
                    continue;
                }

                if(graph.containsEdge(p.v1(), p.v2()))
                {
                    truePos++;
                }
                else
                {
                    falsePos++;
                }
                ++i;
            }
        }
        else
        {
            for(Tuple2od<Pair<U>> link : ranking)
            {
                if(link.v2 < threshold) break;
                if(!set.contains(link.v1)) continue;

                if(graph.containsEdge(link.v1.v1(), link.v1.v2()))
                {
                    truePos++;
                }
                else
                {
                    falsePos++;
                }
            }
        }

        falseNeg = numPos - truePos;
        trueNeg = numNeg - falseNeg;

        return this.compute(size, truePos, trueNeg, falsePos, falseNeg);
    }

    /**
     * Computes the actual classification metric.
     * @param size      the total number of links to consider.
     * @param truePos   the number of correctly classified positive links.
     * @param trueNeg   the number of correctly classified negative links.
     * @param falsePos  the number of wrongly classified positive links.
     * @param falseNeg  the number of wrongly classified negative links.
     * @return the value of the metric.
     */
    protected abstract double compute(long size, long truePos, long trueNeg, long falsePos, long falseNeg);
}