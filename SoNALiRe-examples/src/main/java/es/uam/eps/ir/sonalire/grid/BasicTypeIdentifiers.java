/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid;

/**
 * Basic type identifiers for reading parameters from configuration files.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class BasicTypeIdentifiers 
{
    /**
     * Type identifier for integers
     */
    public final static String INTEGER_TYPE = "int";
    /**
     * Type identifier for doubles
     */
    public final static String DOUBLE_TYPE = "double";
    /**
     * Type identifier for booleans
     */
    public final static String BOOLEAN_TYPE = "boolean";
    /**
     * Type identifier for strings
     */
    public final static String STRING_TYPE = "string";
    /**
     * Type identifier for longs
     */
    public final static String LONG_TYPE = "long";
    /**
     * Type identifier for edge orientations
     */
    public final static String ORIENTATION_TYPE = "orientation";
    /**
     * Type identifier for grids
     */
    public final static String OBJECT_TYPE = "object";
}
