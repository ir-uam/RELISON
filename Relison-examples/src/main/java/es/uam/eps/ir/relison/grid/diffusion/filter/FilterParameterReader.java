/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.filter;


import es.uam.eps.ir.relison.grid.Parameters;
import es.uam.eps.ir.relison.grid.ParametersReader;

import java.util.Map;

/**
 * Class for reading a filter for information diffusion.
 * For each filter, a single set of parameters is read.
 *
 * <br>
 *
 * File format:<br>
 * filter:<br>
 *     param1: ...<br >
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see ParametersReader
 */
public class FilterParameterReader extends ParametersReader
{
    /**
     * The set of parameters for the filter.
     */
    private Parameters parameters;

    /**
     * The name of the filter.
     */
    private String name;

    /**
     * Constructor.
     */
    public FilterParameterReader()
    {
        this.parameters = new Parameters();
        this.name = null;
    }
    
    /**
     * Reads the parameters of a filter.
     * @param element an entry containing the parameter information of the filter.
     */
    public void readFilter(Map.Entry<String,Object> element)
    {
        this.name = element.getKey();

        if(element.getValue().getClass() == String.class)
        {
            this.parameters = new Parameters();
        }
        else
        {
            Map<String, Object> params = (Map<String, Object>) element.getValue();
            this.parameters = readParameterValues(params);
        }
    }

    /**
     * Gets the name of the filter.
     * @return the name of the filter, if it exists, null otherwise.
     */
    public String getName()
    {
        return this.name;
    }
    
    /**
     * Gets the parameters for the read filter.
     * @return the parameters if they exist, an empty parameter object if not.
     */
    public Parameters getParameter()
    {
        return this.parameters;
    }

    /**
     * Shows the configuration of a filter.
     * @return a string containing the configuration of the filter.
     */
    public String printFilter()
    {
        String filter = "";

        filter += this.getName() + "\n";

        filter += this.parameters.getBooleanValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        filter += this.parameters.getDoubleValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        filter += this.parameters.getIntegerValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        filter += this.parameters.getLongValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        filter += this.parameters.getStringValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        filter += this.parameters.getOrientationValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        return filter;
    }

}
