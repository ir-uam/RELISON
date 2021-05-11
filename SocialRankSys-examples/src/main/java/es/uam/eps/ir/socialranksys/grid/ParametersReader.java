/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import org.ranksys.formats.parsing.Parsers;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

import static es.uam.eps.ir.socialranksys.grid.BasicTypeIdentifiers.*;
import static org.w3c.dom.Node.ELEMENT_NODE;

/**
 * Reads parameters.
 * @author Javier Sanz-Cruzado Puig
 */
public abstract class ParametersReader 
{
    /**
     * Identifier for the values.
     */
    private final static String VALUE = "value";
    /**
     * Identifier for the name of a parameter.
     */
    private final static String NAME = "name";
    /**
     * Identifier for the type of a parameter.
     */
    private final static String TYPE = "type";
    /**
     * Identifier for the parameters.
     */
    private final static String PARAMS = "params";
    /**
     * Identifier for an individual parameter.
     */
    private final static String PARAM = "param";
    /**
     * Identifier for the grid.
     */
    private final static String GRID = "grid";
    
    /**
     * Reads the possible values for the parameters of an algorithm.
     * @param parameters XML nodes containing the parameters information
     * @return The grid
     */
    protected Parameters readParameterGrid(NodeList parameters)
    {
        Map<String, Double> doubleValues = new HashMap<>();
        Map<String, EdgeOrientation> orientationValues = new HashMap<>();
        Map<String, String> stringValues = new HashMap<>();
        Map<String, Integer> integerValues = new HashMap<>();
        Map<String, Boolean> booleanValues = new HashMap<>();
        Map<String, Long> longValues = new HashMap<>();
        Map<String, Tuple2oo<String,Parameters>> recursiveValues = new HashMap<>();
        
        for(int i = 0; i < parameters.getLength(); ++i)
        {
            if(parameters.item(i).getNodeType() == ELEMENT_NODE)
            {
                Element element = (Element) parameters.item(i);
                String parameterName = element.getElementsByTagName(NAME).item(0).getTextContent();
                String type = element.getElementsByTagName(TYPE).item(0).getTextContent();

                switch (type) {
                    case INTEGER_TYPE:
                    {
                        Integer value = readIntegerGrid((Element) element.getElementsByTagName(VALUE).item(0));
                        integerValues.put(parameterName, value);
                        break;
                    }
                    case DOUBLE_TYPE:
                    {
                        Double value = readDoubleGrid((Element) element.getElementsByTagName(VALUE).item(0));
                        doubleValues.put(parameterName, value);
                        break;
                    }
                    case STRING_TYPE:
                    {
                        String value =  readStringGrid((Element) element.getElementsByTagName(VALUE).item(0));
                        stringValues.put(parameterName, value);
                        break;
                    }
                    case BOOLEAN_TYPE:
                    {
                        Boolean value = readBooleanGrid((Element) element.getElementsByTagName(VALUE).item(0));
                        booleanValues.put(parameterName, value);
                        break;
                    }
                    case ORIENTATION_TYPE:
                    {
                        EdgeOrientation value = readOrientationGrid((Element) element.getElementsByTagName(VALUE).item(0));
                        orientationValues.put(parameterName, value);
                        break;
                    }
                    case LONG_TYPE:
                    {
                        Long value = readLongGrid((Element) element.getElementsByTagName(VALUE).item(0));
                        longValues.put(parameterName, value);
                        break;
                    }
                    case OBJECT_TYPE:
                    {
                        Tuple2oo<String,Parameters> params = readParametersGrid((Element) element.getElementsByTagName(VALUE).item(0));
                        recursiveValues.put(parameterName, params);
                        break;
                    }
                    default:
                    {
                        System.err.println("Unidentified type " + type);
                        break;
                    }
                }
            }
        }        
        return new Parameters(doubleValues, orientationValues, stringValues, integerValues, booleanValues, longValues, recursiveValues);
        
    }

    /**
     * Reads an integer value from the parameters file.
     * @param element XML element containing the value for the attribute
     * @return The integer value
     */
    protected Integer readIntegerGrid(Element element)
    {
        String value = element.getTextContent();
        return Parsers.ip.parse(value);
    }

    /**
     * Reads a long value from the parameters file.
     * @param element XML element containing the value for the attribute
     * @return The long value
     */
    protected Long readLongGrid(Element element)
    {
        String value = element.getTextContent();
        return Parsers.lp.parse(value);
    }
    
    /**
     * Reads a long value from the parameters file.
     * @param element XML element containing the value for the attribute
     * @return The long value
     */
    protected Double readDoubleGrid(Element element)
    {
        String value = element.getTextContent();
        return Parsers.dp.parse(value);
    }
    
    /**
     * Reads a string value from the parameters file.
     * @param element XML element containing the value for the attribute
     * @return The string value
     */
    protected String readStringGrid(Element element)
    {
        return element.getTextContent();
    }
    
    /**
     * Reads a boolean value from the parameters file.
     * @param element XML element containing the value for the attribute
     * @return The boolean value
     */
    protected Boolean readBooleanGrid(Element element)
    {
        String value = element.getTextContent();
        return value.equalsIgnoreCase("true");
    }
    
    /**
     * Reads an orientation value from the parameters file.
     * @param element XML element containing the value for the attribute
     * @return The orientation value
     */
    protected EdgeOrientation readOrientationGrid(Element element)
    {
        String value = element.getTextContent();
        return EdgeOrientation.valueOf(value);
    }

    protected Tuple2oo<String,Parameters> readParametersGrid(Element element) 
    {
        NodeList valuesNodes = element.getElementsByTagName(GRID);
        Element value = (Element) valuesNodes.item(0);
        String name = value.getElementsByTagName(NAME).item(0).getTextContent();
        NodeList params = value.getElementsByTagName(PARAM);
        Parameters parameters = this.readParameterGrid(params);
        return new Tuple2oo<>(name, parameters);
    }
}
