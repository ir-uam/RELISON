/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.expiration;

import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.ParametersReader;

import java.util.Map;

/**
 * Class for reading an expiration mechanism for information diffusion.
 * For each mechanism, a single set of parameters is read.
 *
 * <br>
 *
 * File format:<br>
 * expiration:<br>
 *     name: selection_name<br>
 *     params: ...<br >
 *          param1:
 *          ...
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see ParametersReader
 */
public class ExpirationParameterReader extends ParametersReader
{
    /**
     * Identifier for the name of the mechanism
     */
    private final static String NAME = "name";
    /**
     * Identifier for the parameters
     */
    private final static String PARAMS = "params";
    /**
     * Name of the expiration mechanism
     */
    private String name;
    /**
     * Parameter values for the expiration mechanism.
     */
    private Parameters values;
    
    /**
     * Reads the parameters of a expiration mechanism
     * @param node the node containing the information for that expiration mechanism.
     */
    public void readExpiration(Map<String, Object> node)
    {
        this.name = node.get(NAME).toString();

        if(!node.containsKey(PARAMS) || node.get(PARAMS).getClass() == String.class)
        {
            this.values = new Parameters();
        }
        else
        {
            Map<String, Object> params = (Map<String, Object>) node.get(PARAMS);
            this.values = this.readParameterValues(params);
        }
    }

    /**
     * Obtains the name of the expiration mechanism.
     * @return the name of the expiration mechanism.
     */
    public String getName() 
    {
        return name;
    }

    /**
     * Obtains the values of the parameters of the expiration mechanism.
     * @return the values of the parameters
     */
    public Parameters getParams() 
    {
        return values;
    }
    
    /**
     * Shows the configuration of a expiration mechanism.
     * @return a string containing the configuration of the expiration mechanism.
     */
    public String printExpirationMechanism()
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
