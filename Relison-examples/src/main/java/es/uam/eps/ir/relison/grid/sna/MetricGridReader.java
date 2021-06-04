/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna;

import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.GridReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class for reading social network analysis metrics.
 * For each metric, a grid of parameters is read.
 *
 * <br>
 *
 * File format:<br>
 * metrics:<br>
 *     metric_name:<br >
 *          type: value<br >
 *          params:<br >
 *              param1: ...<br>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see GridReader
 */
public class MetricGridReader extends GridReader
{
    /**
     * Identifier for the metrics.
     */
    private final static String METRICS = "metrics";
    /**
     * Identifier for the metric type.
     */
    private final static String TYPE = "type";
    /**
     * Identifier for the metric type.
     */
    private final static String PARAMS = "params";
    /**
     * A map containing the different configurations for each metric. It is indexed by the name of the
     * metric.
     */
    private final Map<String, Map<String, Grid>> metricsGrid;

    /**
     * Constructor.
     */
    public MetricGridReader()
    {
        this.metricsGrid = new HashMap<>();
        MetricTypeIdentifiers.values().forEach(type -> this.metricsGrid.put(type, new HashMap<>()));
    }
    
    /**
     * It reads the YAML document containing the data.
     * @param map map containing the grids for the metrics.
     */
    public void read(Map<String, Object> map)
    {
        MetricTypeIdentifiers.values().forEach(type -> this.metricsGrid.get(type).clear());
        Map<String, Object> algs = (Map<String, Object>) map.get(METRICS);

        for(Map.Entry<String, Object> entry : algs.entrySet())
        {
            this.readMetric(entry);
        }
    }

    /**
     * Reads the grid for a single metric.
     * @param element the YAML element containing the information about the metric.
     */
    private void readMetric(Map.Entry<String, Object> element)
    {
        String metricName = element.getKey();
        Map<String, Object> metricObject = (Map<String, Object>) element.getValue();

        String metricType = metricObject.get(TYPE).toString();
        if(metricsGrid.containsKey(metricType))
        {
            if(metricObject.containsKey(PARAMS) && metricObject.get(PARAMS).getClass() == String.class) // In this case, there are no parameters
            {
                metricsGrid.get(metricType).put(metricName, new Grid());
            }
            else
            {
                Object params = metricObject.get(PARAMS);
                Map<String, Object> parameters = (Map<String, Object>) params;
                Grid g = this.readParameterGrid(parameters);
                this.metricsGrid.get(metricType).put(metricName, g);
            }
        }
        else
        {
            System.err.println("Invalid metric type");
        }
    }
    
    /**
     * Once the metrics have been read, it obtains the set of metrics of a given type which have been
     * retrieved from the file.
     * @param type the type of metric.
     * @return the set of metrics of the given metric which have been retrieved.
     */
    public Set<String> getMetrics(String type)
    {
        return this.metricsGrid.get(type).keySet();
    }

    /**
     * Once the metrics have been read, it obtains the grid for a single one.
     * @param metric    the name of the metric.
     * @param type      the type of metric.
     * @return the set of metrics of the given metric which have been retrieved.
     */
    public Grid getGrid(String metric, String type)
    {
        return this.metricsGrid.get(type).getOrDefault(metric, new Grid());
    }
}
