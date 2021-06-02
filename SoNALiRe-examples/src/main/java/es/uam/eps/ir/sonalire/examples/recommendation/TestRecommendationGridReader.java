/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.examples.recommendation;

import es.uam.eps.ir.sonalire.AuxiliarMethods;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.AlgorithmGridReader;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.AlgorithmGridSelector;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import org.ranksys.formats.parsing.Parsers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for reproducing the experiments for the EWC1 axiom.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class TestRecommendationGridReader
{
    /**
     * Program that reproduces the experiments for the EWC1 axiom.
     * Generates a file comparing weigthed and unweighted algorithm variants.
     *
     * @param args Execution arguments:
     *             <ol>
     *               <li><b>Train:</b> Route to the file containing the training graph.</li>
     *               <li><b>Test:</b> Route to the file containing the test links.</li>
     *               <li><b>Algorithms:</b> Route to an XML file containing the recommender configurations.</li>
     *               <li><b>Output directory:</b> Directory in which to store the recommendations and the output file.</li>
     *               <li><b>Directed:</b> True if the network is directed, false otherwise.</li>
     *               <li><b>Rec. Length:</b> Maximum number of recommendations per user.</li>
     *               <li><b>Print recommendations:</b> True if, additionally to the results, you want to print the recommendations. False otherwise</li>
     *               <li><b>All users: </b> true if we want to generate recommendations for all users in the training set, false if only for those who have test links</li>
     *             </ol>
     */
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tAlgorithms: Route to an YAML file containing the recommender configurations.");
            return;
        }

        // Read the execution arguments:
        String algorithmsPath = args[0];

        // Read the XML containing the parameter grid for each algorithm
        AlgorithmGridReader gridreader = new AlgorithmGridReader();
        Map<String, Object> map = AuxiliarMethods.readYAML(algorithmsPath);
        gridreader.read(map);

        Map<String, RecommendationAlgorithmFunction<Long>> recMap = new HashMap<>();
        // Get the different recommenders to execute
        gridreader.getAlgorithms().forEach(algorithm ->
        {
            AlgorithmGridSelector<Long> ags = new AlgorithmGridSelector<>(Parsers.lp);
            Map<String, RecommendationAlgorithmFunction<Long>> suppliers = ags.getRecommenders(algorithm, gridreader.getGrid(algorithm));
            if (suppliers == null)
            {
                System.err.println("ERROR: Algorithm " + algorithm + " could not be read");
            }
            else
            {
                recMap.putAll(ags.getRecommenders(algorithm, gridreader.getGrid(algorithm)));
            }
        });
    }
}
