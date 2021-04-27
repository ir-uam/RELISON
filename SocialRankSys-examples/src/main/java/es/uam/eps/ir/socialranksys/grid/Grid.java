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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A grid containing all the possible values for the different parameters of
 * an algorithm, metric, etc.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Grid
{
    /**
     * Integer values for the different parameters
     */
    private final Map<String, List<Integer>> integerValues;
    /**
     * Long values for the parameters.
     */
    private final Map<String, List<Long>> longValues;
    /**
     * Double values for the different parameters
     */
    private final Map<String, List<Double>> doubleValues;

    /**
     * Boolean values for the different parameters.
     */
    private final Map<String, List<Boolean>> booleanValues;
    /**
     * Edge orientation values for the different parameters
     */
    private final Map<String, List<EdgeOrientation>> orientationValues;
    /**
     * String values for the different parameters
     */
    private final Map<String, List<String>> stringValues;

    /**
     * Grid values for the parameters.
     */
    private final Map<String, Map<String, Grid>> gridValues;

    /**
     * Constructor.
     * @param doubleValues      double values for the different parameters.
     * @param orientationValues edge orientation values for the different parameters.
     * @param stringValues      string values for the different parameters.
     * @param integerValues     integer values for the different parameters.
     * @param booleanValues     boolean values for the different parameters.
     * @param longValues        long values for the parameters.
     * @param gridValues        grid values for the parameters.
     */
    public Grid(Map<String, List<Double>> doubleValues, Map<String, List<EdgeOrientation>> orientationValues, Map<String, List<String>> stringValues, Map<String, List<Integer>> integerValues, Map<String, List<Boolean>> booleanValues, Map<String, List<Long>> longValues, Map<String, Map<String, Grid>> gridValues)
    {
        this.doubleValues = doubleValues;
        this.orientationValues = orientationValues;
        this.stringValues = stringValues;
        this.integerValues = integerValues;
        this.booleanValues = booleanValues;
        this.longValues = longValues;
        this.gridValues = gridValues;
    }

    /**
     * Empty constructor.
     */
    public Grid()
    {
        doubleValues = new HashMap<>();
        orientationValues = new HashMap<>();
        stringValues = new HashMap<>();
        integerValues = new HashMap<>();
        booleanValues = new HashMap<>();
        longValues = new HashMap<>();
        gridValues = new HashMap<>();
    }

    /**
     * Obtains the integer values for the different parameters.
     * @return a map, indexed by parameter name, containing the possible integer values for the parameter.
     */
    public Map<String, List<Integer>> getIntegerValues()
    {
        return integerValues;
    }

    /**
     * Obtains the long values for the different parameters.
     * @return a map, indexed by parameter name, containing the possible long values for the parameter.
     */
    public Map<String, List<Long>> getLongValues()
    {
        return longValues;
    }

    /**
     * Obtains the double values for the different parameters.
     * @return a map, indexed by parameter name, containing the possible double values for the parameter.
     */
    public Map<String, List<Double>> getDoubleValues()
    {
        return doubleValues;
    }

    /**
     * Obtains the boolean values for the different parameters.
     * @return a map, indexed by parameter name, containing the possible boolean values for the parameter.
     */
    public Map<String, List<Boolean>> getBooleanValues()
    {
        return booleanValues;
    }

    /**
     * Obtains the edge orientation values for the different parameters.
     * @return a map, indexed by parameter name, containing the edge orientation values for the parameter.
     */
    public Map<String, List<EdgeOrientation>> getOrientationValues()
    {
        return orientationValues;
    }

    /**
     * Obtains the string values for the different parameters.
     * @return a map, indexed by parameter name, containing the string values for the parameter.
     */
    public Map<String, List<String>> getStringValues()
    {
        return stringValues;
    }

    /**
     * Obtains the internal grids for the different parameters.
     * @return a map, indexed by parameter name, containing the internal grids for the parameter.
     */
    public Map<String, Map<String, Grid>> getGridValues()
    {
        return gridValues;
    }

    /**
     * Gets the integer values for a single parameter.
     * @param paramName the name of the parameter.
     * @return a list containing the values for that parameter.
     */
    public List<Integer> getIntegerValues(String paramName)
    {
        return this.integerValues.getOrDefault(paramName, new ArrayList<>());
    }

    /**
     * Gets the long values for a single parameter.
     * @param paramName the name of the parameter.
     * @return a list containing the values for that parameter.
     */
    public List<Long> getLongValues(String paramName)
    {
        return this.longValues.getOrDefault(paramName, new ArrayList<>());
    }

    /**
     * Gets the double values for a single parameter.
     * @param paramName the name of the parameter.
     * @return a list containing the values for that parameter.
     */
    public List<Double> getDoubleValues(String paramName)
    {
        return this.doubleValues.getOrDefault(paramName, new ArrayList<>());
    }

    /**
     * Gets the boolean values for a single parameter.
     * @param paramName the name of the parameter.
     * @return a list containing the values for that parameter.
     */
    public List<Boolean> getBooleanValues(String paramName)
    {
        return this.booleanValues.getOrDefault(paramName, new ArrayList<>());
    }

    /**
     * Gets the edge orientation values for a single parameter.
     * @param paramName the name of the parameter.
     * @return a list containing the values for that parameter.
     */
    public List<EdgeOrientation> getOrientationValues(String paramName)
    {
        return this.orientationValues.getOrDefault(paramName, new ArrayList<>());
    }

    /**
     * Gets the string values for a single parameter.
     * @param paramName the name of the parameter.
     * @return a list containing the values for that parameter.
     */
    public List<String> getStringValues(String paramName)
    {
        return this.stringValues.getOrDefault(paramName, new ArrayList<>());
    }

    /**
     * Gets the internal grids for a single parameter.
     * @param paramName the name of the parameter.
     * @return a list containing the values for that parameter.
     */
    public Map<String, Grid> getGridValues(String paramName)
    {
        return this.gridValues.getOrDefault(paramName, new HashMap<>());
    }

    /**
     * Given the current grid, formulates the different individual configurations from
     * those parameters:
     * @return a set of configurations.
     */
    public Configurations getConfigurations()
    {
        return new Configurations(this.getListParameters());
    }



    private static <T> List<Map<String, T>> getMaps(Map<String, List<T>> allValues)
    {
        List<Map<String, T>> mapsList = new ArrayList<>();
        for (String variable : allValues.keySet())
        {
            List<Map<String, T>> aux = new ArrayList<>();
            for (T value : allValues.get(variable))
            {
                if (!mapsList.isEmpty())
                {
                    for (Map<String, T> auxMap : mapsList)
                    {
                        Map<String, T> map = new HashMap<>(auxMap);
                        map.put(variable, value);
                        aux.add(map);
                    }
                }
                else
                {
                    Map<String, T> map = new HashMap<>();
                    map.put(variable, value);
                    aux.add(map);
                }
            }
            mapsList = aux;
        }

        if (mapsList.isEmpty()) mapsList.add(new HashMap<>());

        return mapsList;
    }

    private static List<Map<String, Tuple2oo<String, Parameters>>> getMapsFromGrids(Map<String, Map<String, Grid>> allValues)
    {
        List<Map<String, Tuple2oo<String, Parameters>>> mapList = new ArrayList<>();
        for (String variable : allValues.keySet())
        {
            List<Map<String, Tuple2oo<String, Parameters>>> aux = new ArrayList<>();

            Map<String, Grid> values = allValues.get(variable);
            for (String name : values.keySet())
            {
                Grid grid = values.get(name);
                List<Parameters> parameters = grid.getListParameters();

                for (Parameters params : parameters)
                {
                    if (!mapList.isEmpty())
                    {
                        for (Map<String, Tuple2oo<String, Parameters>> auxMap : mapList)
                        {
                            Map<String, Tuple2oo<String, Parameters>> map = new HashMap<>(auxMap);
                            map.put(variable, new Tuple2oo<>(name, params));
                            aux.add(map);
                        }
                    }
                    else
                    {
                        Map<String, Tuple2oo<String, Parameters>> map = new HashMap<>();
                        map.put(variable, new Tuple2oo<>(name, params));
                        aux.add(map);
                    }
                }
            }

            mapList = aux;
        }

        if (mapList.isEmpty()) mapList.add(new HashMap<>());

        return mapList;
    }

    /**
     * Obtains a list of parameters
     * @return the list of parameter configurations.
     */
    private List<Parameters> getListParameters()
    {
        List<Map<String, Double>> doubleMapsList = getMaps(doubleValues);
        List<Map<String, EdgeOrientation>> orientationMapsList = getMaps(orientationValues);
        List<Map<String, String>> stringMapsList = getMaps(stringValues);
        List<Map<String, Integer>> intMapsList = getMaps(integerValues);
        List<Map<String, Boolean>> booleanMapsList = getMaps(booleanValues);
        List<Map<String, Long>> longMapsList = getMaps(longValues);
        List<Map<String, Tuple2oo<String, Parameters>>> parametersList = getMapsFromGrids(gridValues);

        List<Parameters> parameterList = new ArrayList<>();

        for (Map<String, Double> doubles : doubleMapsList)
        {
            for (Map<String, EdgeOrientation> orientations : orientationMapsList)
            {
                for (Map<String, String> strings : stringMapsList)
                {
                    for (Map<String, Integer> integers : intMapsList)
                    {
                        for (Map<String, Boolean> booleans : booleanMapsList)
                        {
                            for (Map<String, Long> longs : longMapsList)
                            {
                                for (Map<String, Tuple2oo<String, Parameters>> parameters : parametersList)
                                {
                                    Parameters params = new Parameters(doubles, orientations, strings, integers, booleans, longs, parameters);
                                    parameterList.add(params);
                                }
                            }
                        }
                    }
                }
            }
        }

        return parameterList;
    }
}
