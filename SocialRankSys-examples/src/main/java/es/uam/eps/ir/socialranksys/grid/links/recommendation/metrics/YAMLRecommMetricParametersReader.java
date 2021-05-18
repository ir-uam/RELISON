/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics;


import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.YAMLParametersReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class for reading contact recommendation metrics.
 * For each metric, it reads a single configuration.
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
 * @see YAMLParametersReader
 */
public class YAMLRecommMetricParametersReader extends YAMLParametersReader
{
    private final static String METRICS = "metrics";

    /**
     * Metric parameters. A single parameter set for each metric is stored.
     */
    private final Map<String, Parameters> metricsGrid;

    /**
     * Constructor.
     */
    public YAMLRecommMetricParametersReader()
    {
        this.metricsGrid = new HashMap<>();
    }

    /**
     * Obtains the metric parameters for different recommendation metrics.
     */
    public void read(Map<String, Object> map)
    {
        metricsGrid.clear();
        if(map != null)
        {
            Map<String, Object> algs = (Map<String, Object>) map.get(METRICS);
            for(Map.Entry<String, Object> entry : algs.entrySet())
            {
                this.readMetric(entry);
            }
        }
    }

    /**
     * Reads the parameters for a single metric.
     *
     * @param element a map containing the information in the YAML file about the metric.
     */
    private void readMetric(Map.Entry<String, Object> element)
    {
        String metricName = element.getKey();

        // Then: we check whether there is something else to this algorithm:
        if(element.getValue().getClass() == String.class) // if this happens, there is not
        {
            Parameters p = new Parameters();
            this.metricsGrid.put(metricName, p);
        }
        else
        {
            Map<String, Object> params = (Map<String, Object>) element.getValue();
            Parameters p = readParameterValues(params);
            this.metricsGrid.put(metricName, p);
        }
    }

    /**
     * Gets the set of metrics previously read.
     *
     * @return The set of metrics previously read from the grid file.
     */
    public Set<String> getMetric()
    {
        return this.metricsGrid.keySet();
    }

    /**
     * Gets the grid for a given metric
     *
     * @param metric the metric to search
     *
     * @return the grid if exists, an empty grid if not.
     */
    public Parameters getParameters(String metric)
    {
        return this.metricsGrid.getOrDefault(metric, new Parameters());
    }
}
