/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms;

import com.esotericsoftware.yamlbeans.YamlReader;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.YAMLGridReader;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reads the grids for several algorithms.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class YAMLAlgorithmGridReader extends YAMLGridReader
{
    private final String ALGORITHMS = "algorithms";

    /**
     * Algorithms grid. Uses a grid for each algorithm.
     */
    private final Map<String, Grid> algorithmsGrid;
    /**
     * The name of the file
     */
    private final String file;

    /**
     * Constructor
     *
     * @param file File that contains the grid data
     */
    public YAMLAlgorithmGridReader(String file)
    {
        this.file = file;
        this.algorithmsGrid = new HashMap<>();
    }

    /**
     * Reads a XML document containing a grid
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
     * Reads the grid for a single algorithm.
     *
     * @param element The YAML Element containing the algorithm information
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
     * @param algorithm The algorithm to search
     *
     * @return The grid if exists, an empty grid if not.
     */
    public Grid getGrid(String algorithm)
    {
        return this.algorithmsGrid.getOrDefault(algorithm, new Grid());
    }
}
