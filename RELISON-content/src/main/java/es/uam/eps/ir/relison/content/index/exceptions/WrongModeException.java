/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.content.index.exceptions;

/**
 * Exception which is thrown when a index is tried to operate in the wrong mode
 * (if the index is configured in read mode, and the user tries to write into it,
 * or if the index is configured in write mode, and the user to read it).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class WrongModeException extends Exception
{
    /**
     * Constructs a WrongModeException with the given detail message.
     *
     * @param message The detail message of the WrongModeException.
     */
    public WrongModeException(String message)
    {
        super(message);
    }

    /**
     * Constructs a WrongModeException with the given root cause.
     *
     * @param cause The root cause of the WrongModeException.
     */
    public WrongModeException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Constructs a WrongModeException with the given detail message and root cause.
     *
     * @param message The detail message of the WrongModeException.
     * @param cause   The root cause of the WrongModeException.
     */
    public WrongModeException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
