/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.metrics;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Identifiers for the different metric types.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MetricTypeIdentifiers 
{
    // Identifiers for metric types
    /**
     * Graph / network metrics.
     */
    public final static String GRAPH_METRIC = "graph";
    /**
     * Vertex / node metrics.
     */
    public final static String VERTEX_METRIC = "vertex";
    /**
     * Edge / link metrics.
     */
    public final static String EDGE_METRIC = "edge";
    /**
     * Pair metrics.
     */
    public final static String PAIR_METRIC = "pair";
    /**
     * Individual community metrics.
     */
    public final static String INDIV_COMM_METRIC = "indiv. community";
    /**
     * Community metrics.
     */
    public final static String GLOBAL_COMM_METRIC = "global community";
    
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
                    ex.printStackTrace();
                }
            }
        }
        
        return list;
    }
}
