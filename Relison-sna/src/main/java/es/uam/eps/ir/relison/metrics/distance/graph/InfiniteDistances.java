/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics.distance.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.metrics.GraphMetric;
import es.uam.eps.ir.relison.metrics.distance.DistanceCalculator;

/**
 * Finds the number of infinite distance pairs of nodes.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class InfiniteDistances<U> implements GraphMetric<U>
{
    /**
     * The distance calculator.
     */
    private final DistanceCalculator<U> calculator;

    /**
     * Constructor.
     * @param calculator a distance calculator.
     */
    public InfiniteDistances(DistanceCalculator<U> calculator)
    {
        this.calculator = calculator;
    }

    @Override
    public double compute(Graph<U> graph)
    {
        calculator.computeDistances(graph);
        return calculator.getInfiniteDistances();
        //return graph.getAllNodes().mapToDouble(node -> graph.getAllNodes().filter(target -> !target.equals(node)).filter(target -> !Double.isFinite(this.calculator.getDistances(node, target))).count() + 0.0).sum();
    }
}
