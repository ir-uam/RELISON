/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for storing the different possible configurations for an algorithm, metric, etc.
 * Differently from a grid, where we can mix any two values of a parameter, this class
 * stores each possible parameter selection separately.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Configurations 
{
    /**
     * A list of parameter configurations.
     */
    private final List<Parameters> configurations;
    
    /**
     * Constructor.
     * @param configurations a list containing the different configurations for the algorithms
     */
    public Configurations(List<Parameters> configurations)
    {
        this.configurations = configurations;
    }
    
    /**
     * Default constructor.
     * Initializes the list with a single configuration containing an empty set of parameters.
     */
    public Configurations()
    {
        this.configurations = new ArrayList<>();
        this.configurations.add(new Parameters());
    }
    
    
    /**
     * Obtains the configurations for the different algorithms.
     * @return the configurations for the different algorithms.
     */
    public List<Parameters> getConfigurations()
    {
        return this.configurations;
    }

    /**
     * Obtains the number of different configurations.
     * @return the number of different configurations.
     */
    public int numConfigs()
    {
        return this.configurations.size();
    }
    
    /**
     * Obtains the parameters for a single configuration of the algorithm, metric, etc.
     * @param idx index of the configuration.
     * @return the configurations if it exists, null otherwise.
     */
    public Parameters getConfiguration(int idx)
    {
        if(idx < 0 || idx > this.configurations.size())
            return null;
        return this.configurations.get(idx);
    }
}
