/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.utils.matrix;

/**
 * Enumeration for identifying the matrix libraries.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public enum MatrixLibrary
{
    /**
     * Identifier for the COLT library
     */
    COLT,
    /**
     * Identifier for the Matrix Toolkit Java library
     */
    MTJ,
    /**
     * Identifier for the JBLAS library.
     */
    JBLAS;

    @Override
    public String toString()
    {
        return switch (this)
        {
            case COLT -> "colt";
            case MTJ -> "mtj";
            case JBLAS -> "jblas";
        };
    }

    /**
     * From a string, reads the identifier of the matrix library.
     * @param str the string for selecting the matrix library.
     * @return the matrix library.
     */
    public static MatrixLibrary fromString(String str)
    {
        return switch (str.toLowerCase())
        {
            case "colt" -> COLT;
            case "mtj" -> MTJ;
            case "jblas" -> JBLAS;
            default -> null;
        };
    }
}