/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.stop;


import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.ParametersReader;

import java.util.Map;

/**
 * Class for reading a filter for information diffusion.
 * For each filter, a single set of parameters is read.
 *
 * <br>
 *
 * File format:<br>
 * stop:<br>
 *     name: ...<br >
 *     params: ...<br >
 *      param1: ...<br >
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see ParametersReader
 */
public class StopConditionParameterReader extends ParametersReader
{
    /**
     * Identifier for the name of the algorithm.
     */
    private final static String NAME = "name";
    /**
     * Identifier for the set of parameters.
     */
    private final static String PARAMS = "params";


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
    public StopConditionParameterReader()
    {
        this.parameters = new Parameters();
        this.name = null;
    }

    /**
     * Reads the parameters of a stop condition.
     * @param element an entry containing the parameter information of the stop condition.
     */
    public void readStopCondition(Map<String, Object> element)
    {
        this.name = element.get(NAME).toString();

        if(element.get(PARAMS).getClass() == String.class)
        {
            this.parameters = new Parameters();
        }
        else
        {
            Map<String, Object> params = (Map<String, Object>) element.get(PARAMS);
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
     * Shows the configuration of a stop condition.
     * @return a string containing the configuration of the stop condition.
     */
    public String printStopCondition()
    {
        String stop = "";

        stop += this.getName() + "\n";

        stop += this.parameters.getBooleanValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        stop += this.parameters.getDoubleValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        stop += this.parameters.getIntegerValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        stop += this.parameters.getLongValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        stop += this.parameters.getStringValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        stop += this.parameters.getOrientationValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        return stop;
    }
}
