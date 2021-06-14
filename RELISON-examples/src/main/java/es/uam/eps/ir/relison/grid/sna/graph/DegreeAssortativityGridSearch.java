/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.graph;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.sna.metrics.GraphMetric;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.graph.DegreeAssortativity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.sna.graph.GraphMetricIdentifiers.DEGREEASSORT;

/**
 * Grid search for the degree assortativity metric
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> type of the users
 *
 * @see DegreeAssortativity
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
