/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.grid.Grid;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Builds a RELISON {@link Grid} from a parameter specification and the values supplied in a request, falling back
 * to each parameter's default when a value is absent.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class Grids
{
    private Grids()
    {
    }

    /**
     * Builds a single-configuration grid for the given parameters.
     * @param params   the parameter specification of the metric/algorithm.
     * @param provided the values supplied by the client (may be {@code null} or partial).
     * @return a grid populated with one value per parameter.
     */
    public static Grid build(List<Param> params, Map<?, ?> provided)
    {
        Grid grid = new Grid();
        for (Param p : params)
        {
            Object value = provided == null ? null : provided.get(p.name);
            switch (p.type)
            {
                case "orientation" ->
                {
                    String name = value != null ? value.toString() : (String) p.defaultValue;
                    grid.getOrientationValues().put(p.name, List.of(EdgeOrientation.valueOf(name)));
                }
                case "string" -> grid.getStringValues().put(p.name,
                        List.of(value != null ? value.toString() : (String) p.defaultValue));
                case "double" -> grid.getDoubleValues().put(p.name,
                        List.of(value != null ? toDouble(value) : (Double) p.defaultValue));
                case "int" -> grid.getIntegerValues().put(p.name,
                        List.of(value != null ? toInt(value) : (Integer) p.defaultValue));
                case "bool" -> grid.getBooleanValues().put(p.name,
                        List.of(value != null ? toBool(value) : (Boolean) p.defaultValue));
                default -> throw new IllegalArgumentException("Unknown parameter type: " + p.type);
            }
        }
        return grid;
    }

    /**
     * Builds a human-readable suffix describing the resolved parameter values, e.g. {@code " (orientation=OUT)"}.
     * Used to make computed-metric names unique per parameter set (and identical when the parameters match).
     * @param params   the parameter specification.
     * @param provided the values supplied by the client (may be {@code null} or partial).
     * @return the suffix, or an empty string when there are no parameters.
     */
    public static String suffix(List<Param> params, Map<?, ?> provided)
    {
        if (params.isEmpty()) return "";
        StringJoiner sj = new StringJoiner(", ", " (", ")");
        for (Param p : params)
        {
            Object v = provided == null ? null : provided.get(p.name);
            sj.add(p.name + "=" + (v != null ? v : p.defaultValue));
        }
        return sj.toString();
    }

    private static double toDouble(Object v)
    {
        return v instanceof Number ? ((Number) v).doubleValue() : Double.parseDouble(v.toString().trim());
    }

    private static int toInt(Object v)
    {
        return v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString().trim());
    }

    private static boolean toBool(Object v)
    {
        if (v instanceof Boolean) return (Boolean) v;
        String s = v.toString().trim();
        return s.equalsIgnoreCase("true") || s.equals("1");
    }
}
