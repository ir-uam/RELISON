/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.attributes;

/**
 * The data type of a node or edge attribute.
 *
 * <p>An attribute is a named, typed value associated with each node or edge of a graph (beyond the edge weight and
 * type already provided by the graph model). The type determines how the textual representation of a value is parsed
 * and which Java values are accepted for it.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public enum AttributeType
{
    /** A 32-bit integer value, stored as {@link Integer}. */
    INTEGER("int"),
    /** A 64-bit integer value, stored as {@link Long}. */
    LONG("long"),
    /** A floating-point value, stored as {@link Double}. */
    DOUBLE("double"),
    /** A boolean value, stored as {@link Boolean}. */
    BOOLEAN("bool"),
    /** A free-form textual value, stored as {@link String}. */
    STRING("string"),
    /** A textual value drawn from a (typically small) set of categories, stored as {@link String}. */
    CATEGORICAL("categorical"),
    /**
     * A temporal value: a comma-separated list whose items are either a single (non-negative) timestamp or a
     * {@code start-end} range of timestamps, stored in canonical text form as a {@link String} (e.g.
     * {@code "5,10-20,30"}).
     */
    TIME("time");

    /** Canonical token used in serialized headers. */
    private final String token;

    AttributeType(String token)
    {
        this.token = token;
    }

    /**
     * Returns the canonical token for this type, used when serializing attribute headers.
     * @return the canonical token.
     */
    public String token()
    {
        return this.token;
    }

    /**
     * Indicates whether values of this type are numeric (integer, long or double).
     * @return true if the type is numeric, false otherwise.
     */
    public boolean isNumeric()
    {
        return this == INTEGER || this == LONG || this == DOUBLE;
    }

    /**
     * Parses the textual representation of a value of this type.
     * @param text the text to parse (an empty or {@code null} string yields {@code null}).
     * @return the parsed value, boxed in the corresponding Java type, or {@code null} for empty input.
     * @throws NumberFormatException    if a numeric value is malformed.
     * @throws IllegalArgumentException if a boolean value is unrecognised.
     */
    public Object parse(String text)
    {
        if (text == null) return null;
        String t = text.trim();
        if (t.isEmpty()) return null;
        return switch (this)
        {
            case INTEGER -> Integer.valueOf(t);
            case LONG -> Long.valueOf(t);
            case DOUBLE -> Double.valueOf(t);
            case BOOLEAN -> parseBoolean(t);
            case TIME -> parseTime(t);
            case STRING, CATEGORICAL -> t;
        };
    }

    /**
     * Formats a value of this type as text suitable for serialization. A {@code null} value yields an empty string.
     * @param value the value to format.
     * @return the textual representation.
     */
    public String format(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * Checks whether a Java value is a valid instance of this type. A {@code null} value is always valid (it denotes
     * the absence of a value).
     * @param value the value to check.
     * @return true if the value is acceptable for this type, false otherwise.
     */
    public boolean isValid(Object value)
    {
        if (value == null) return true;
        return switch (this)
        {
            case INTEGER -> value instanceof Integer;
            case LONG -> value instanceof Long;
            case DOUBLE -> value instanceof Double;
            case BOOLEAN -> value instanceof Boolean;
            case STRING, CATEGORICAL, TIME -> value instanceof String;
        };
    }

    /**
     * Resolves an attribute type from a textual token (case-insensitive). Several aliases are accepted, e.g.
     * {@code integer}/{@code int}, {@code real}/{@code double}/{@code float}, {@code boolean}/{@code bool},
     * {@code str}/{@code string}, {@code cat}/{@code categorical}.
     * @param token the token to resolve.
     * @return the matching attribute type.
     * @throws IllegalArgumentException if the token does not match any known type.
     */
    public static AttributeType fromToken(String token)
    {
        if (token == null) throw new IllegalArgumentException("Null attribute type token.");
        return switch (token.trim().toLowerCase())
        {
            case "int", "integer" -> INTEGER;
            case "long" -> LONG;
            case "double", "real", "float", "numeric" -> DOUBLE;
            case "bool", "boolean" -> BOOLEAN;
            case "string", "str", "text" -> STRING;
            case "categorical", "category", "cat", "nominal" -> CATEGORICAL;
            case "time", "temporal", "timestamps" -> TIME;
            default -> throw new IllegalArgumentException("Unknown attribute type: " + token);
        };
    }

    /**
     * Validates and canonicalises a temporal value: a comma-separated list whose items are either a single
     * (non-negative) timestamp or a {@code start-end} range. Whitespace is ignored, reversed ranges are normalised
     * (so {@code 20-10} becomes {@code 10-20}) and single-point ranges collapse to a plain timestamp.
     * @param text the trimmed, non-empty text.
     * @return the canonical representation, e.g. {@code "5,10-20,30"}.
     * @throws IllegalArgumentException if any item is neither a timestamp nor a valid range of integers.
     */
    private static String parseTime(String text)
    {
        StringBuilder canonical = new StringBuilder();
        for (String rawItem : text.split(","))
        {
            String item = rawItem.trim();
            if (item.isEmpty()) continue;
            int dash = item.indexOf('-');
            String piece;
            try
            {
                if (dash < 0)
                {
                    piece = Long.toString(Long.parseLong(item));
                }
                else
                {
                    long start = Long.parseLong(item.substring(0, dash).trim());
                    long end = Long.parseLong(item.substring(dash + 1).trim());
                    long lo = Math.min(start, end);
                    long hi = Math.max(start, end);
                    piece = (lo == hi) ? Long.toString(lo) : (lo + "-" + hi);
                }
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("Invalid time value '" + item
                        + "': expected a timestamp or a 'start-end' range of integers.");
            }
            if (canonical.length() > 0) canonical.append(',');
            canonical.append(piece);
        }
        if (canonical.length() == 0)
        {
            throw new IllegalArgumentException("A time value must contain at least one timestamp or range.");
        }
        return canonical.toString();
    }

    /**
     * Parses a boolean from a textual representation, accepting common spellings.
     * @param t the trimmed, non-empty text.
     * @return the parsed boolean.
     * @throws IllegalArgumentException if the text is not a recognised boolean.
     */
    private static Boolean parseBoolean(String t)
    {
        return switch (t.toLowerCase())
        {
            case "true", "1", "yes", "y", "t" -> Boolean.TRUE;
            case "false", "0", "no", "n", "f" -> Boolean.FALSE;
            default -> throw new IllegalArgumentException("Unrecognised boolean value: " + t);
        };
    }
}
