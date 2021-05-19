/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.uam.eps.ir.socialranksys.grid.sna.comm.indiv;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.CommunityMetric;
import es.uam.eps.ir.socialranksys.metrics.IndividualCommunityMetric;
import es.uam.eps.ir.socialranksys.metrics.communities.graph.AggregateIndividualCommMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.sna.comm.indiv.IndividualCommunityMetricIdentifiers.COMMDEGREE;
import static es.uam.eps.ir.socialranksys.grid.sna.comm.indiv.IndividualCommunityMetricIdentifiers.COMMSIZE;


/**
 * Class that translates from a grid to the different contact recommendation algorithns.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
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
        IndividualCommunityMetricGridSearch<U> gridsearch;
        switch(metric)
        {
            case COMMSIZE:
                gridsearch = new CommSizeGridSearch<>();
                break;
            case COMMDEGREE:
                gridsearch = new CommDegreeGridSearch<>();
                break;
            // Default behavior
            default:
                gridsearch = null;
        }
        return gridsearch;
    }
    
    /**
     * Obtains the different variants of a given community metric depending on the 
     * parameters selected in a grid.
     * @param metric the name of the metric.
     * @param grid the grid containing the different parameters.
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
     * @param metric the name of the metric.
     * @param grid the grid containing the different parameters.
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
