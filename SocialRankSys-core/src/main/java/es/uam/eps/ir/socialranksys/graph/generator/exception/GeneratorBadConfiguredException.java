/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.generator.exception;

/**
 * Exception for bad configured generators.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class GeneratorBadConfiguredException extends Exception
{
    /**
     * Constructs a GeneratorBadConfiguredException with the given detail message.
     *
     * @param message The detail message of the GeneratorBadConfiguredException.
     */
    public GeneratorBadConfiguredException(String message)
    {
        super(message);
    }

    /**
     * Constructs a GeneratorBadConfiguredException with the given root cause.
     *
     * @param cause The root cause of the GeneratorBadConfiguredException.
     */
    public GeneratorBadConfiguredException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Constructs a GeneratorBadConfiguredException with the given detail message and root cause.
     *
     * @param message The detail message of the GeneratorBadConfiguredException.
     * @param cause   The root cause of the GeneratorBadConfiguredException.
     */
    public GeneratorBadConfiguredException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
