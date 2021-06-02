/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.community;

import es.uam.eps.ir.sonalire.grid.Configurations;
import es.uam.eps.ir.sonalire.grid.ConfigurationsReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for reading a community detection algorithm.
 * For each algorithm, a list of parameter configurations is read.
 *
 * <br/>
 *
 * File format:<br/>
 * algorithm:<br/>
 *     param1: ...<br />
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see ConfigurationsReader
 */
public class CommunityDetectionConfigurationsReader extends ConfigurationsReader
{
    /**
     * Identifier for the name of the mechanism
     */
    private final static String ALGORITHMS = "algorithms";
    /**
     * The set of parameters for the community detection algorithms.
     */
    private final Map<String, Configurations> communityDetectionAlgorithms;

    /**
     * Constructor.
     */
    public CommunityDetectionConfigurationsReader()
    {
        this.communityDetectionAlgorithms = new HashMap<>();
    }
    
    /**
     * Reads a YAML document containing the parameters
     * @param map a map containing the information about the community detection algorithms in the YAML file.
     */
    public void read(Map<String, Object> map)
    {

        Map<String, Object> algs = (Map<String, Object>) map.get(ALGORITHMS);

        for(Map.Entry<String, Object> entry : algs.entrySet())
        {
            this.readAlgorithm(entry);
        }
    }
    
    
    /**
     * Reads the parameters of a community detection algorithm.
     * @param element the YAML element containing the information for that community detection algorithm.
     */
    public void readAlgorithm(Map.Entry<String,Object> element)
    {
        String name = element.getKey();

        if(element.getValue().getClass() == String.class)
        {
            Configurations p = new Configurations();
            this.communityDetectionAlgorithms.put(name, p);
        }
        else
        {
            List<Object> params = (List<Object>) element.getValue();
            Configurations p = this.readConfigurationGrid(params, new CommunityDetectionParametersReader());
            this.communityDetectionAlgorithms.put(name, p);
        }
    }

    /**
     * Gets the set of community detection algorithms previously read from the YAML file.
     * @return the set of algorithms.
     */
    public Set<String> getAlgorithms()
    {
        return this.communityDetectionAlgorithms.keySet();
    }
    
    /**
     * Gets the parameter configurations for the given algorithm.
     * @param algorithm the community detection algorithm to search.
     * @return The parameter configurations if it exists, an empty parameter object if not.
     */
    public Configurations getConfigurations(String algorithm)
    {
        return this.communityDetectionAlgorithms.getOrDefault(algorithm, new Configurations());
    }
}
