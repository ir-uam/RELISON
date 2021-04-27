/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
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

import java.util.HashMap;
import java.util.Map;

import static es.uam.eps.ir.socialranksys.grid.BasicTypeIdentifiers.*;

/**
 * Reads parameters from a YAML file.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class YAMLParametersReader
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
     * Reads the values of the parameters for a single algorithm, metric, etc.
     * @param parameters a map, indexed by parameter name, containing the values for those parameters.
     * @return the parameter configuration.
     */
    protected Parameters readParameterValues(Map<String, Object> parameters)
    {
        Map<String, Double> doubleValues = new HashMap<>();
        Map<String, EdgeOrientation> orientationValues = new HashMap<>();
        Map<String, String> stringValues = new HashMap<>();
        Map<String, Integer> integerValues = new HashMap<>();
        Map<String, Boolean> booleanValues = new HashMap<>();
        Map<String, Long> longValues = new HashMap<>();
        Map<String, Tuple2oo<String,Parameters>> recursiveValues = new HashMap<>();

        for(Map.Entry<String, Object> param : parameters.entrySet())
        {
            // Obtain the parameter name
            String paramName = param.getKey();
            Map<String, Object> paramSetting = (Map<String, Object>) param.getValue();
            // and the parameter type
            String type = paramSetting.get(TYPE).toString();

            switch (type)
            {
                case INTEGER_TYPE -> integerValues.put(paramName, readInteger(paramSetting));
                case DOUBLE_TYPE -> doubleValues.put(paramName, readDouble(paramSetting));
                case STRING_TYPE -> stringValues.put(paramName, readString(paramSetting));
                case BOOLEAN_TYPE -> booleanValues.put(paramName, readBoolean(paramSetting));
                case ORIENTATION_TYPE -> orientationValues.put(paramName, readOrientation(paramSetting));
                case LONG_TYPE -> longValues.put(paramName, readLong(paramSetting));
                case GRID_TYPE -> recursiveValues.put(paramName, readParameters(paramSetting));
                default -> System.err.println("ERROR: Unidentified type");
            }
        }

        return new Parameters(doubleValues, orientationValues, stringValues, integerValues, booleanValues, longValues, recursiveValues);
        
    }

    /**
     * Reads an integer value from the parameters file.
     * @param map a map containing the value of the parameter.
     * @return the integer value.
     */
    protected Integer readInteger(Map<String, Object> map)
    {
        String value = map.get(VALUE).toString();
        return Parsers.ip.parse(value);
    }

    /**
     * Reads an long value from the parameters file.
     * @param map a map containing the value of the parameter.
     * @return the long value.
     */
    protected Long readLong(Map<String, Object> map)
    {
        String value = map.get(VALUE).toString();
        return Parsers.lp.parse(value);
    }

    /**
     * Reads a double value from the parameters file.
     * @param map a map containing the value of the parameter.
     * @return the double value.
     */
    protected Double readDouble(Map<String, Object> map)
    {
        String value = map.get(VALUE).toString();
        return Parsers.dp.parse(value);
    }

    /**
     * Reads a string value from the parameters file.
     * @param map a map containing the value of the parameter.
     * @return the string value.
     */
    protected String readString(Map<String, Object> map)
    {
        return map.get(VALUE).toString();
    }

    /**
     * Reads a boolean value from the parameters file.
     * @param map a map containing the value of the parameter.
     * @return the boolean value.
     */
    protected Boolean readBoolean(Map<String, Object> map)
    {
        String value = map.get(VALUE).toString();
        return value.equalsIgnoreCase("true");
    }

    /**
     * Reads an edge orientation value from the parameters file.
     * @param map a map containing the value of the parameter.
     * @return the edge orientation value.
     */
    protected EdgeOrientation readOrientation(Map<String, Object> map)
    {
        String value = map.get(VALUE).toString();
        return EdgeOrientation.valueOf(value);
    }

    /**
     * Reads the parameters for the internal elements of the algorithm, metric, etc.
     * @param map a map containing the value of the parameter.
     * @return a tuple containing the name of the internal element, and its parameters.
     */
    protected Tuple2oo<String,Parameters> readParameters(Map<String, Object> map)
    {
        Map<String, Object> grid = (Map<String, Object>) map.get(GRID);
        String name = grid.get(NAME).toString();
        Map<String, Object> params = (Map<String, Object>) map.get(PARAMS);
        Parameters parameters = this.readParameterValues(params);
        return new Tuple2oo<>(name, parameters);
    }
}
