/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.stop;

import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.ParametersReader;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Reads the values for a stop condition
 * @author Javier Sanz-Cruzado Puig
 */
public class StopConditionParamReader extends ParametersReader
{
    /**
     * Identifier for the name of the mechanism
     */
    private final static String NAME = "name";
    /**
     * Identifier for the parameters
     */
    private final static String PARAM = "param";
    /**
     * Name of the stop condition
     */
    private String name;
    /**
     * Parameter values for the stop condition.
     */
    private Parameters values;
    
    /**
     * Reads the elements of a stop condition
     * @param node the node containing the information for that stop.
     */
    public void readStopCondition(Element node)
    {
        this.name = node.getElementsByTagName(NAME).item(0).getTextContent();
        
        NodeList params = node.getElementsByTagName(PARAM);
        if(params == null || params.getLength() == 0)
        {
            this.values = new Parameters();
        }
        else
        {
            this.values = this.readParameterGrid(params);
        }
    }

    /**
     * Gets the name of the stop condition.
     * @return The name of the stop condition.
     */
    public String getName() 
    {
        return name;
    }

    /**
     * Gets the parameters for the stop condition.
     * @return The parameters for the stop condition.
     */
    public Parameters getParams() {
        return values;
    }

    /**
     * Shows the configuration of a stop condition.
     * @return a string containing the configuration of the stop condition.
     */
    public String printStopCondition() 
    {
        String stop = "";
        
        stop += this.getName() + "\n";
        
        stop += this.values.getBooleanValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        stop += this.values.getDoubleValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        stop += this.values.getIntegerValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        stop += this.values.getLongValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        stop += this.values.getStringValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        stop += this.values.getOrientationValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        return stop;
    }
    
}
