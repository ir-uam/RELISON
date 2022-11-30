/*
 *  Copyright (C) 2022 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es / Terrier Team at University of Glasgow,
 *  http://http://terrierteam.dcs.gla.ac.uk
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.relison.grid.sna.vertex;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.sna.metrics.VertexMetric;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.vertex.Distance2Degree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.sna.vertex.VertexMetricIdentifiers.FOAF;

/**
 * Grid for computing the number of neighbors at distance 2 in the graph (friends of friends)
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 *
 * @see Distance2Degree
 */
public class Distance2DegreeGridSearch<U> implements VertexMetricGridSearch<U>
{
    /**
     * Identifier for the first neighbor of the target user.
     */
    private final static String USEL = "uSel";
    /**
     * Identifier for the neighbor of the neighbor of the target user.
     */
    private final static String VSEL = "vSel";

    @Override
    public Map<String, Supplier<VertexMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<VertexMetric<U>>> metrics = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);

        uSels.forEach(uSel ->
                vSels.forEach(vSel ->
                        metrics.put(FOAF + "_" + uSel + "_" + vSel, () ->
                                new Distance2Degree<>(uSel, vSel))));

        return metrics;
    }

    @Override
    public Map<String, VertexMetricFunction<U>> grid(Grid grid)
    {
        Map<String, VertexMetricFunction<U>> metrics = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);

        uSels.forEach(uSel ->
                vSels.forEach(vSel ->
                        metrics.put(FOAF + "_" + vSel + "_" + vSel, (distCalc) ->
                                new Distance2Degree<>(uSel, vSel))));

        return metrics;
    }
}
