/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.update;

import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.ParametersReader;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Reads the values for an update mechanism
 * @author Javier Sanz-Cruzado Puig
 */
public class UpdateParamReader extends ParametersReader
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
     * Name of the update mechanism
     */
    private String name;
    /**
     * Parameter values for the update mechanism.
     */
    private Parameters values;
    
    /**
     * Reads the elements of a update mechanism.
     * @param node the node containing the information for that update mechanism.
     */
    public void readUpdate(Element node)
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
     * Obtains the name of the update mechanism.
     * @return the name of the update mechanism.
     */
    public String getName() 
    {
        return name;
    }

    /**
     * Obtains the values of the parameters of the update mechanism.
     * @return the values of the parameters
     */
    public Parameters getParams() {
        return values;
    }
    
    /**
     * Shows the configuration of a update mechanism.
     * @return a string containing the configuration of the update mechanism.
     */
    public String printUpdateMechanism() 
    {
        String update = "";
        
        update += this.getName() + "\n";
        
        update += this.values.getBooleanValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        update += this.values.getDoubleValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        update += this.values.getIntegerValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        update += this.values.getLongValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        update += this.values.getStringValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        update += this.values.getOrientationValues()
                .entrySet()
                .stream()
                .map(entry -> "\t\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        return update;
    }
    
}
