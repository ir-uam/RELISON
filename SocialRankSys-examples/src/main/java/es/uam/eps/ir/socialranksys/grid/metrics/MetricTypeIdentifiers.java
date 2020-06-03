/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.metrics;

import org.openide.util.Exceptions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Metric identifiers
 * @author Javier Sanz-Cruzado Puig
 */
public class MetricTypeIdentifiers 
{
    
    // Identifiers for metric types
    /**
     * Type for graph metrics
     */
    public final static String GRAPH_METRIC = "Graph";
    /**
     * Type for vertex metrics
     */
    public final static String VERTEX_METRIC = "Vertex";
    /**
     * Type for edge metrics
     */
    public final static String EDGE_METRIC = "Edge";
    /**
     * Type for pair metrics
     */
    public final static String PAIR_METRIC = "Pair";
    /**
     * Individual community metric
     */
    public final static String INDIV_COMM_METRIC = "Indiv. Comm.";
    /**
     * Community metric
     */
    public final static String GLOBAL_COMM_METRIC = "Global Comm.";
    
    /**
     * Obtains all the possible values for metric type identifiers.
     * @return a list containing the values.
     */
    public static List<String> values()
    {
        List<String> list = new ArrayList<>();

        Field[] fields = MetricTypeIdentifiers.class.getDeclaredFields();
        for(Field f : fields)
        {
            if(Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()))
            {
                String field = "";
                try 
                {
                    String value = f.get(field).toString();
                    list.add(value);
                } 
                catch (IllegalArgumentException | IllegalAccessException ex) 
                {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        
        return list;
    }
}
