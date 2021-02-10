/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion;

import es.uam.eps.ir.socialranksys.grid.diffusion.filter.FilterParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.distributions.DistributionParamReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the grids for simulation metrics.
 * @author Javier Sanz-Cruzado Puig
 */
public class SimulationMetricsParamReader 
{
    /**
     * Identifier for the metrics list
     */
    private final static String METRICS = "metrics";
        /**
     * Identifier for the metric
     */
    private final static String METRIC = "metric";
    /**
     * Identifier for the distributions list
     */
    private final static String DISTRIBUTIONS = "distributions";
    /**
     * Identifier for the distribution
     */
    private final static String DISTRIBUTION = "distribution";
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
    private final List<FilterParamReader> filterParams;
    
    /**
     * Parameters for the different metrics to compute.
     */
    private final List<MetricParamReader> metricParams;
    /**
     * Parameters for the different distributions to compute.
     */
    private final List<DistributionParamReader> distribParams;
    /**
     * The name of the file
     */
    private final String file;
    
    
    /**
     * Constructor
     * @param file File that contains the grid data 
     */
    public SimulationMetricsParamReader(String file)
    {
        this.file = file;
        this.filterParams = new ArrayList<>();
        this.metricParams = new ArrayList<>();
        this.distribParams = new ArrayList<>();
    }
    
    
    /**
     * Reads a XML document containing a grid
     */
    public void readDocument()
    {
        filterParams.clear();
        metricParams.clear();
        distribParams.clear();
        
        try
        {
            // First of all, obtain the XML Document
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            
            Element parent = doc.getDocumentElement();
            parent.normalize();
                        
            NodeList metricsList = parent.getElementsByTagName(METRICS);
            Node node = metricsList.item(0);
            this.readMetrics((Element) node);
            
            NodeList distributionList = parent.getElementsByTagName(DISTRIBUTIONS);
            node = distributionList.item(0);
            this.readDistributions((Element) node);
            
            // Read the data filter
            NodeList filtersElem = parent.getElementsByTagName(FILTERS);
            node = filtersElem.item(0);
            this.readFilters((Element) node);
        } 
        catch (ParserConfigurationException | SAXException | IOException ex)
        {
            ex.printStackTrace();
        }
    }
    
    
    /**
     * Reads the configuration parameters for a list of information propagation metrics.
     * @param node The node containing the list of metrics.
     */
    private void readMetrics(Element node) 
    {
        NodeList metricsList = node.getElementsByTagName(METRIC);
        for(int i = 0; i < metricsList.getLength(); ++i)
        {
            Node metricElem = metricsList.item(i);
            MetricParamReader mpr = new MetricParamReader();
            mpr.readMetric((Element) metricElem);
            this.metricParams.add(mpr);
        }
    }
    
    /**
     * Reads the configuration parameters for a list of information propagation distributions.
     * @param node The node containing the list of metrics.
     */
    private void readDistributions(Element node) 
    {
        NodeList metricsList = node.getElementsByTagName(DISTRIBUTION);
        for(int i = 0; i < metricsList.getLength(); ++i)
        {
            Node metricElem = metricsList.item(i);
            DistributionParamReader dpr = new DistributionParamReader();
            dpr.readDistribution((Element) metricElem);
            this.distribParams.add(dpr);
        }
    }
    
    /**
     * Reads the configuration parameters for a list of information propagation distributions.
     * @param node The node containing the list of metrics.
     */
    private void readFilters(Element node) 
    {
        NodeList metricsList = node.getElementsByTagName(FILTER);

        List<FilterParamReader> fprs = new ArrayList<>();
        for(int i = 0; i < metricsList.getLength(); ++i)
        {
            Node metricElem = metricsList.item(i);
            FilterParamReader fpr = new FilterParamReader();
            fpr.readFilter((Element) metricElem);
            this.filterParams.add(fpr);
        }
    }
    
    /**
     * Gets the parameters for all the different metrics.
     * @return the list of metric parameters.
     */
    public List<MetricParamReader> getMetricsParameters()
    {
        return this.metricParams;
    }
    
    /**
     * Gets the parameters for a single metric.
     * @param num the index of the metric
     * @return the metric parameters, or null if it does not exist.
     */
    public MetricParamReader getMetricParameters(int num)
    {
        if(num >= 0 && num < this.metricParams.size())
        {
            return this.metricParams.get(num);
        }
        return null;
    }
    
    /**
     * Gets the parameters for all the different distributions.
     * @return the list of distribution parameters.
     */
    public List<DistributionParamReader> getDistributionsParameters()
    {
        return this.distribParams;
    }
    
    /**
     * Gets the parameters for a single distribution.
     * @param num the index of the distribution
     * @return the distribution parameters, or null if it does not exist.
     */
    public DistributionParamReader getDistributionParameters(int num)
    {
        if(num >= 0 && num < this.distribParams.size())
        {
            return this.distribParams.get(num);
        }
        return null;
    }
    
    /**
     * Gets the filter parameters for a given simulation.
     * @return the filter parameters.
     */
    public List<FilterParamReader> getFiltersParameters()
    {
        return this.filterParams;
    }
    
    /**
     * Gets the parameters for a single filter.
     * @param num the index of the filter
     * @return the filter parameters, or null if it does not exist.
     */
    public FilterParamReader getFilterParameter(int num)
    {
        if(num >= 0 && num < this.filterParams.size())
        {
            return this.filterParams.get(num);
        }
        return null;
    }
    

}