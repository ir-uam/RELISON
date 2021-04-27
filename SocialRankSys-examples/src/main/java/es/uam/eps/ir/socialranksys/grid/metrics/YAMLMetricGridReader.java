/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.metrics;

import com.esotericsoftware.yamlbeans.YamlReader;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.YAMLGridReader;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reads the different graph metrics from a YAML file.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class YAMLMetricGridReader extends YAMLGridReader
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
     * The route of the YAML file containing the grid data.
     */
    private final String file;

    /**
     * Constructor.
     * @param file the route of the YAML file containing the grid data.
     */
    public YAMLMetricGridReader(String file)
    {
        this.file = file;
        this.metricsGrid = new HashMap<>();
        MetricTypeIdentifiers.values().forEach(type -> this.metricsGrid.put(type, new HashMap<>()));
    }
    
    /**
     * It reads the YAML document containing the data.
     */
    public void readDocument()
    {
        try
        {
            // First of all, obtain the XML Document
            File inputFile = new File(file);
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
            YamlReader yaml = new YamlReader(reader);

            Map<String, Object> map = (Map<String, Object>) yaml.read();
            Map<String, Object> algs = (Map<String, Object>) map.get(METRICS);

            for(Map.Entry<String, Object> entry : algs.entrySet())
            {
                this.readMetric(entry);
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
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
            Object params = metricObject.get(PARAMS);
            if(params.getClass() == String.class) // In this case, there are no parameters
            {
                metricsGrid.get(metricType).put(metricName, new Grid());
            }
            else
            {
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
