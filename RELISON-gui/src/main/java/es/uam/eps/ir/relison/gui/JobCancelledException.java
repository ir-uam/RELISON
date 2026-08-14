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
 * Thrown by {@link JobManager#run(String, java.util.concurrent.Callable)} when the computation was stopped by a
 * concurrent {@link JobManager#cancel(String)} (the user pressing a Stop button). Request handlers catch it to reply
 * with a {@code {cancelled:true}} payload rather than treating the interruption as a failure.
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
