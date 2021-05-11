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
import es.uam.eps.ir.socialranksys.metrics.complementary.vertex.ComplementaryInverseDegree;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.metrics.vertex.VertexMetricIdentifiers.COMPLINVDEGREE;


/**
 * Grid for the inverse degree of a node in the complementary graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class ComplementaryInverseDegreeGridSearch<U> implements VertexMetricGridSearch<U> 
{
    /**
     * Identifier for the orientation
     */
    private static final String ORIENT = "orientation";

    @Override
    public Map<String, Supplier<VertexMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<VertexMetric<U>>> metrics = new HashMap<>();
        List<EdgeOrientation> orients = grid.getOrientationValues(ORIENT);
        orients.forEach(orient -> metrics.put(COMPLINVDEGREE + "_" + orient, () -> new ComplementaryInverseDegree<>(orient)));
        return metrics;
    }

    @Override
    public Map<String, VertexMetricFunction<U>> grid(Grid grid)
    {
        Map<String, VertexMetricFunction<U>> metrics = new HashMap<>();
        List<EdgeOrientation> orients = grid.getOrientationValues(ORIENT);
        orients.forEach(orient -> metrics.put(COMPLINVDEGREE + "_" + orient, (distCalc) -> new ComplementaryInverseDegree<>(orient)));
        return metrics;
    }
    
}
