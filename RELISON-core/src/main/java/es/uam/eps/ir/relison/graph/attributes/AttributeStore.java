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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A typed store of attribute values keyed by an arbitrary object. It is used by the graph implementations to keep
 * the attributes of nodes (keyed by the vertex) and of edges (keyed by a normalised pair of vertices).
 *
 * <p>The store holds an {@link AttributeSchema} together with, for each declared attribute, a sparse map from keys
 * to values. Keying by the application object rather than by an internal numeric index makes the store robust to the
 * re-indexing that the graph performs when nodes are removed.</p>
 *
 * @param <K> the type of the keys (e.g. the vertex type for node attributes).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class AttributeStore<K> implements Serializable
{
    /** The attributes declared in this store. */
    private final AttributeSchema schema = new AttributeSchema();
    /** For each attribute name, a sparse map from key to value. */
    private final Map<String, Map<K, Object>> values = new HashMap<>();

    /**
     * Obtains the schema describing the declared attributes.
     * @return the schema.
     */
    public AttributeSchema getSchema()
    {
        return this.schema;
    }

    /**
     * Declares an attribute (or changes the type of an existing one).
     * @param name the attribute name.
     * @param type the attribute type.
     */
    public void define(String name, AttributeType type)
    {
        this.schema.addAttribute(name, type);
        this.values.computeIfAbsent(name, k -> new HashMap<>());
    }

    /**
     * Removes an attribute and all of its values.
     * @param name the attribute name.
     * @return true if the attribute existed, false otherwise.
     */
    public boolean undefine(String name)
    {
        this.values.remove(name);
        return this.schema.removeAttribute(name);
    }

    /**
     * Sets the value of an attribute for a key. A {@code null} value clears the entry.
     * @param key   the key (vertex or edge pair).
     * @param name  the attribute name.
     * @param value the value to set, or {@code null} to clear it.
     * @return true if the attribute is declared and the value was set/cleared, false if the attribute is unknown.
     * @throws IllegalArgumentException if the value does not match the declared type.
     */
    public boolean set(K key, String name, Object value)
    {
        AttributeType type = this.schema.getType(name);
        if (type == null) return false;
        if (value == null)
        {
            Map<K, Object> map = this.values.get(name);
            if (map != null) map.remove(key);
            return true;
        }
        if (!type.isValid(value))
        {
            throw new IllegalArgumentException("Value " + value + " (" + value.getClass().getSimpleName()
                    + ") is not valid for attribute '" + name + "' of type " + type + ".");
        }
        this.values.computeIfAbsent(name, k -> new HashMap<>()).put(key, value);
        return true;
    }

    /**
     * Obtains the value of an attribute for a key.
     * @param key  the key.
     * @param name the attribute name.
     * @return the value, or {@code null} if unset or the attribute is unknown.
     */
    public Object get(K key, String name)
    {
        Map<K, Object> map = this.values.get(name);
        return map == null ? null : map.get(key);
    }

    /**
     * Removes every attribute value associated with a key (e.g. when its node or edge is deleted).
     * @param key the key to purge.
     */
    public void removeKey(K key)
    {
        for (Map<K, Object> map : this.values.values()) map.remove(key);
    }

    /**
     * Removes every attribute value whose key matches a predicate (e.g. all edge keys involving a deleted node).
     * @param predicate the predicate selecting keys to purge.
     */
    public void removeKeysMatching(Predicate<K> predicate)
    {
        for (Map<K, Object> map : this.values.values()) map.keySet().removeIf(predicate);
    }

    /**
     * Rewrites the keys of every stored value through a mapping function (e.g. when a node is renamed). Values whose
     * key is unchanged by the mapper keep their place.
     * @param mapper the key remapping function.
     */
    public void remapKeys(Function<K, K> mapper)
    {
        for (Map<K, Object> map : this.values.values())
        {
            Map<K, Object> rebuilt = new HashMap<>();
            map.forEach((k, v) -> rebuilt.put(mapper.apply(k), v));
            map.clear();
            map.putAll(rebuilt);
        }
    }

    /**
     * Obtains the declared attribute names, in declaration order.
     * @return a stream of attribute names.
     */
    public Stream<String> names()
    {
        return this.schema.getAttributes();
    }

    /**
     * Obtains the type of an attribute.
     * @param name the attribute name.
     * @return the type, or {@code null} if the attribute is not declared.
     */
    public AttributeType type(String name)
    {
        return this.schema.getType(name);
    }

    /**
     * Checks whether an attribute is declared.
     * @param name the attribute name.
     * @return true if the attribute exists, false otherwise.
     */
    public boolean contains(String name)
    {
        return this.schema.contains(name);
    }
}
