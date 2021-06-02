/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.defexamples.graphgen;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.generator.CompleteGraphGenerator;
import es.uam.eps.ir.sonalire.graph.generator.GraphGenerator;
import es.uam.eps.ir.sonalire.graph.generator.NoLinksGraphGenerator;
import es.uam.eps.ir.sonalire.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.sonalire.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.sonalire.graph.generator.random.BarabasiGenerator;
import es.uam.eps.ir.sonalire.graph.generator.random.ErdosGenerator;
import es.uam.eps.ir.sonalire.graph.generator.random.WattsStrogatzGenerator;
import es.uam.eps.ir.sonalire.io.graph.TextGraphWriter;
import es.uam.eps.ir.sonalire.utils.generator.Generators;
import org.ranksys.formats.parsing.Parsers;

/**
 * Program for generating random graphs.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class RandomGraphGenerator
{
    /**
     * Program that generates a random network.
     * @param args Execution arguments
     *             <ul>
     *                  <li><b>Output:</b> the file where we want to store the network.</li>
     *                  <li><b>Directed:</b> true if the graph is directed, false otherwise.</li>
     *                  <li><b>Algorithm:</b> the graph generation algorithm.</li>
     *                  <li><b>Number of nodes:</b> the number of nodes.</li>
     *                  <li><b>Rest of parameters: </b> algorithm parameters.</li>
     *             </ul>
     */
    public static void main(String[] args) throws GeneratorNotConfiguredException, GeneratorBadConfiguredException
    {
        if(args.length < 4)
        {
            System.err.println("ERROR: Invalid arguments");
            System.err.println("\tOutput: file where we want to store the network.");
            System.err.println("\tDirected: true if the network is directed, false otherwise.");
            System.err.println("\tAlgorithm: the name of the graph generation algorithm.");
            System.err.println("\t\t" + GraphGenerationAlgorithms.EMPTY + " : the empty graph.");
            System.err.println("\t\t" + GraphGenerationAlgorithms.COMPLETE + " : the complete graph.");
            System.err.println("\t\t" + GraphGenerationAlgorithms.ERDOS + " : an Erdos-Renyi graph.");
            System.err.println("\t\t" + GraphGenerationAlgorithms.BARABASI + " : the Barabasi-Albert graph.");
            System.err.println("\t\t" + GraphGenerationAlgorithms.WATTS + " : the Watts-Strogatz graph.");
            return;
        }

        String output = args[0];
        boolean directed = args[1].equalsIgnoreCase("true");
        int numNodes = Parsers.ip.parse(args[2]);
        String algorithm = args[3];

        // Graph generator configuration.
        System.out.println("Configuring graph generator");
        long a = System.currentTimeMillis();
        GraphGenerator<Long> ggen;
        switch (algorithm)
        {
            case GraphGenerationAlgorithms.EMPTY ->
            {
                ggen = new NoLinksGraphGenerator<>();
                ggen.configure(directed, numNodes, Generators.longgen);
            }
            case GraphGenerationAlgorithms.COMPLETE ->
            {
                ggen = new CompleteGraphGenerator<>();
                ggen.configure(directed, numNodes, Generators.longgen);
            }
            case GraphGenerationAlgorithms.ERDOS ->
            {
                ggen = new ErdosGenerator<>();
                if (args.length < 5)
                {
                    System.err.println("ERROR: Invalid arguments for the Erdös-Renyi graph");
                    System.err.println("Missing: \n\t prob: link creation probability");
                    return;
                }
                double prob = Parsers.dp.parse(args[4]);
                ggen.configure(directed, numNodes, prob, Generators.longgen);
            }
            case GraphGenerationAlgorithms.BARABASI ->
            {
                ggen = new BarabasiGenerator<>();

                if (args.length < 6)
                {
                    System.err.println("ERROR: Invalid arguments for the Barabási-Albert graph");
                    System.err.println("Missing: \n");
                    System.err.println("\tinitialNodes: the number of initial nodes");
                    System.err.println("\tnumEdgesIter: the number of new edges on each iteration");
                    return;
                }

                int initialNodes = Parsers.ip.parse(args[4]);
                int numIter = numNodes - initialNodes;
                int numEdgesIter = Parsers.ip.parse(args[5]);
                ggen.configure(directed, initialNodes, numIter, numEdgesIter, Generators.longgen);
            }
            case GraphGenerationAlgorithms.WATTS ->
            {
                ggen = new WattsStrogatzGenerator<>();
                if (args.length < 6)
                {
                    System.err.println("ERROR: Invalid arguments for the Watts-Strogatz graph");
                    System.err.println("Missing: \n");
                    System.err.println("\tavgDegree: the initial average degree");
                    System.err.println("\tbeta: the rewiring probability");
                    return;
                }

                int avgDegree = Parsers.ip.parse(args[4]);
                double beta = Parsers.dp.parse(args[5]);
                ggen.configure(directed, numNodes, avgDegree, beta, Generators.longgen);
            }
            default ->
            {
                System.err.println("ERROR: Invalid algorithm");
                return;
            }
        }
        long b = System.currentTimeMillis();
        System.out.println("Finished configuring graph generator (" + (b-a) + " ms.)");

        // Graph generation
        System.out.println("Generating graph...");
        Graph<Long> graph = ggen.generate();
        b = System.currentTimeMillis();
        System.out.println("Finished generating graph + (" + (b-a) + " ms.");

        // Graph writing
        System.out.println("Writing the graph...");
        TextGraphWriter<Long> textGraphWriter = new TextGraphWriter<>("\t");
        textGraphWriter.write(graph, output);
        b = System.currentTimeMillis();
        System.out.println("Finished writing the graph (" + (b-a) + " ms.)");
    }
}
