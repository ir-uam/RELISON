/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.algorithms;

import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.GridReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class for reading contact recommendation / link prediction algorithms.
 * For each algorithm, a grid of parameters is read.
 *
 * <br>
 *
 * File format:<br>
 * algorithms:<br>
 *     algorithm_name:<br >
 *         param1: ...<br>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see GridReader
 */
public class AlgorithmGridReader extends GridReader
{
    private final static String ALGORITHMS = "algorithms";

    /**
     * Algorithms grid. Uses a grid for each algorithm.
     */
    private final Map<String, Grid> algorithmsGrid;

    /**
     * Constructor
     */
    public AlgorithmGridReader()
    {
        this.algorithmsGrid = new HashMap<>();
    }

    /**
     * Obtains the algorithm grids for different people-to-people recommendation algorithms.
     * @param map a map containing the information in the YAML file about the different algorithms.
     */
    public void read(Map<String, Object> map)
    {
        algorithmsGrid.clear();
        if(map != null)
        {
            Map<String, Object> algs = (Map<String, Object>) map.get(ALGORITHMS);
            for(Map.Entry<String, Object> entry : algs.entrySet())
            {
                this.readAlgorithm(entry);
            }
        }
    }

    /**
     * Reads the grid for a single algorithm.
     *
     * @param element A map containing the information in the YAML file about the algorithm.
     */
    private void readAlgorithm(Map.Entry<String, Object> element)
    {
        String algorithmName = element.getKey();

        // Then: we check whether there is something else to this algorithm:
        if(element.getValue().getClass() == String.class) // if this happens, there is not
        {
            Grid g = new Grid();
            this.algorithmsGrid.put(algorithmName, g);
        }
        else
        {
            Map<String, Object> params = (Map<String, Object>) element.getValue();

            Grid g = readParameterGrid(params);
            this.algorithmsGrid.put(algorithmName, g);
        }
    }

    /**
     * Gets the set of algorithms previously read.
     *
     * @return The set of algorithms previously read from the grid file.
     */
    public Set<String> getAlgorithms()
    {
        return this.algorithmsGrid.keySet();
    }

    /**
     * Gets the grid for a given algorithm
     *
     * @param algorithm the algorithm to search
     *
     * @return the grid if exists, an empty grid if not.
     */
    public Grid getGrid(String algorithm)
    {
        return this.algorithmsGrid.getOrDefault(algorithm, new Grid());
    }
}
