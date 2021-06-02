/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.metrics;

import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.ParametersReader;

import java.util.Map;

/**
 * Class for reading the parameters for a diffusion metric.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MetricParameterReader extends ParametersReader
{
    /**
     * Name of the diffusion metric.
     */
    private String name;
    /**
     * Parameter values for the diffusion metric.
     */
    private Parameters values;

    /**
     * Reads a metric.
     * @param metric an entry representing the metric in the YAML file.
     */
    public void readMetric(Map.Entry<String, Object> metric)
    {
        this.name = metric.getKey();
        if(metric.getValue().getClass() == String.class)
        {
            this.values = new Parameters();
        }
        else
        {
            this.values = this.readParameterValues((Map<String, Object>) metric.getValue());
        }
    }

    /**
     * Obtains the name of the diffusion metric.
     * @return the name of the diffusion metric.
     */
    public String getName() 
    {
        return name;
    }

    /**
     * Obtains the values of the parameters of the diffusion metric.
     * @return the values of the parameters of the diffusion metric.
     */
    public Parameters getParams() 
    {
        return values;
    }
    
    /**
     * Shows the configuration of a metric.
     * @return a string containing the configuration of the diffusion metric.
     */
    public String printMetric() 
    {
        String selection = "";
        
        selection += this.getName() + "\n";
        
        selection += this.values.getBooleanValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        selection += this.values.getDoubleValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        selection += this.values.getIntegerValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        selection += this.values.getLongValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        selection += this.values.getStringValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        selection += this.values.getOrientationValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        return selection;
    }
    
}
