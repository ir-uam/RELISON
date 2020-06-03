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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static es.uam.eps.ir.socialranksys.grid.BasicTypeIdentifiers.*;


/**
 * Reads grids.
 * @author Javier Sanz-Cruzado Puig
 */
public abstract class GridReader 
{
    /**
     * Identifier for the values of the parameter
     */
    private final static String VALUES = "values";
    
    private final static String GRIDS = "grids";
    private final static String GRID = "grid";
    /**
     * Identifier for the parameter name
     */
    private final static String NAME = "name";
    /**
     * Identifier for the parameter type
     */
    private final static String TYPE = "type";
    /**
     * Identifier for an individual parameter value
     */
    private final static String VALUE = "value";
    /**
     * Identifier for a range of values
     */
    private final static String RANGE = "range";
    /**
     * Identifier for the start of an interval
     */
    private final static String START = "start";
    /**
     * Identifier for the end of an interval
     */
    private final static String END = "end";
    /**
     * Identifier for the interval step
     */
    private final static String STEP = "step";
    /**
     * Identifier for the parameter list
     */
    private final static String PARAMS = "param";
    
    /**
     * Reads the possible values for the parameters of an algorithm.
     * @param parameters XML nodes containing the parameters information
     * @return The grid
     */
    protected Grid readParameterGrid(NodeList parameters)
    {
        Map<String, List<Double>> doubleValues = new HashMap<>();
        Map<String, List<EdgeOrientation>> orientationValues = new HashMap<>();
        Map<String, List<String>> stringValues = new HashMap<>();
        Map<String, List<Integer>> integerValues = new HashMap<>();
        Map<String, List<Boolean>> booleanValues = new HashMap<>();
        Map<String, List<Long>> longValues = new HashMap<>();
        Map<String, Map<String, Grid>> gridValues = new HashMap<>();
        
        for(int i = 0; i < parameters.getLength(); ++i)
        {
            Element element = (Element) parameters.item(i);
            String parameterName = element.getElementsByTagName(NAME).item(0).getTextContent();
            String type = element.getElementsByTagName(TYPE).item(0).getTextContent();
            
            switch (type) {
                case INTEGER_TYPE:
                {
                    List<Integer> grid = readIntegerGrid((Element) element.getElementsByTagName(VALUES).item(0));
                    integerValues.put(parameterName, grid);
                    break;
                }
                case DOUBLE_TYPE:
                {
                    List<Double> grid = readDoubleGrid((Element) element.getElementsByTagName(VALUES).item(0));
                    doubleValues.put(parameterName, grid);
                    break;
                }
                case STRING_TYPE:
                {
                    List<String> grid =  readStringGrid((Element) element.getElementsByTagName(VALUES).item(0));
                    stringValues.put(parameterName, grid);
                    break;
                }
                case BOOLEAN_TYPE:
                {
                    List<Boolean> grid = readBooleanGrid((Element) element.getElementsByTagName(VALUES).item(0));
                    booleanValues.put(parameterName, grid);
                    break;
                }
                case ORIENTATION_TYPE:
                {
                    List<EdgeOrientation> grid = readOrientationGrid((Element) element.getElementsByTagName(VALUES).item(0));
                    orientationValues.put(parameterName, grid);
                    break;
                }
                case LONG_TYPE:
                {
                    List<Long> grid = readLongGrid((Element) element.getElementsByTagName(VALUES).item(0));
                    longValues.put(parameterName, grid);
                    break;
                }
                case GRID_TYPE:
                {
                    Map<String, Grid> grid = readGridGrid((Element) element.getElementsByTagName(GRIDS).item(0));
                    gridValues.put(parameterName, grid);
                    break;
                }
                default:
                {
                    System.err.println("Unidentified type " + type);
                    break;
                }
            }
        }        
        return new Grid(doubleValues, orientationValues, stringValues, integerValues, booleanValues, longValues, gridValues);
        
    }
    
    
    
    /**
     * Reads integer values from a grid
     * @param element XML element containing the different possible values for an integer attribute
     * @return The list of integer values
     */
    protected List<Integer> readIntegerGrid(Element element)
    {
        List<Integer> values = new ArrayList<>();
        
        //Case 1: Values
        NodeList valuesNodes = element.getElementsByTagName(VALUE);
        if(valuesNodes.getLength() > 0)
        {
            for(int i = 0; i < valuesNodes.getLength(); ++i)
            {
                String value = valuesNodes.item(i).getTextContent();
                values.add(Integer.valueOf(value));
            }
        }
        
        //Case 2: Ranges
        NodeList rangesNodes = element.getElementsByTagName(RANGE);
        if(rangesNodes.getLength() > 0)
        {
            for(int i = 0; i < rangesNodes.getLength(); ++i)
            {
                Element range = (Element) rangesNodes.item(i);
                int start = Integer.parseInt(range.getElementsByTagName(START).item(0).getTextContent());
                int end = Integer.parseInt(range.getElementsByTagName(END).item(0).getTextContent());
                int step = Integer.parseInt(range.getElementsByTagName(STEP).item(0).getTextContent());
                
                for(int j = start; j <= end; j += step)
                {
                    values.add(j);
                }
            }
        }
        
        return values;
    }

   /**
     * Reads long values from a grid
     * @param element XML element containing the different possible values for a long attribute
     * @return The list of long values
     */
    protected List<Long> readLongGrid(Element element)
    {
        List<Long> values = new ArrayList<>();
        
        //Case 1: Values
        NodeList valuesNodes = element.getElementsByTagName(VALUE);
        if(valuesNodes.getLength() > 0)
        {
            for(int i = 0; i < valuesNodes.getLength(); ++i)
            {
                String value = valuesNodes.item(i).getTextContent();
                values.add(Long.valueOf(value));
            }
        }
        
        //Case 2: Ranges
        NodeList rangesNodes = element.getElementsByTagName(RANGE);
        if(rangesNodes.getLength() > 0)
        {
            for(int i = 0; i < rangesNodes.getLength(); ++i)
            {
                Element range = (Element) rangesNodes.item(i);
                long start = Long.parseLong(range.getElementsByTagName(START).item(0).getTextContent());
                long end = Long.parseLong(range.getElementsByTagName(END).item(0).getTextContent());
                long step = Long.parseLong(range.getElementsByTagName(STEP).item(0).getTextContent());
                
                for(long j = start; j <= end; j += step)
                {
                    values.add(j);
                }
            }
        }
        
        return values;
    }
    
   /**
     * Reads double values from a grid
     * @param element XML element containing the different possible values for a double attribute
     * @return The list of double values
     */
    protected List<Double> readDoubleGrid(Element element)
    {
        List<Double> values = new ArrayList<>();
        
        //Case 1: Values
        NodeList valuesNodes = element.getElementsByTagName(VALUE);
        if(valuesNodes.getLength() > 0)
        {
            for(int i = 0; i < valuesNodes.getLength(); ++i)
            {
                String value = valuesNodes.item(i).getTextContent();
                values.add(Double.parseDouble(value));
            }
        }
        
        //Case 2: Ranges
        NodeList rangesNodes = element.getElementsByTagName(RANGE);
        if(rangesNodes.getLength() > 0)
        {
            for(int i = 0; i < rangesNodes.getLength(); ++i)
            {
                Element range = (Element) rangesNodes.item(i);
                double start = Double.parseDouble(range.getElementsByTagName(START).item(0).getTextContent());
                double end = Double.parseDouble(range.getElementsByTagName(END).item(0).getTextContent());
                double step = Double.parseDouble(range.getElementsByTagName(STEP).item(0).getTextContent());
                
                for(double j = start; j <= end; j += step)
                {
                    values.add(j);
                }
                
                if(!values.contains(end))
                    values.add(end);
            }
        }
        
        return values;
    }

    /**
     * Reads string values from a grid
     * @param element XML element containing the different possible values for a string attribute
     * @return The list of string values
     */
    protected List<String> readStringGrid(Element element)
    {
        List<String> values = new ArrayList<>();
        
        //Case 1: Values
        NodeList valuesNodes = element.getElementsByTagName(VALUE);
        if(valuesNodes.getLength() > 0)
        {
            for(int i = 0; i < valuesNodes.getLength(); ++i)
            {
                String value = valuesNodes.item(i).getTextContent();
                values.add(value);
            }
        }
        
        return values;
    }

    /**
     * Reads boolean values from a grid
     * @param element XML element containing the different possible values for a boolean attribute
     * @return The list of boolean values
     */
    protected List<Boolean> readBooleanGrid(Element element)
    {
        List<Boolean> values = new ArrayList<>();
        
        //Case 1: Values
        NodeList valuesNodes = element.getElementsByTagName(VALUE);
        if(valuesNodes.getLength() > 0)
        {
            for(int i = 0; i < valuesNodes.getLength(); ++i)
            {
                String value = valuesNodes.item(i).getTextContent();
                values.add(value.equalsIgnoreCase("true"));
            }
        }
        
        return values;
    }
    
    /**
     * Reads a grid of grids.
     * @param element XML element containing the different possible values for a Grid.
     * @return The list of grids.
     */
    protected Map<String, Grid> readGridGrid(Element element)
    {
        Map<String,Grid> values = new HashMap<>();
        NodeList valuesNodes = element.getElementsByTagName(GRID);
        if(valuesNodes.getLength() > 0)
        {
            for(int i = 0; i < valuesNodes.getLength(); ++i)
            {
                Element value = (Element) valuesNodes.item(i);
                String name = value.getElementsByTagName(NAME).item(0).getTextContent();
                NodeList params = value.getElementsByTagName(PARAMS);
                Grid grid = this.readParameterGrid(params);
                values.put(name, grid);            
            }
        }
        return values;
        
    }
    
    /**
     * Reads edge orientation values from a grid
     * @param element XML element containing the different possible values for an orientation attribute
     * @return The list of edge orientation values
     */
    protected List<EdgeOrientation> readOrientationGrid(Element element)
    {
        List<EdgeOrientation> values = new ArrayList<>();
        
        //Case 1: Values
        NodeList valuesNodes = element.getElementsByTagName(VALUE);
        if(valuesNodes.getLength() > 0)
        {
            for(int i = 0; i < valuesNodes.getLength(); ++i)
            {
                String value = valuesNodes.item(i).getTextContent();
                values.add(EdgeOrientation.valueOf(value));
            }
        }
        
        return values;
    }
}
