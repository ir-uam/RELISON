/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for storing the different possible configurations for a set of algorithms.
 * @author Javier Sanz-Cruzado Puig
 */
public class Configurations 
{
    /**
     * A map containing the different configurations for the algorithms.
     */
    private final List<Parameters> configurations;
    
    /**
     * Constructor.
     * @param configurations a map containing the different configurations for the algorithms 
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
    
    public int numConfigs()
    {
        return this.configurations.size();
    }
    
    /**
     * Obtains the configurations for a single algorithm.
     * @param idx index of the configuration.
     * @return the configurations if the algorithm exists, an empty list otherwise.
     */
    public Parameters getConfiguration(int idx)
    {
        if(idx < 0 || idx > this.configurations.size())
            return null;
        return this.configurations.get(idx);
    }
}
