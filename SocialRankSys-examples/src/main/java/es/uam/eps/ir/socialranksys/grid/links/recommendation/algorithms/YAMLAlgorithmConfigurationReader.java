/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms;

import com.esotericsoftware.yamlbeans.YamlReader;
import es.uam.eps.ir.socialranksys.grid.Configurations;
import es.uam.eps.ir.socialranksys.grid.YAMLConfigurationsReader;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the configurations for contact recommendation / link prediction algorithms from a YAML file.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class YAMLAlgorithmConfigurationReader extends YAMLConfigurationsReader
{
    private final String ALGORITHMS = "algorithms";

    /**
     * Map for storing the different configurations of an algorithm.
     */
    private final Map<String, Configurations> algorithmConfs;
    /**
     * The YAML file containing the configurations of the algorithms.
     */
    private final String file;

    /**
     * Constructor.
     *
     * @param file file containing the algorithm configurations.
     */
    public YAMLAlgorithmConfigurationReader(String file)
    {
        this.file = file;
        this.algorithmConfs = new HashMap<>();
    }

    /**
     * Reads a YAML document containing the different configurations of a list of algorithms.
     */
    public void readDocument()
    {
        try
        {
            // First of all, obtain the XML Document
            File inputFile = new File(file);
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
            YamlReader yaml = new YamlReader(reader);

            Map<String, Object> map = (Map<String, Object>) yaml.read();
            Map<String, Object> algs = (Map<String, Object>) map.get(ALGORITHMS);

            for(Map.Entry<String, Object> entry : algs.entrySet())
            {
                this.readAlgorithm(entry);
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
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
            this.algorithmConfs.put(algorithmName, confs);
        }
        else
        {
            List<Object> confs = (List<Object>) element.getValue();
            Configurations configurations = this.readConfigurationGrid(confs, new YAMLAlgorithmParametersReader());
            this.algorithmConfs.put(algorithmName, configurations);
        }
    }

    /**
     * Once the configurations have been read, it obtains the set of algorithms which have been read
     * from the file. Otherwise, it returns an empty set.
     * @return a set containing the names of the algorithms in the configuration file.
     */
    public Set<String> getAlgorithms()
    {
        return this.algorithmConfs.keySet();
    }

    /**
     * Gets the different configurations for a single algorithm.
     * @param algorithm the name of the algorithm.
     * @return the configurations for the given algorithm if exists, an empty list of configurations otherwise.
     */
    public Configurations getConfigurations(String algorithm)
    {
        return this.algorithmConfs.getOrDefault(algorithm, new Configurations());
    }
}
