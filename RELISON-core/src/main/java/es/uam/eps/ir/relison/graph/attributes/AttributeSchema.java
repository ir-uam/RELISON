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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Describes the set of attributes available for the nodes or edges of a graph, i.e. a mapping from each attribute
 * name to its {@link AttributeType}. Attributes are kept in declaration order.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class AttributeSchema implements Serializable
{
    /** Attribute name to type, in declaration order. */
    private final Map<String, AttributeType> types = new LinkedHashMap<>();

    /**
     * Declares an attribute, or changes the type of an existing one.
     * @param name the attribute name.
     * @param type the attribute type.
     * @throws IllegalArgumentException if the name or type is {@code null}, or the name is blank.
     */
    public void addAttribute(String name, AttributeType type)
    {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Attribute name must not be blank.");
        if (type == null) throw new IllegalArgumentException("Attribute type must not be null.");
        this.types.put(name, type);
    }

    /**
     * Removes an attribute from the schema.
     * @param name the attribute name.
     * @return true if the attribute existed and was removed, false otherwise.
     */
    public boolean removeAttribute(String name)
    {
        return this.types.remove(name) != null;
    }

    /**
     * Checks whether an attribute is declared.
     * @param name the attribute name.
     * @return true if the attribute exists, false otherwise.
     */
    public boolean contains(String name)
    {
        return this.types.containsKey(name);
    }

    /**
     * Obtains the type of an attribute.
     * @param name the attribute name.
     * @return the type, or {@code null} if the attribute is not declared.
     */
    public AttributeType getType(String name)
    {
        return this.types.get(name);
    }

    /**
     * Obtains the declared attribute names, in declaration order.
     * @return a stream of attribute names.
     */
    public Stream<String> getAttributes()
    {
        return this.types.keySet().stream();
    }

    /**
     * Obtains the number of declared attributes.
     * @return the number of attributes.
     */
    public int size()
    {
        return this.types.size();
    }
}
