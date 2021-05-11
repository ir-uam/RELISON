/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics.distributions;


import es.uam.eps.ir.socialranksys.grid.ParametersReader;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the parameters for a metric
 * @author Javier Sanz-Cruzado Puig
 */
public class DistributionParamReader extends ParametersReader
{
    /**
     * Identifier for the name of the distribution
     */
    private final static String NAME = "name";
    /**
     * Identifier for the parameters
     */
    private final static String PARAM = "param";
    /**
     * Identifiers for the times
     */
    private final static String TIMES = "times";
    /**
     * Identifier for the time
     */
    private final static String TIME = "time";
    /**
     * Name of the selection mechanism
     */
    private String name;
    /**
     * Parameter values for the distribution.
     */
    private es.uam.eps.ir.socialranksys.grid.Parameters values;
    /**
     * Times in which to store the times
     */
    
    private List<Integer> times;
    /**
     * Reads the elements of a distribution
     * @param node the node containing the information for that selection mechanism.
     */
    public void readDistribution(Element node)
    {
        this.name = node.getElementsByTagName(NAME).item(0).getTextContent();
        
        // Obtain the parameters
        NodeList params = node.getElementsByTagName(PARAM);
        if(params == null || params.getLength() == 0)
        {
            this.values = new es.uam.eps.ir.socialranksys.grid.Parameters();
        }
        else
        {
            this.values = this.readParameterGrid(params);
        }
        
        NodeList timeList = node.getElementsByTagName(TIMES);
        if(timeList == null)
        {
            this.times = new ArrayList<>();
        }
        else
        {
            Element element = (Element) timeList.item(0);
            this.times = this.readTimesGrid(element);
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
     * @return the values of the parameters
     */
    public es.uam.eps.ir.socialranksys.grid.Parameters getParams()
    {
        return values;
    }
    
    /**
     * Obtains the list of iterations when the distribution will be written into
     * a file.
     * @param element An element containing the iterations.
     * @return the list of iterations.
     */
    private List<Integer> readTimesGrid(Element element)
    {
        List<Integer> timeList = new ArrayList<>();
        
        NodeList valueNodes = element.getElementsByTagName(TIME);
        
        if(valueNodes != null && valueNodes.getLength() > 0)
        {
            for(int i = 0; i < valueNodes.getLength(); ++i)
            {
                String value = valueNodes.item(i).getTextContent();
                timeList.add(Integer.parseInt(value));
            }
        }
        
        return timeList;
    }
    
    /**
     * Obtains the list of iterations when the distribution will be written into 
     * a file.
     * @return the list of iterations 
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
