/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion;

import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.diffusion.filter.FilterParameterReader;
import es.uam.eps.ir.sonalire.grid.diffusion.metrics.MetricParameterReader;
import es.uam.eps.ir.sonalire.grid.diffusion.metrics.distributions.DistributionParameterReader;
import org.jooq.lambda.tuple.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Obtains the configuration of metrics to use in the evaluation of the information diffusion process.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SimulationMetricsParameterReader
{
    /**
     * Identifier for the metrics list.
     */
    private final static String METRICS = "metrics";
    /**
     * Identifier for the distributions list
     */
    private final static String DISTRIBUTIONS = "distributions";
    /**
     * Identifier for filters
     */
    private final static String FILTERS = "filters";
    /**
     * Identifier for the filter
     */
    private final static String FILTER = "filter";

    /**
     * Parameters for the different filters.
     */
    private final List<FilterParameterReader> filterParams;

    /**
     * Parameters for the different metrics to compute.
     */
    private final Map<String, Parameters> metricParams;
    /**
     * Parameters for the different distributions to compute.
     */
    private final Map<String, Tuple2<Parameters, List<Integer>>> distribParams;

    /**
     * Constructor
     */
    public SimulationMetricsParameterReader()
    {
        this.filterParams = new ArrayList<>();
        this.metricParams = new HashMap<>();
        this.distribParams = new HashMap<>();
    }
    
    
    /**
     * Obtains the metric configuration for the diffusion simulation from a YAML file.
     * @param map a map representing the contents of the YAML file.
     */
    public void read(Map<String, Object> map)
    {
        filterParams.clear();
        metricParams.clear();
        distribParams.clear();

        if(map.containsKey(METRICS) && map.get(METRICS).getClass() != String.class)
        {
            Map<String, Object> metricList = (Map<String, Object>) map.get(METRICS);
            this.readMetrics(metricList);
        }

        if(map.containsKey(DISTRIBUTIONS) && map.get(DISTRIBUTIONS).getClass() != String.class)
        {
            Map<String, Object> distribList = (Map<String, Object>) map.get(DISTRIBUTIONS);
            this.readDistributions(distribList);
        }

        if(map.containsKey(FILTERS) && map.get(FILTERS).getClass() != String.class)
        {
            Map<String, Object> filterList = (Map<String, Object>) map.get(FILTERS);
            this.readFilters(filterList);
        }
    }
    
    
    /**
     * Reads the configuration parameters for a list of information propagation metrics.
     * @param node the map specifying the list of metrics.
     */
    private void readMetrics(Map<String, Object> node)
    {
        for(Map.Entry<String, Object> metric : node.entrySet())
        {
            MetricParameterReader mpr = new MetricParameterReader();
            mpr.readMetric(metric);
            this.metricParams.put(mpr.getName(), mpr.getParams());
        }
    }
    
    /**
     * Reads the configuration parameters for a list of information propagation distributions.
     * @param node the node containing the list of distributions.
     */
    private void readDistributions(Map<String, Object> node)
    {
        for(Map.Entry<String, Object> distrib : node.entrySet())
        {
            DistributionParameterReader dpr = new DistributionParameterReader();
            dpr.readDistribution(distrib);
            this.distribParams.put(dpr.getName(), new Tuple2<>(dpr.getParams(), dpr.getTimes()));
        }
    }
    
    /**
     * Reads the configuration parameters for the data filters.
     * @param filters a map containing the filters to apply.
     */
    private void readFilters(Map<String, Object> filters)
    {
        for(Map.Entry<String, Object> filter : filters.entrySet())
        {
            FilterParameterReader fpr = new FilterParameterReader();
            fpr.readFilter(filter);
            this.filterParams.add(fpr);
        }
    }

    /**
     * Obtains the list of metrics.
     * @return a list containing the names of the metrics.
     */
    public List<String> getMetrics()
    {
        return new ArrayList<>(metricParams.keySet());
    }

    /**
     * Obtains the set of parameters for a single metric.
     * @param metric the name of the metric.
     * @return the parameters for the given metric if they exist, an empty parameter set otherwise.
     */
    public Parameters getMetricParameters(String metric)
    {
        return metricParams.getOrDefault(metric, new Parameters());
    }


    /**
     * Obtains the list of metrics.
     * @return a list containing the names of the metrics.
     */
    public List<String> getDistributions()
    {
        return new ArrayList<>(distribParams.keySet());
    }

    /**
     * Obtains the set of parameters for a single distribution.
     * @param distrib the name of the distribution.
     * @return the parameters for the given distribution if they exist, an empty parameter set otherwise.
     */
    public Tuple2<Parameters, List<Integer>> getDistributionParameters(String distrib)
    {
        return distribParams.getOrDefault(distrib, new Tuple2<>(new Parameters(), new ArrayList<>()));
    }
    
    /**
     * Gets the filter parameters for a given simulation.
     * @return the filter parameters.
     */
    public List<FilterParameterReader> getFiltersParameters()
    {
        return this.filterParams;
    }
    
    /**
     * Gets the parameters for a single filter.
     * @param num the index of the filter
     * @return the filter parameters, or null if it does not exist.
     */
    public FilterParameterReader getFilterParameter(int num)
    {
        if(num >= 0 && num < this.filterParams.size())
        {
            return this.filterParams.get(num);
        }
        return null;
    }
}