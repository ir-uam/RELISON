/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.community;


import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.ParametersReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class for reading a community detection algorithm.
 * For each metric, a single set of parameters is read.
 *
 * <br>
 *
 * File format:<br>
 * algorithm:<br>
 *     param1: ...<br >
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see ParametersReader
 */
public class CommunityDetectionParametersReader extends ParametersReader
{
    /**
     * Identifier for the name of the mechanism
     */
    private final static String ALGORITHMS = "algorithms";
    /**
     * The set of parameters for the community detection algorithms.
     */
    private final Map<String, Parameters> communityDetectionAlgorithms;

    /**
     * Constructor.
     */
    public CommunityDetectionParametersReader()
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
            Parameters p = new Parameters();
            this.communityDetectionAlgorithms.put(name, p);
        }
        else
        {
            Map<String, Object> params = (Map<String, Object>) element.getValue();
            Parameters p = readParameterValues(params);
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
     * Gets the parameters for the given algorithm.
     * @param algorithm the community detection algorithm to search.
     * @return The parameters if it exists, an empty parameter object if not.
     */
    public Parameters getParameters(String algorithm)
    {
        return this.communityDetectionAlgorithms.getOrDefault(algorithm, new Parameters());
    }
}
