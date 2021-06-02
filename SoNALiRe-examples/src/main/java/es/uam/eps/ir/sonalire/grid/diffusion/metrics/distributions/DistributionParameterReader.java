/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.metrics.distributions;


import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.ParametersReader;
import org.ranksys.formats.parsing.Parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads the parameter for a distribution during information diffusion simulations.
 * It reads a single configuration for each distribution. In a file, it should look as:
 *
 * distrib_name:<br>
 *  params:<br >
 *      param1: ...<br >
 *  times:<br >
 *      values: [value1,value2,...,valueN] or value1<br>
 *      range:
 *      - start: start_val<br>
 *        end: end_val <br>
 *        step: steps_val <br>
 *      - start: ... <br>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DistributionParameterReader extends ParametersReader
{
    /**
     * Identifier for the parameters.
     */
    private final static String PARAMS = "params";
    /**
     * Identifiers for the times.
     */
    private final static String TIMES = "times";
    /**
     * Identifier for the time values.
     */
    private final static String VALUES = "values";

    private final static String RANGE = "range";
    private final static String START = "start";
    private final static String END = "end";
    private final static String STEP = "step";
    /**
     * Name of the selection mechanism.
     */
    private String name;
    /**
     * Parameter values for the distribution.
     */
    private Parameters values;
    /**
     * Times in which to store the times
     */
    private List<Integer> times;

    /**
     * Reads the elements of a distribution
     * @param node the node containing the information for that selection mechanism.
     */
    public void readDistribution(Map.Entry<String, Object> node)
    {
        this.name = node.getKey();

        if(node.getValue().getClass() == String.class)
        {
            this.values = new Parameters();
            this.times = new ArrayList<>();
        }
        else
        {
            Map<String, Object> distrib = (Map<String, Object>) node.getValue();
            if(distrib.containsKey(PARAMS) && distrib.get(PARAMS).getClass() != String.class)
            {
                Map<String, Object> params = (Map<String, Object>) distrib.get(PARAMS);
                this.readParameterValues(params);
            }
            else
            {
                this.values = new Parameters();
            }

            if(distrib.containsKey(TIMES) && distrib.get(TIMES).getClass() != String.class)
            {
                Map<String, Object> times = (Map<String, Object>) distrib.get(TIMES);
                this.readTimes(times);
            }
            else
            {
                this.times = new ArrayList<>();
            }
        }
    }

    /**
     * Obtains the list of iterations of the simulation when the distribution shall be stored in a file.
     * @param map a map containing the times to use.
     */
    private void readTimes(Map<String, Object> map)
    {
        this.times.clear();

        // First, we get the individual values:
        if(map.containsKey(VALUES))
        {
            Object value = map.get(VALUES);
            if(value.getClass() == String.class) // It is a single value:
            {
                int val = Parsers.ip.parse(value.toString());
                times.add(val);
            }
            else // It is a list of values
            {
                List<Object> list = (List<Object>) value;
                for(Object aux : list)
                {
                    int val = Parsers.ip.parse(aux.toString());
                    times.add(val);
                }
            }
        }

        // Then, we study the intervals:
        if(map.containsKey(RANGE))
        {
            List<Object> intervals = (List<Object>) map.get(RANGE);
            // For each interval:
            for(Object obj : intervals)
            {
                Map<String, Object> interval = (Map<String, Object>) obj;

                // If some value is missing, we ignore this interval.
                if(!interval.containsKey(START) || !interval.containsKey(END) || !interval.containsKey(STEP))
                {
                    continue;
                }

                int start = Parsers.ip.parse(interval.get(START).toString());
                int end = Parsers.ip.parse(interval.get(END).toString());
                int step = Parsers.ip.parse(interval.get(STEP).toString());

                for(int j = start; j <= end; j += step)
                {
                    times.add(j);
                }
                if(!times.contains(end))
                {
                    times.add(end);
                }
            }
        }
    }

    /**
     * Obtains the name of the selection mechanism.
     * @return the name of the selection mechanism.
     */
    public String getName() 
    {
        return name;
    }

    /**
     * Obtains the values of the parameters of the selection mechanism.
     * @return the values of the parameters.
     */
    public Parameters getParams() 
    {
        return values;
    }
    
    /**
     * Obtains the list of iterations when the distribution will be written into a file.
     * @return the list of iterations.
     */
    public List<Integer> getTimes()
    {
        return this.times;
    }
    
    /**
     * Shows the configuration of a distribution.
     * @return a string containing the configuration of the distribution.
     */
    public String printDistribution() 
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

        selection += "times:\n";
        selection += this.times.stream()
                .map(time -> "\t" + time + "\n")
                .reduce("", (x,y) -> x + y);
        
        return selection;
    }
    
}
