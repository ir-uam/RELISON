/* 
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.linkprediction.supervised.classifiers.exceptions;

/**
 * Exception which is thrown when a pattern cannot be classified, since the
 * model has not been built.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class UntrainedException extends Exception
{
    /**
     * Constructs a UntrainedException with the given detail message.
     * @param message The detail message of the UntrainedException.
     */
    public UntrainedException(String message) 
    {
        super(message);
    }

    /**
     * Constructs a UntrainedException with the given root cause.
     * @param cause The root cause of the UntrainedException.
     */
    public UntrainedException(Throwable cause) 
    {
        super(cause);
    }

    /**
     * Constructs a UntrainedException with the given detail message and root cause.
     * @param message The detail message of the UntrainedException.
     * @param cause The root cause of the UntrainedException.
     */
    public UntrainedException(String message, Throwable cause) 
    {
        super(message, cause);
    }
}
