/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.distributions;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;

/**
 * Interface for defining a distribution of elements.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <F> Type of the features.
 */
public interface Distribution<U extends Serializable,I extends Serializable, F>
{    
    /**
     * Initializes the necessary parameters for the distribution.
     * @param data the data.
     */
    void initialize(Data<U, I, F> data);

    /**
     * Updates the different values of the distribution.
     * @param iteration The current iteration.
     */
    void update(Iteration<U, I, F> iteration);
    
    /**
     * Prints the distribution into a file.
     * @param file The output file.
     */
    void print(String file);
    
    /**
     * Resets the distribution.
     */
    void clear();
    
    /**
     * Obtains the name of the distribution.
     * @return the name.
     */
    String getName();
    
    /**
     * Checks if the distribution has been initialized.
     * @return true if the distribution has been initialized, false if it has not.
     */
    boolean isInitialized();
}
