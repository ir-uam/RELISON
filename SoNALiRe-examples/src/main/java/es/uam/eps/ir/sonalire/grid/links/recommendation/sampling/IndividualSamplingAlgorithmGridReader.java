/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.recommendation.sampling;

import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.ParametersReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reads the grid for sampling algorithms from a YAML file.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class IndividualSamplingAlgorithmGridReader extends ParametersReader
{
    /**
     * String for identifying the samplers in the file:
     */
    private final static String SAMPLERS = "samplers";

    /**
     * Algorithms grid. Uses a grid for each algorithm.
     */
    private final Map<String, Parameters> samplersGrid;

    /**
     * Constructor.
     */
    public IndividualSamplingAlgorithmGridReader()
    {
        this.samplersGrid = new HashMap<>();
    }
    
    /**
     * Reads a YAML document containing a grid.
     * @param map a map containing the grid information.
     */
    public void read(Map<String, Object> map)
    {
        Map<String, Object> algs = (Map<String, Object>) map.get(SAMPLERS);

        for(Map.Entry<String, Object> entry : algs.entrySet())
        {
            this.readIndividualSamplingAlgorithm(entry);
        }
    }
    
    /**
     * Reads the grid for a single algorithm.
     * @param element the YAML element containing the algorithm information.
     */
    private void readIndividualSamplingAlgorithm(Map.Entry<String, Object> element)
    {
        String algorithmName = element.getKey();

        if(element.getValue().getClass() == String.class) // if this happens, there is not
        {
            Parameters params = new Parameters();
            this.samplersGrid.put(algorithmName, params);
        }
        else
        {
            Map<String, Object> params = (Map<String, Object>) element.getValue();
            Parameters p = this.readParameterValues(params);
            this.samplersGrid.put(algorithmName, p);
        }
    }

    /**
     * Gets the set of algorithms previously read.
     * @return the set of algorithms previously read from the grid file.
     */
    public Set<String> getIndividualSamplingAlgorithms()
    {
        return this.samplersGrid.keySet();
    }
    
    /**
     * Gets the grid for a given algorithm.
     * @param algorithm the algorithm to search.
     * @return the grid if exists, an empty grid if not.
     */
    public Parameters getParameters(String algorithm)
    {
        return this.samplersGrid.getOrDefault(algorithm, new Parameters());
    }
}
