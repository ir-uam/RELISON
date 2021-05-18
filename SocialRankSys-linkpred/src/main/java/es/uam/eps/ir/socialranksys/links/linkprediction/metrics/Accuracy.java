/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction.metrics;

/**
 * Implementation of the accuracy metric for link prediction.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Accuracy<U> extends AbstractClassificationMetric<U>
{
    /**
     * Constructor.
     * @param cutoff the number of items in the ranking to consider. If cutoff <= 0, we consider that the links in the ranking are all
     *               the seen objects.
     */
    public Accuracy(int cutoff)
    {
        super(cutoff);
    }

    /**
     * Constructor.
     * @param threshold the minimum threshold. Pairs given a value greater or equal than the threshold will be considered as if they
     *                  were positively classified. The rest, as negative.
     */
    public Accuracy(double threshold)
    {
        super(threshold);
    }

    @Override
    protected double compute(long size, long truePos, long trueNeg, long falsePos, long falseNeg)
    {
        return (truePos + trueNeg + 0.0)/(size + 0.0);
    }
}