/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.examples.diffusion;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.relison.diffusion.io.DataReader;
import es.uam.eps.ir.relison.diffusion.io.backup.BinarySimulationReader;
import es.uam.eps.ir.relison.diffusion.io.backup.BinarySimulationWriter;
import es.uam.eps.ir.relison.diffusion.io.backup.SimulationWriter;
import es.uam.eps.ir.relison.diffusion.simulation.Simulation;
import es.uam.eps.ir.relison.diffusion.simulation.Simulator;
import es.uam.eps.ir.relison.examples.AuxiliarMethods;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.grid.diffusion.SimulationParameterReader;
import es.uam.eps.ir.relison.grid.diffusion.SimulatorSelector;
import es.uam.eps.ir.relison.io.graph.GraphReader;
import es.uam.eps.ir.relison.io.graph.TextGraphReader;
import es.uam.eps.ir.relison.io.graph.TextMultiGraphReader;
import es.uam.eps.ir.relison.utils.datatypes.Tuple2oo;
import org.ranksys.formats.parsing.Parsers;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static es.uam.eps.ir.relison.examples.AuxiliarVariables.TRUE;

/**
 * Executes an information diffusion procedure over a network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Diffusion
{
    private final static String REC = "-rec";
    private final static String USERFEATS = "-userfeats";
    private final static String INFOFEATS = "-infofeats";
    private final static String N = "-n";
    private final static String TESTGRAPH = "-test-graph";
    private final static String BACKUP = "-previous";
    private final static String REALPROP = "-realprop";

    /**
     * Executes the information diffusion through a social network.
     * @param args Execution parameters
     * <ol>
     *  <li><b>configuration:</b> a YAML file containing the simulation parameters.</li>
     *  <li><b>output:</b> the directory for storing the outcomes of the simulation.</li>
     *  <li><b>numReps:</b> the number of executions of each simulation.</li>
     *  <li><b>graphFile:</b> path to a file containing the graph.</li>
     *  <li><b>multigraph:</b> true if the graph has multiple edges between users, false otherwise.</li>
     *  <li><b>directed:</b> true if the graph is directed, false otherwise.</li>
     *  <li><b>weighted:</b> true if the graph is weighted, false otherwise.</li>
     *  <li><b>selfLoops:</b> true if the graph accepts selfloops, false otherwise.</li>
     *  <li><b>readTypes:</b> true if the graph types have to be read from the file, false otherwise.</li>
     *  <li><b>uIndexPath:</b> route of a file containing the list of users.</li>
     *  <li><b>iIndexPath:</b> route of a file containing the list of identifiers of the different information pieces.</li>
     *  <li><b>infoFile:</b> file containing the relation between users and information pieces.</li>
     *  <li><b>Optional arguments:</b>
     *      <ul>
     *          <li><b>-rec recFile:</b> path to a recommendation file, whose edges will be added to the network.</li>
     *          <li><b>-n n:</b> the number of links (per user) to add from the recommendation (if any). By default: 10</li>
     *          <li><b>-test-graph file:</b> a folder to a network file containing additional edges (and shall be used for filtering the recommended edges to add).</li>
     *          <li><b>-userfeats file1,file2,...,fileN:</b> a comma-separated list of files containing the features for the users in the network (e.g. communities).</li>
     *          <li><b>-infofeats file1,file2,...,fileN:</b> a comma-separated list of files containing the features for the information pieces (e.g. hashtags).</li>
     *          <li><b>-realprop file:</b> a file indicating which information pieces have been repropagated by users in another information diffusion process.</li>
     *          <li><b>-previous folder:</b> file containing the result of a previous diffusion procedure.</li>
     *      </ul></li>
     * </ol>
     * @throws IOException if something fails while reading / writing.
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 12)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tconfiguration: a YAML file containing the simulation parameters.");
            System.err.println("\toutput: the directory for storing the outcomes of the simulation.");
            System.err.println("\tnumReps: the number of executions of each simulation.");
            System.err.println("\tgraphFile: path to a file containing the graph.");
            System.err.println("\tmultigraph: true if the graph has multiple edges between users, false otherwise.");
            System.err.println("\tdirected: true if the graph is directed, false otherwise.");
            System.err.println("\tweighted: true if the graph is weighted, false otherwise.");
            System.err.println("\tselfLoops: true if the graph accepts selfloops, false otherwise.");
            System.err.println("\treadTypes: true if the graph types have to be read from the file, false otherwise.");
            System.err.println("\tuIndexPath: route of a file containing the list of users.");
            System.err.println("\tiIndexPath: route of a file containing the list of identifiers of the different information pieces.");
            System.err.println("\tinfoFile: file containing the relation between users and information pieces.");
            System.err.println("Optional arguments:");
            System.err.println("\t" + REC + " recFile: path to a recommendation file, whose edges will be added to the network.");
            System.err.println("\t" + N + " n: the number of links (per user) to add from the recommendation (if any). By default: 10");
            System.err.println("\t" + TESTGRAPH + " file: a folder to a network file containing additional edges (and shall be used for filtering the recommended edges to add).");
            System.err.println("\t" + USERFEATS + " file1,file2,...,fileN: a comma-separated list of files containing the features for the users in the network (e.g. communities).");
            System.err.println("\t" + INFOFEATS + " file1,file2,...,fileN: a comma-separated list of files containing the features for the information pieces (e.g. hashtags).");
            System.err.println("\t" + REALPROP + " file: a file indicating which information pieces have been repropagated by users in another information diffusion process.");
            System.err.println("\t" + BACKUP + " folder: file containing the result of a previous diffusion procedure.");
            return;
        }

        // Configuration of the experiment:
        String config = args[0];
        String output = args[1];
        int numReps = Parsers.ip.parse(args[2]);
        String backup = null;

        // Network parameters:
        String graph = args[3];
        boolean multigraph = args[4].equalsIgnoreCase(TRUE);
        boolean directed = args[5].equalsIgnoreCase(TRUE);
        boolean weighted = args[6].equalsIgnoreCase(TRUE);
        boolean selfLoops = args[7].equalsIgnoreCase(TRUE);
        boolean readTypes = args[8].equalsIgnoreCase(TRUE);

        // Now, we do need a file containing the user information and information pieces information
        String uIndexPath = args[9];
        String iIndexPath = args[10];
        // A file containing the relationship between users and information pieces, timestamps, etc.
        String infoFile = args[11];

        String[] userFeatFiles = null;
        String[] infoFeatFiles = null;
        String recFile = null;
        int topN = Integer.MAX_VALUE;
        String realProp = null;
        String testGraphFile = null;

        for(int i = 12; i < args.length; ++i)
        {
            if(args[i].equalsIgnoreCase(REC))
            {
                recFile = args[++i];
            }
            else if(args[i].equalsIgnoreCase(USERFEATS))
            {
                userFeatFiles = args[++i].split(",");
            }
            else if(args[i].equalsIgnoreCase(INFOFEATS))
            {
                infoFeatFiles = args[++i].split(",");
            }
            else if(args[i].equalsIgnoreCase(N))
            {
                topN = Parsers.ip.parse(args[++i]);
            }
            else if(args[i].equalsIgnoreCase(REALPROP))
            {
                realProp = args[++i];
            }
            else if(args[i].equalsIgnoreCase(BACKUP))
            {
                backup = args[++i];
            }
            else if(args[i].equalsIgnoreCase(TESTGRAPH))
            {
                testGraphFile = args[++i];
            }
        }


        // Now, we read the data for the simulation:
        long timea = System.currentTimeMillis();

        DataReader<Long, Long, Long> dataReader = new DataReader<>();
        Data<Long, Long, Long> data;

        if(recFile == null) // When no recommendation data is used:
        {
            if(userFeatFiles == null && infoFeatFiles == null && realProp == null)
            {
                data = dataReader.readData(graph, multigraph, directed, weighted, selfLoops, readTypes, uIndexPath, iIndexPath, infoFile, Parsers.lp, Parsers.lp);
            }
            else if(realProp == null)
            {
                data = dataReader.readData(graph, multigraph, directed, weighted, selfLoops, readTypes, uIndexPath, iIndexPath, infoFile, userFeatFiles, infoFeatFiles, Parsers.lp, Parsers.lp, Parsers.lp);
            }
            else if(infoFeatFiles == null && userFeatFiles == null)
            {
                data = dataReader.readData(graph, multigraph, directed, weighted, selfLoops, readTypes, uIndexPath, iIndexPath, infoFile, realProp, Parsers.lp, Parsers.lp);
            }
            else
            {
                data = dataReader.readData(graph, multigraph, directed, weighted, selfLoops, readTypes, uIndexPath, iIndexPath, infoFile, userFeatFiles, infoFeatFiles, realProp, Parsers.lp, Parsers.lp, Parsers.lp);
            }
        }
        else // When no recommendation data is used:
        {
            if(userFeatFiles == null && infoFeatFiles == null && realProp == null)
            {
                data = dataReader.readData(graph, multigraph, directed, weighted, selfLoops, uIndexPath, iIndexPath, infoFile, recFile, topN, Parsers.lp, Parsers.lp);
            }
            else if(realProp == null)
            {
                data = dataReader.readData(graph, multigraph, directed, weighted, selfLoops, uIndexPath, iIndexPath, infoFile, userFeatFiles, infoFeatFiles, recFile, topN, Parsers.lp, Parsers.lp, Parsers.lp);
            }
            else if(infoFeatFiles == null && userFeatFiles == null)
            {
                data = dataReader.readData(graph, multigraph, directed, weighted, selfLoops, uIndexPath, iIndexPath, infoFile, realProp, recFile, topN, Parsers.lp, Parsers.lp);
            }
            else
            {
                data = dataReader.readData(graph, multigraph, directed, weighted, selfLoops, uIndexPath, iIndexPath, infoFile, userFeatFiles, infoFeatFiles, realProp, recFile, topN, Parsers.lp, Parsers.lp, Parsers.lp);
            }
        }

        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" + (timeb-timea) + " ms.)");

        GraphReader<Long> greader = multigraph ? new TextMultiGraphReader<>(directed, weighted, selfLoops, "\t", Parsers.lp) : new TextGraphReader<>(directed, weighted, selfLoops, "\t", Parsers.lp);
        Graph<Long> testGraph = testGraphFile == null ? null : greader.read(testGraphFile, weighted, readTypes);

        // Read the simulation configuration
        SimulationParameterReader simReader = new SimulationParameterReader();
        Map<String, Object> sims = AuxiliarMethods.readYAML(config);
        simReader.read(sims);
        SimulatorSelector<Long, Long, Long> simSel = new SimulatorSelector<>(Parsers.lp);
        
        // For each simulator
        for(int i = 0; i < simReader.numberSimulations(); ++i)
        {
            // Obtain the simulation

            // a) Print the simulation data:
            System.out.println(simReader.printSimulation(i));
            // b) Obtain the simulation:
            Tuple2oo<Simulator<Long,Long,Long>, DataFilter<Long,Long,Long>> pair = simSel.select(simReader, i, Long.MAX_VALUE, testGraph);
            Simulator<Long,Long,Long> sim = pair.v1();
            DataFilter<Long,Long,Long> filter = pair.v2();

            // Filter the data.
            Data<Long,Long,Long> filteredData = filter.filter(data);

            Simulation<Long, Long, Long> backupSim = null;
            // Read the previous data:
            if(backup != null)
            {
                timea = System.currentTimeMillis();
                BinarySimulationReader<Long, Long, Long> binaryReader = new BinarySimulationReader<>();
                binaryReader.initialize(backup);
                backupSim = binaryReader.readSimulation(filteredData);
                binaryReader.close();
                timeb = System.currentTimeMillis();
                System.out.println("Previous simulation read (" + (timeb-timea));
            }


            // Execute the simulation numReps times
            for(int j = 0; j < numReps; ++j)
            {
                // Initialize the simulation
                timea = System.currentTimeMillis();
                if(backupSim != null)
                {
                    sim.initialize(filteredData, backupSim);
                }
                else
                {
                    sim.initialize(filteredData);
                }
                timeb = System.currentTimeMillis();
                System.out.println("Simulation " + j + " initialized (" + (timeb-timea) + " ms.)");

                // Execute the simulation.
                Simulation<Long,Long,Long> simulation = sim.simulate();

                String rec = null;
                if(recFile != null)
                {
                    // Read the recommendation
                    String[] recRoute = recFile.split("/");
                    //String[] recRoute = recFile.split("\\Q\\\\E");
                    rec = recRoute[recRoute.length - 1];
                }

                String outputPath = output + i + "-" + (rec != null ? (rec + "-") : "") + j + ".txt";
                System.out.println("Output path: " + outputPath);


                // Write the simulation into a file (binary mode).
                SimulationWriter<Long,Long,Long> simwriter = new BinarySimulationWriter<>();
                simwriter.initialize(outputPath);
                simwriter.writeSimulation(simulation);
                simwriter.close();  

                timeb = System.currentTimeMillis();
                System.out.println("Conf: " + i + ": Simulation " + j + "finished (" + (timeb-timea) + " ms.)");
            }
            
            System.out.println("Conf. " + i + " finished.");
        }
        
        System.out.println("Finished");
    }
}
