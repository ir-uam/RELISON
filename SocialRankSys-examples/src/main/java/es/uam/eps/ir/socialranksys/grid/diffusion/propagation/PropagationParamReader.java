/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.propagation;

import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.ParametersReader;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Reads the values for a propagation mechanism.
 * @author Javier Sanz-Cruzado Puig
 * @see es.uam.eps.socialranksys.diffusion.propagation
 */
public class PropagationParamReader extends ParametersReader
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
     * Name of the propagation mechanism.
     */
    private String name;
    /**
     * Parameter values for the propagation mechanism.
     */
    private Parameters values;
    
    /**
     * Reads the elements of a filter
     * @param node the node containing the information for that propagation mechanism.
     */
    public void readPropagation(Element node)
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
     * Obtains the name of the propagation mechanism.
     * @return the name of the propagation mechanism.
     */
    public String getName() 
    {
        return name;
    }

    /**
     * Obtains the values of the parameters of the propagation mechanism.
     * @return the values of the parameters
     */
    public Parameters getParams() {
        return values;
    }
    
    /**
     * Shows the configuration of a propagation mechanism.
     * @return a string containing the configuration of the propagation mechanism.
     */
    public String printPropagationMechanism() 
    {
        String propagation = "";
        
        propagation += this.getName() + "\n";
        
        propagation += this.values.getBooleanValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        propagation += this.values.getDoubleValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        propagation += this.values.getIntegerValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        propagation += this.values.getLongValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        propagation += this.values.getStringValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        propagation += this.values.getOrientationValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        return propagation;
    }
    
}
