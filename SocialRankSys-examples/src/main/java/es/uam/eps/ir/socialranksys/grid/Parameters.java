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
 * Class for storing the configuration parameters for an algorithm, metric, etc.
 * Each parameter has a single associated value.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Parameters 
{
    /**
     * Double values for the different parameters
     */
    private final Map<String, Double> doubleValues;
    /**
     * Edge orientation values for the different parameters
     */
    private final Map<String, EdgeOrientation> orientationValues;
    /**
     * String values for the different parameters
     */
    private final Map<String, String> stringValues;
    /**
     * Integer values for the different parameters
     */
    private final Map<String, Integer> integerValues;
    /**
     * Boolean values for the different parameters.
     */
    private final Map<String, Boolean> booleanValues;
    /**
     * Long values for the parameters.
     */
    private final Map<String, Long> longValues;
    /**
     * Parameter grids for the parameters.
     */
    private final Map<String, Tuple2oo<String,Parameters>> paramValues;

    /**
     * Constructor.
     * @param doubleValues      double values for the different parameters
     * @param orientationValues edge orientation values for the different parameters
     * @param stringValues      string values for the different parameters
     * @param integerValues     integer values for the different parameters
     * @param booleanValues     boolean values for the different parameters.
     * @param longValues        long values for the parameters.
     * @param paramValues       recursive values for the parameters.
     */
    public Parameters(Map<String, Double> doubleValues, Map<String, EdgeOrientation> orientationValues, Map<String, String> stringValues, Map<String, Integer> integerValues, Map<String, Boolean> booleanValues, Map<String, Long> longValues, Map<String, Tuple2oo<String, Parameters>> paramValues)
    {
        this.doubleValues = doubleValues;
        this.orientationValues = orientationValues;
        this.stringValues = stringValues;
        this.integerValues = integerValues;
        this.booleanValues = booleanValues;
        this.longValues = longValues;
        this.paramValues = paramValues;
    }

    /**
     * Empty constructor.
     */
    public Parameters()
    {
        doubleValues = new HashMap<>();
        orientationValues = new HashMap<>();
        stringValues = new HashMap<>();
        integerValues = new HashMap<>();
        booleanValues = new HashMap<>();
        longValues = new HashMap<>();
        paramValues = new HashMap<>();
    }

    /**
     * 
     * @return Double values for the different parameters
     */
    public Map<String, Double> getDoubleValues()
    {
        return doubleValues;
    }

    /**
     * 
     * @return Edge orientation values for the different parameters
     */
    public Map<String, EdgeOrientation> getOrientationValues()
    {
        return orientationValues;
    }

    /**
     * 
     * @return String values for the different parameters
     */
    public Map<String, String> getStringValues()
    {
        return stringValues;
    }

    /**
     * 
     * @return Integer values for the different parameters
     */
    public Map<String, Integer> getIntegerValues()
    {
        return integerValues;
    }

    /**
     * 
     * @return Boolean values for the different parameters.
     */
    public Map<String, Boolean> getBooleanValues()
    {
        return booleanValues;
    }
    
    /**
     * 
     * @return Long values for the different parameters.
     */
    public Map<String, Long> getLongValues()
    {
        return longValues;
    }
    
    public Map<String, Tuple2oo<String, Parameters>> getParamValues()
    {
        return paramValues;
    }

    /**
     * Gets a double values for a single parameter.
     * @param paramName Parameter name
     * @return the values for that parameter
     */
    public Double getDoubleValue(String paramName)
    {
        return this.doubleValues.getOrDefault(paramName, null);
    }
    
    /**
     * Gets the integer values for a single parameter.
     * @param paramName Parameter name
     * @return the values for that parameter
     */
    public Integer getIntegerValue(String paramName)
    {
        return this.integerValues.getOrDefault(paramName, null);
    }
    
    /**
     * Gets the string values for a single parameter.
     * @param paramName Parameter name
     * @return the values for that parameter
     */
    public String getStringValue(String paramName)
    {
        return this.stringValues.getOrDefault(paramName, null);
    }
    
    /**
     * Gets the long values for a single parameter.
     * @param paramName Parameter name
     * @return the values for that parameter
     */
    public Long getLongValue(String paramName)
    {
        return this.longValues.getOrDefault(paramName, null);
    }
    
    /**
     * Gets the boolean values for a single parameter.
     * @param paramName Parameter name
     * @return the values for that parameter
     */
    public Boolean getBooleanValue(String paramName)
    {
        return this.booleanValues.getOrDefault(paramName, null);
    }

    /**
     * Gets the edge orientation values for a single parameter.
     * @param paramName Parameter name
     * @return the values for that parameter
     */
    public EdgeOrientation getOrientationValue(String paramName)
    {
        return this.orientationValues.getOrDefault(paramName, null);
    }
    
    /**
     * Gets the recursive definition of a single parameter.
     * @param paramName Parameter name.
     * @return the values for that parameter.
     */
    public Tuple2oo<String, Parameters> getParametersValue(String paramName)
    {
        return this.paramValues.getOrDefault(paramName, null);
    }
    
    /**
     * Converts a Parameters object into a grid.
     * @return the grid containing the parameters.
     */
    public Grid toGrid()
    {
        Map<String, List<Double>> doubleValues = new HashMap<>();
        Map<String, List<EdgeOrientation>> orientationValues = new HashMap<>();
        Map<String, List<String>> stringValues = new HashMap<>();
        Map<String, List<Integer>> integerValues = new HashMap<>();
        Map<String, List<Boolean>> booleanValues = new HashMap<>();
        Map<String, List<Long>> longValues = new HashMap<>();
        Map<String, Map<String, Grid>> gridValues = new HashMap<>();
        
        this.doubleValues.forEach((key, value) -> 
        {
            List<Double> list = new ArrayList<>();
            list.add(value);
            doubleValues.put(key, list);
        });
        
        this.orientationValues.forEach((key, value) -> 
        {
            List<EdgeOrientation> list = new ArrayList<>();
            list.add(value);
            orientationValues.put(key, list);
        });
        
        this.stringValues.forEach((key, value) -> 
        {
           List<String> list = new ArrayList<>();
           list.add(value);
           stringValues.put(key, list);
        });
        
        this.integerValues.forEach((key, value) -> 
        {
            List<Integer> list = new ArrayList<>();
            list.add(value);
            integerValues.put(key, list);
        });
        
        this.booleanValues.forEach((key, value) -> 
        {
            List<Boolean> list = new ArrayList<>();
            list.add(value);
            booleanValues.put(key, list);
        });
        
        this.longValues.forEach((key, value) -> 
        {
            List<Long> list = new ArrayList<>();
            list.add(value);
            longValues.put(key, list);
        });
        
        this.paramValues.forEach((key, value) -> 
        {
            Map<String, Grid> grids = new HashMap<>();
            grids.put(value.v1(), value.v2().toGrid());
            gridValues.put(key, grids);
        });
        
        return new Grid(doubleValues, orientationValues, stringValues, integerValues, booleanValues, longValues, gridValues);
    }
}
