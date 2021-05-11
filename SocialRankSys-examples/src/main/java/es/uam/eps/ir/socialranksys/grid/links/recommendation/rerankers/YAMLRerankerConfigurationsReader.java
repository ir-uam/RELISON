/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers;

import es.uam.eps.ir.socialranksys.grid.Configurations;
import es.uam.eps.ir.socialranksys.grid.YAMLConfigurationsReader;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.YAMLAlgorithmParametersReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for reading reranking algorithms.
 * For each algorithm, it reads a list of configurations.
 *
 * <br/>
 *
 * File format:<br/>
 * rerankers:<br/>
 *     reranker:<br />
 *     - param1: ...<br/>
 *       param2: ...<br/>
 *     - param1: ...<br />
 *     ...
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see YAMLConfigurationsReader
 */
public class YAMLRerankerConfigurationsReader extends YAMLConfigurationsReader
{
    private final static String RERANKERS = "rerankers";

    /**
     * Map for storing the different configurations of an algorithm.
     */
    private final Map<String, Configurations> rerankersConfs;

    /**
     * Constructor.
     */
    public YAMLRerankerConfigurationsReader()
    {
        this.rerankersConfs = new HashMap<>();
    }

    /**
     * Reads a YAML document containing the different configurations of a list of rerankers.
     */
    public void read(Map<String, Object> map)
    {
        rerankersConfs.clear();
        Map<String, Object> algs = (Map<String, Object>) map.get(RERANKERS);

        for(Map.Entry<String, Object> entry : algs.entrySet())
        {
            this.readAlgorithm(entry);
        }
    }

    /**
     * Reads the configurations for a single algorithm.
     *
     * @param element an object containing the configurations for a single algorithm.
     */
    private void readAlgorithm(Map.Entry<String, Object> element)
    {
        String algorithmName = element.getKey();

        // Then: we check whether there is something else to this algorithm:
        if(element.getValue().getClass() == String.class) // if this happens, there is not
        {
            Configurations confs = new Configurations();
            this.rerankersConfs.put(algorithmName, confs);
        }
        else
        {
            List<Object> confs = (List<Object>) element.getValue();
            Configurations configurations = this.readConfigurationGrid(confs, new YAMLAlgorithmParametersReader());
            this.rerankersConfs.put(algorithmName, configurations);
        }
    }

    /**
     * Once the configurations have been read, it obtains the set of rerankers which have been read
     * from the file. Otherwise, it returns an empty set.
     * @return a set containing the names of the rerankers in the configuration file.
     */
    public Set<String> getRerankers()
    {
        return this.rerankersConfs.keySet();
    }

    /**
     * Gets the different configurations for a single algorithm.
     * @param algorithm the name of the reranker.
     * @return the configurations for the given reranker if exists, an empty list of configurations otherwise.
     */
    public Configurations getConfigurations(String algorithm)
    {
        return this.rerankersConfs.getOrDefault(algorithm, new Configurations());
    }
}
