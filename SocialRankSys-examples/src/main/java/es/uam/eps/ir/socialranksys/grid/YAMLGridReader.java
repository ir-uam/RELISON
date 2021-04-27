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
import org.ranksys.formats.parsing.Parsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static es.uam.eps.ir.socialranksys.grid.BasicTypeIdentifiers.*;

/**
 * Grid for reading a configuration from a YAML file.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class YAMLGridReader
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
     * Given a map containing the different parameters to read, obtains a grid storing the different
     * possible values for each single parameter.
     * @param parameters a map, indexed by parameter name, containing the different values.
     * @return the configured grid.
     */
    protected Grid readParameterGrid(Map<String, Object> parameters)
    {
        Map<String, List<Double>> doubleValues = new HashMap<>();
        Map<String, List<EdgeOrientation>> orientationValues = new HashMap<>();
        Map<String, List<String>> stringValues = new HashMap<>();
        Map<String, List<Integer>> integerValues = new HashMap<>();
        Map<String, List<Boolean>> booleanValues = new HashMap<>();
        Map<String, List<Long>> longValues = new HashMap<>();
        Map<String, Map<String, Grid>> gridValues = new HashMap<>();

        for(Map.Entry<String, Object> param : parameters.entrySet())
        {
            // We first obtain the parameter name
            String paramName = param.getKey();
            Map<String, Object> paramSetting = (Map<String, Object>) param.getValue();
            // and the parameter type
            String type = paramSetting.get(TYPE).toString();

            // Depending on the type of the parameter, we obtain the corresponding value list for the given parameter.
            switch (type)
            {
                case INTEGER_TYPE -> integerValues.put(paramName, readIntegerGrid(paramSetting));
                case DOUBLE_TYPE -> doubleValues.put(paramName, readDoubleGrid(paramSetting));
                case STRING_TYPE -> stringValues.put(paramName, readStringGrid(paramSetting));
                case BOOLEAN_TYPE -> booleanValues.put(paramName, readBooleanGrid(paramSetting));
                case ORIENTATION_TYPE -> orientationValues.put(paramName, readOrientationGrid(paramSetting));
                case LONG_TYPE -> longValues.put(paramName, readLongGrid(paramSetting));
                case GRID_TYPE -> gridValues.put(paramName, readGridGrid(paramSetting));
                default -> System.err.println("ERROR: Unidentified type");
            }
        }

        return new Grid(doubleValues, orientationValues, stringValues, integerValues, booleanValues, longValues, gridValues);
    }

    /**
     * Obtains the integer values for a grid search.
     * @param map a map containing the possible values for the parameter.
     * @return the list of integer values.
     */
    protected List<Integer> readIntegerGrid(Map<String, Object> map)
    {
        List<Integer> values = new ArrayList<>();

        // First, we get the individual values:
        if(map.containsKey(VALUES))
        {
            Object value = map.get(VALUES);
            if(value.getClass() == String.class) // It is a single value:
            {
                int val = Parsers.ip.parse(value.toString());
                values.add(val);
            }
            else // It is a list of values
            {
                List<Object> list = (List<Object>) value;
                for(Object aux : list)
                {
                    int val = Parsers.ip.parse(aux.toString());
                    values.add(val);
                }
            }
        }

        // Then, we study the intervals:
        if(map.containsKey(RANGE))
        {
            List<Object> intervals = (List<Object>) map.get(RANGE);
            // For each interval:
            for(Object obj : intervals)
            {
                Map<String, Object> interval = (Map<String, Object>) obj;

                // If some value is missing, we ignore this interval.
                if(!interval.containsKey(START) || !interval.containsKey(END) || !interval.containsKey(STEP))
                {
                    continue;
                }

                int start = Parsers.ip.parse(interval.get(START).toString());
                int end = Parsers.ip.parse(interval.get(END).toString());
                int step = Parsers.ip.parse(interval.get(STEP).toString());

                for(int j = start; j <= end; j += step)
                {
                    values.add(j);
                }
                if(!values.contains(end))
                {
                    values.add(end);
                }
            }
        }

        return values;
    }

    /**
     * Reads long values from a grid
     * @param map a map containing the possible values for the parameter.
     * @return The list of long values
     */
    protected List<Long> readLongGrid(Map<String, Object> map)
    {
        List<Long> values = new ArrayList<>();

        // First, we get the individual values:
        if(map.containsKey(VALUES))
        {
            Object value = map.get(VALUES);
            if(value.getClass() == String.class) // It is a single value:
            {
                long val = Parsers.lp.parse(value.toString());
                values.add(val);
            }
            else // It is a list of values
            {
                List<Object> list = (List<Object>) value;
                for(Object aux : list)
                {
                    long val = Parsers.lp.parse(aux.toString());
                    values.add(val);
                }
            }
        }

        // Then, we study the intervals:
        if(map.containsKey(RANGE))
        {
            List<Object> intervals = (List<Object>) map.get(RANGE);
            // For each interval:
            for(Object obj : intervals)
            {
                Map<String, Object> interval = (Map<String, Object>) obj;

                // If some value is missing, we ignore this interval.
                if(!interval.containsKey(START) || !interval.containsKey(END) || !interval.containsKey(STEP))
                {
                    continue;
                }

                long start = Parsers.lp.parse(interval.get(START).toString());
                long end = Parsers.lp.parse(interval.get(END).toString());
                long step = Parsers.lp.parse(interval.get(STEP).toString());

                for(long j = start; j <= end; j += step)
                {
                    values.add(j);
                }
                if(!values.contains(end))
                {
                    values.add(end);
                }
            }
        }

        return values;
    }

    /**
     * Reads double values from a grid.
     * @param map a map containing the possible values for the parameter.
     * @return the list of double values.
     */
    protected List<Double> readDoubleGrid(Map<String, Object> map)
    {
        List<Double> values = new ArrayList<>();

        // First, we get the individual values:
        if(map.containsKey(VALUES))
        {
            Object value = map.get(VALUES);
            if(value.getClass() == String.class) // It is a single value:
            {
                double val = Parsers.dp.parse(value.toString());
                values.add(val);
            }
            else // It is a list of values
            {
                List<Object> list = (List<Object>) value;
                for(Object aux : list)
                {
                    double val = Parsers.dp.parse(aux.toString());
                    values.add(val);
                }
            }
        }

        // Then, we study the intervals:
        if(map.containsKey(RANGE))
        {
            List<Object> intervals = (List<Object>) map.get(RANGE);
            // For each interval:
            for(Object obj : intervals)
            {
                Map<String, Object> interval = (Map<String, Object>) obj;

                // If some value is missing, we ignore this interval.
                if(!interval.containsKey(START) || !interval.containsKey(END) || !interval.containsKey(STEP))
                {
                    continue;
                }

                double start = Parsers.dp.parse(interval.get(START).toString());
                double end = Parsers.dp.parse(interval.get(END).toString());
                double step = Parsers.dp.parse(interval.get(STEP).toString());

                for(double j = start; j <= end; j += step)
                {
                    values.add(j);
                }
                if(!values.contains(end))
                {
                    values.add(end);
                }
            }
        }

        return values;
    }

    /**
     * Reads string values from a grid
     * @param map a map containing the possible values for the parameter.
     * @return the list of string values.
     */
    protected List<String> readStringGrid(Map<String, Object> map)
    {
        List<String> values = new ArrayList<>();

        if(map.containsKey(VALUES))
        {
            Object value = map.get(VALUES);
            if(value.getClass() == String.class) // It is a single value:

            {
                String val = value.toString();
                values.add(val);
            }
            else // If it is a list of values
            {
                List<Object> list = (List<Object>) value;
                for(Object aux : list)
                {
                    // It is a single value:
                    String val = aux.toString();
                    values.add(val);
                }
            }
        }

        return values;
    }

    /**
     * Reads boolean values from a grid
     * @param map the map containing the values for the grid.
     * @return the list of boolean values.
     */
    protected List<Boolean> readBooleanGrid(Map<String, Object> map)
    {
        List<Boolean> values = new ArrayList<>();

        if(map.containsKey(VALUES))
        {
            Object value = map.get(VALUES);
            if(value.getClass() == String.class) // It is a single value:
            {
                String val = value.toString();
                values.add(val.equalsIgnoreCase("true"));
            }
            else // It is a list of values
            {
                List<Object> list = (List<Object>) value;
                for(Object aux : list)
                {
                    // It is a single value:
                    String val = aux.toString();
                    values.add(val.equalsIgnoreCase("true"));
                }
            }
        }

        return values;
    }

    /**
     * Reads a grid of grids.
     * @param map the map containing the values for the grid.
     * @return the list of grids.
     */
    protected Map<String, Grid> readGridGrid(Map<String, Object> map)
    {
        Map<String,Grid> values = new HashMap<>();

        Map<String, Object> grids = (Map<String, Object>) map.get(GRIDS);
        for(Map.Entry<String, Object> grid : grids.entrySet())
        {
            String name = grid.getKey();
            Grid defGrid = this.readParameterGrid((Map<String, Object>) grid.getValue());
            values.put(name, defGrid);
        }

        return values;
    }

    /**
     * Reads edge orientation values from a grid
     * @param map the map containing the values for the grid.
     * @return the list of edge orientation values.
     */
    protected List<EdgeOrientation> readOrientationGrid(Map<String, Object> map)
    {
        List<EdgeOrientation> values = new ArrayList<>();

        // First, we get the individual values:
        if(map.containsKey(VALUES))
        {
            Object value = map.get(VALUES);
            if(value.getClass() == String.class)
            {
                // It is a single value:
                String val = value.toString();
                values.add(EdgeOrientation.valueOf(val));
            }
            else
            {
                List<Object> list = (List<Object>) value;
                for(Object aux : list)
                {
                    // It is a single value:
                    String val = aux.toString();
                    values.add(EdgeOrientation.valueOf(val));
                }
            }
        }

        return values;
    }
}
