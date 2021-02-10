/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.io.backup;

import es.uam.eps.ir.socialranksys.diffusion.simulation.Simulation;

import java.io.Serializable;

/**
 * Interface for writing a simulation into a file.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public interface SimulationWriter<U extends Serializable, I extends Serializable, P> 
{
    /**
     * Initializes the simulation writing.
     * @param file the name of the file where we want to store the simulation.
     * @return true if everything went OK, false if something failed while configuring the writer.
     */
    boolean initialize(String file);
    
    /**
     * Writes a whole simulation in a file.
     * @param simulation the simulation.
     * @return true if everything went OK, false if something failed while writing.
     */
    boolean writeSimulation(Simulation<U, I, P> simulation);

    /**
     * Writes a single iteration in a file.
     * @param simulation the simulation.
     * @param numIter the iteration number.
     * @return true if everything went OK, false if something failed while writing
     */
    boolean writeIteration(Simulation<U, I, P> simulation, int numIter);
    
    /**
     * Closes the writing objects.
     * @return true if everything went OK, false if something failed while deconfiguring the writer.
     */
    boolean close();
}
