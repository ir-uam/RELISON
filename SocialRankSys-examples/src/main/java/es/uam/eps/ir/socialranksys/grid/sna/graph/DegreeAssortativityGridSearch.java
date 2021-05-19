/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.sna.graph;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.graph.DegreeAssortativity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.sna.graph.GraphMetricIdentifiers.DEGREEASSORT;

/**
 * Grid search for the degree assortativity metric
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class DegreeAssortativityGridSearch<U> implements GraphMetricGridSearch<U> {
    /**
     * Identifier for the orientation
     */
    private static final String ORIENT = "orientation";

    @Override
    public Map<String, Supplier<GraphMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<GraphMetric<U>>> metrics = new HashMap<>();
        List<EdgeOrientation> orients = grid.getOrientationValues(ORIENT);
        
        orients.forEach(orient -> metrics.put(DEGREEASSORT + "_" + orient, () -> new DegreeAssortativity<>(orient)));
        return metrics;
    }

    @Override
    public Map<String, GraphMetricFunction<U>> grid(Grid grid)
    {
        Map<String, GraphMetricFunction<U>> metrics = new HashMap<>();
        List<EdgeOrientation> orients = grid.getOrientationValues(ORIENT);
        
        orients.forEach(orient -> metrics.put(DEGREEASSORT + "_" + orient, (distCalc) -> new DegreeAssortativity<>(orient)));
        
        return metrics;
    }
}
