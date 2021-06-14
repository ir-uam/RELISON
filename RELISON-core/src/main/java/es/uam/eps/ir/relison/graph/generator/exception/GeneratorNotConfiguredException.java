/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.generator.exception;

/**
 * Exception for unconfigured generators.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class GeneratorNotConfiguredException extends Exception
{
    /**
     * Constructs a GeneratorNotConfiguredException with the given detail message.
     *
     * @param message The detail message of the GeneratorNotConfiguredException.
     */
    public GeneratorNotConfiguredException(String message)
    {
        super(message);
    }

    /**
     * Constructs a GeneratorNotConfiguredException with the given root cause.
     *
     * @param cause The root cause of the GeneratorNotConfiguredException.
     */
    public GeneratorNotConfiguredException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Constructs a GeneratorNotConfiguredException with the given detail message and root cause.
     *
     * @param message The detail message of the GeneratorNotConfiguredException.
     * @param cause   The root cause of the GeneratorNotConfiguredException.
     */
    public GeneratorNotConfiguredException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
