/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.exception;

/**
 * An edge metric could not be computed since the edge does not exist.
 * @author Javier Sanz-Cruzado Puig
 */
public class InexistentEdgeException extends Exception
{
    /**
     * Constructs a InexistentEdgeException with the given detail message.
     * @param message The detail message of the InexistentEdgeException.
     */
    public InexistentEdgeException(String message) {
        super(message);
    }

    /**
     * Constructs a InexistentEdgeException with the given root cause.
     * @param cause The root cause of the InexistentEdgeException.
     */
    public InexistentEdgeException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a InexistentEdgeException with the given detail message and root cause.
     * @param message The detail message of the InexistentEdgeException.
     * @param cause The root cause of the InexistentEdgeException.
     */
    public InexistentEdgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
