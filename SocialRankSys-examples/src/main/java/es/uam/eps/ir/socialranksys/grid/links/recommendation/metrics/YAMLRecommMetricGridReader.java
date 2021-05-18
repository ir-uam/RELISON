/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.YAMLGridReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class for reading contact recommendation metrics..
 * For each metric, a grid of parameters is read.
 *
 * <br/>
 *
 * File format:<br/>
 * metrics:<br/>
 *     metric_name:<br />
 *         param1: ...<br/>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see YAMLGridReader
 */
public class YAMLRecommMetricGridReader extends YAMLGridReader
{
    private final static String METRICS = "metrics";

    /**
     * Metrics grid. Uses a grid for each metric.
     */
    private final Map<String, Grid> metricsGrid;

    /**
     * Constructor
     */
    public YAMLRecommMetricGridReader()
    {
        this.metricsGrid = new HashMap<>();
    }

    /**
     * Obtains the different grids for contact recommendation metrics.
     * @param map a map containing the information in the YAML file about the different metrics.
     */
    public void read(Map<String, Object> map)
    {
        metricsGrid.clear();
        if(map != null)
        {
            Map<String, Object> metrics = (Map<String, Object>) map.get(METRICS);
            for(Map.Entry<String, Object> entry : metrics.entrySet())
            {
                this.readMetric(entry);
            }
        }
    }

    /**
     * Reads the grid for a single metric.
     *
     * @param element a map containing the information in the YAML file about the metric.
     */
    private void readMetric(Map.Entry<String, Object> element)
    {
        String metricName = element.getKey();

        // Then: we check whether there is something else to this algorithm:
        if(element.getValue().getClass() == String.class) // if this happens, there is not
        {
            Grid g = new Grid();
            this.metricsGrid.put(metricName, g);
        }
        else
        {
            Map<String, Object> params = (Map<String, Object>) element.getValue();

            Grid g = readParameterGrid(params);
            this.metricsGrid.put(metricName, g);
        }
    }

    /**
     * Gets the set of algorithms previously read.
     *
     * @return The set of algorithms previously read from the grid file.
     */
    public Set<String> getMetrics()
    {
        return this.metricsGrid.keySet();
    }

    /**
     * Gets the grid for a given algorithm
     *
     * @param algorithm the algorithm to search
     *
     * @return the grid if exists, an empty grid if not.
     */
    public Grid getGrid(String algorithm)
    {
        return this.metricsGrid.getOrDefault(algorithm, new Grid());
    }
}
