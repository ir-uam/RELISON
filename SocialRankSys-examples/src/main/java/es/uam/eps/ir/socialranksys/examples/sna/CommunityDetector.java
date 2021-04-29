/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.examples.sna;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.community.io.TextCommunitiesWriter;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.grid.community.CommunityDetectionParamReader;
import es.uam.eps.ir.socialranksys.grid.community.CommunityDetectionSelector;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.io.graph.TextMultiGraphReader;
import org.ranksys.formats.parsing.Parsers;

import static es.uam.eps.ir.socialranksys.examples.AuxiliarVariables.FALSE;
import static es.uam.eps.ir.socialranksys.examples.AuxiliarVariables.TRUE;

/**
 * Program for computing community partitions of an individual network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CommunityDetector
{
    /**
     * Computes a community partition for several algorithms.
     * @param args Execution arguments
     *             <ol>
     *                  <li><b>Graph:</b> The network graph</li>
     *                  <li><b>Multigraph:</b> true if the network is a multigraph, false otherwise.</li>
     *                  <li><b>Directed:</b> true if the network is directed, false otherwise.</li>
     *                  <li><b>Weighted:</b> true if the network is weighted, false otherwise.</li>
     *                  <li><b>Self-loops:</b> true if we allow self-loops, false otherwise.</li>
     *                  <li><b>Algorithms:</b> YAML file containing the configuration of the community detection algorithms.</li>
     *                  <li><b>Output:</b> Directory in which to store the communities.</li>
     *             </ol>
     */
    public static void main(String[] args)
    {
        // Read the parameters.
        if(args.length < 7)
        {
            System.err.println("ERROR: Invalid arguments");
            System.err.println("Usage:");
            System.err.println("\tGraph: the file containing the network graph.");
            System.err.println("\tMultigraph: " + TRUE + " if the network is a multigraph, " + FALSE + " otherwise.");
            System.err.println("\tDirected: " + TRUE + " if the network is directed, " + FALSE + " otherwise.");
            System.err.println("\tWeighted: " + TRUE + " if the network uses weights, " + FALSE + " otherwise.");
            System.err.println("\tSelf-loops: " + TRUE + " if the network has self-loops, " + FALSE + " otherwise.");
            System.err.println("\tAlgorithms: a YAML file containing the algorithm configurations.");
            System.err.println("\tOutput: the directory in which we want to store the communities.");
            return;
        }

        String graphRoute = args[0];
        boolean multigraph = args[1].equalsIgnoreCase(TRUE);
        boolean directed = args[2].equalsIgnoreCase(TRUE);
        boolean weighted = args[3].equalsIgnoreCase(TRUE);
        boolean selfloops = args[4].equalsIgnoreCase(TRUE);

        String grid = args[5];
        String outputRoute = args[6];

        // Read the graph
        TextGraphReader<Long> greader;
        if(multigraph)
        {
            greader = new TextMultiGraphReader<>(directed, weighted, selfloops, "\t", Parsers.lp);
        }
        else
        {
            greader = new TextGraphReader<>(directed, weighted, selfloops, "\t", Parsers.lp);
        }
        Graph<Long> graph = greader.read(graphRoute, weighted, false);

        // Grid Reader
        CommunityDetectionParamReader cdReader = new CommunityDetectionParamReader(grid);
        cdReader.readDocument();

        // Execute the community detection algorithms.
        System.out.println("Starting community detection");
        long a = System.currentTimeMillis();

        CommunityDetectionSelector<Long> selector = new CommunityDetectionSelector<>();

        cdReader.getAlgorithms().parallelStream().forEach(algorithm ->
        {
            System.out.println("Algorithm " + algorithm + " started.");
            long a1 = System.currentTimeMillis();

            // Detect the communities.
            CommunityDetectionAlgorithm<Long> alg = selector.getCommunityDetectionAlgorithm(algorithm, cdReader.getParameters(algorithm)).v2();
            Communities<Long> comm = alg.detectCommunities(graph);

            // Write the communities into a file.
            TextCommunitiesWriter<Long> cwriter = new TextCommunitiesWriter<>("\t");
            cwriter.write(comm, outputRoute + algorithm + ".txt");

            long b1 = System.currentTimeMillis();
            System.out.println("Algorithm " + algorithm + " finished (" + (b1-a1) + " ms.)");
        });

        long b = System.currentTimeMillis();
        System.out.println("Ending commmunity detection (" + (b-a) + " ms.)");

    }
}
