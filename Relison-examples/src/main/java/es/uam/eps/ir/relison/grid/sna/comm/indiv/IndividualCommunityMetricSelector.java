/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.comm.indiv;

import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.metrics.CommunityMetric;
import es.uam.eps.ir.relison.metrics.IndividualCommunityMetric;
import es.uam.eps.ir.relison.metrics.communities.graph.AggregateIndividualCommMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.sna.comm.indiv.IndividualCommunityMetricIdentifiers.*;


/**
 * Class that translates from a grid to the different individual community metrics.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class IndividualCommunityMetricSelector<U>
{
    /**
     * Obtains the grid configurator for a metric.
     * @param metric the name of the metric.
     * @return the grid configurator for the metric if it exists, null otherwise.
     */
    public IndividualCommunityMetricGridSearch<U> getGridSearch(String metric)
    {
        // Default behavior
        return switch (metric)
        {
            case COMMSIZE -> new CommSizeGridSearch<>();
            case COMMDEGREE -> new CommDegreeGridSearch<>();
            case VOLUME -> new VolumeGridSearch<>();
            default -> null;
        };
    }
    
    /**
     * Obtains the different variants of a given community metric depending on the 
     * parameters selected in a grid.
     * @param metric    the name of the metric.
     * @param grid      the grid containing the different parameters.
     * @return a map containing the different metric suppliers.
     */
    public Map<String, Supplier<IndividualCommunityMetric<U>>> getMetrics(String metric, Grid grid)
    {
        IndividualCommunityMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if(gridsearch != null)
            return gridsearch.grid(grid);
        return new HashMap<>();
    }
    
    /**
     * Obtains the aggregate variants of a given individual community metric, given the parameters selected
     * in a grid.
     * @param metric    the name of the metric.
     * @param grid      the grid containing the different parameters.
     * @return a map, indexed by metric name, containing the different variants of the metric selected in the grid.
     */
    public Map<String, Supplier<CommunityMetric<U>>> getGraphMetrics(String metric, Grid grid)
    {
        IndividualCommunityMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if(gridsearch != null)
        {
            Map<String, Supplier<IndividualCommunityMetric<U>>> map = this.getMetrics(metric, grid);
            Map<String, Supplier<CommunityMetric<U>>> metricMap = new HashMap<>();
            
            map.forEach((key, value) -> metricMap.put(key, () -> new AggregateIndividualCommMetric<>(value.get())));
            return metricMap;
        }

        return new HashMap<>();
    }
}
