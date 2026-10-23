/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

/**
 * Thrown after a worker has stopped following an accepted cancellation request.
 * Handlers return {@code {cancelled:true}} after worker cleanup, while the Processes
 * panel retains the terminal record. Cancellation never implies immediate thread termination.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class JobCancelledException extends Exception
{
    public JobCancelledException()
    {
        super("The computation was cancelled.");
    }
}
