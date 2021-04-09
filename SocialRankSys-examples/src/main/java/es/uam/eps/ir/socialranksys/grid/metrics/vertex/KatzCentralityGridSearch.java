/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.metrics.vertex;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.vertex.KatzCentrality;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.metrics.vertex.VertexMetricIdentifiers.KATZ;

/**
 * Grid for the Katz centrality of a node.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see es.uam.eps.ir.socialranksys.metrics.vertex.KatzCentrality
 */
public class KatzCentralityGridSearch<U> implements VertexMetricGridSearch<U>
{
    /**
     * Identifier for the orientation
     */
    private static final String ORIENT = "orientation";

    /**
     * Dump factor for longer distance paths.
     */
    private static final String ALPHA = "alpha";

    @Override
    public Map<String, Supplier<VertexMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<VertexMetric<U>>> metrics = new HashMap<>();
        List<EdgeOrientation> orients = grid.getOrientationValues(ORIENT);
        List<Double> alphas = grid.getDoubleValues(ALPHA);

        if(orients == null || orients.isEmpty())
        {
            alphas.forEach(alpha -> metrics.put(KATZ + "_" + alpha, () -> new KatzCentrality<>(alpha)));
        }
        else
        {
            alphas.forEach(alpha ->
                orients.forEach(orient ->
                    metrics.put(KATZ + "_" + alpha, () -> new KatzCentrality<>(orient, alpha))));
        }

        return metrics;
    }

    @Override
    public Map<String, VertexMetricFunction<U>> grid(Grid grid)
    {
        Map<String, VertexMetricFunction<U>> metrics = new HashMap<>();
        List<EdgeOrientation> orients = grid.getOrientationValues(ORIENT);
        List<Double> alphas = grid.getDoubleValues(ALPHA);

        if(orients == null || orients.isEmpty())
        {
            alphas.forEach(alpha -> metrics.put(KATZ + "_" + alpha, (distCalc) -> new KatzCentrality<>(alpha)));
        }
        else
        {
            alphas.forEach(alpha ->
                orients.forEach(orient ->
                    metrics.put(KATZ + "_" + alpha, (distCalc) -> new KatzCentrality<>(orient, alpha))));
        }

        return metrics;
    }
    
}
