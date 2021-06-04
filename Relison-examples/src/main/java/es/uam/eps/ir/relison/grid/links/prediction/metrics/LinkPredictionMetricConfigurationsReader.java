/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.prediction.metrics;

import es.uam.eps.ir.relison.grid.Configurations;
import es.uam.eps.ir.relison.grid.ConfigurationsReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for reading contact recommendation metrics.
 * For each metric, it reads a list of configurations.
 *
 * <br>
 *
 * File format:<br>
 * metrics:<br>
 *     metric_name:<br >
 *     - param1: ...<br>
 *       param2: ...<br>
 *     - param1: ...<br >
 *     ...
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see ConfigurationsReader
 */
public class LinkPredictionMetricConfigurationsReader extends ConfigurationsReader
{
    private final static String METRICS = "metrics";

    /**
     * Map for storing the different configurations of a metric.
     */
    private final Map<String, Configurations> metricConfs;

    /**
     * Constructor.
     */
    public LinkPredictionMetricConfigurationsReader()
    {
        this.metricConfs = new HashMap<>();
    }

    /**
     * Reads a YAML document containing the different configurations of a list of metrics.
     * @param map a map containing the configurations for the different metrics.
     */
    public void  read(Map<String, Object> map)
    {
        metricConfs.clear();
        Map<String, Object> algs = (Map<String, Object>) map.get(METRICS);

        for(Map.Entry<String, Object> entry : algs.entrySet())
        {
            this.readMetrics(entry);
        }
    }

    /**
     * Reads the configurations for a single metric.
     *
     * @param element an object containing the configurations for a single metric.
     */
    private void readMetrics(Map.Entry<String, Object> element)
    {
        String metricName = element.getKey();

        // Then: we check whether there is something else to this algorithm:
        if(element.getValue().getClass() == String.class) // if this happens, there is not
        {
            Configurations confs = new Configurations();
            this.metricConfs.put(metricName, confs);
        }
        else
        {
            List<Object> confs = (List<Object>) element.getValue();
            Configurations configurations = this.readConfigurationGrid(confs, new LinkPredictionMetricParametersReader());
            this.metricConfs.put(metricName, configurations);
        }
    }

    /**
     * Once the configurations have been read, it obtains the set of metrics which have been read
     * from the file. Otherwise, it returns an empty set.
     * @return a set containing the names of the metrics in the configuration file.
     */
    public Set<String> getMetrics()
    {
        return this.metricConfs.keySet();
    }

    /**
     * Gets the different configurations for a single metric.
     * @param algorithm the name of the metric.
     * @return the configurations for the given metric if exists, an empty list of configurations otherwise.
     */
    public Configurations getConfigurations(String algorithm)
    {
        return this.metricConfs.getOrDefault(algorithm, new Configurations());
    }
}
