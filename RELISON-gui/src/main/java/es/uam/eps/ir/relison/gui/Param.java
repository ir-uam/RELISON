/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tunable parameter of a metric or community-detection algorithm, used both to advertise the parameter to the
 * frontend (so it can render a control) and to validate/convert the value the user supplies.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class Param
{
    /** The grid key the RELISON selector expects (e.g. {@code orientation}, {@code r}, {@code alpha}). */
    public final String name;
    /** The label shown next to the control. */
    public final String label;
    /** The control type: {@code orientation}, {@code double}, {@code int} or {@code bool}. */
    public final String type;
    /** The default value (an {@link es.uam.eps.ir.relison.graph.edges.EdgeOrientation} name, Double, Integer or Boolean). */
    public final Object defaultValue;
    /** For {@code orientation} parameters, the allowed values; {@code null} otherwise. */
    public final List<String> options;

    private Param(String name, String label, String type, Object defaultValue, List<String> options)
    {
        this.name = name;
        this.label = label;
        this.type = type;
        this.defaultValue = defaultValue;
        this.options = options;
    }

    /** Builds an edge-orientation parameter with the four standard options. */
    public static Param orientation(String name, String label, String def)
    {
        return new Param(name, label, "orientation", def, List.of("OUT", "IN", "UND", "MUTUAL"));
    }

    /** Builds a string parameter chosen from a fixed set of options. */
    public static Param choice(String name, String label, String def, List<String> options)
    {
        return new Param(name, label, "string", def, options);
    }

    /** Builds a real-valued parameter. */
    public static Param real(String name, String label, double def)
    {
        return new Param(name, label, "double", def, null);
    }

    /** Builds an integer parameter. */
    public static Param integer(String name, String label, int def)
    {
        return new Param(name, label, "int", def, null);
    }

    /** Builds a boolean parameter. */
    public static Param bool(String name, String label, boolean def)
    {
        return new Param(name, label, "bool", def, null);
    }

    /**
     * Serializes this parameter for the catalog payload.
     * @return a JSON-serializable map.
     */
    public Map<String, Object> toJson()
    {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", name);
        json.put("label", label);
        json.put("type", type);
        json.put("default", defaultValue);
        if (options != null) json.put("options", options);
        return json;
    }
}
