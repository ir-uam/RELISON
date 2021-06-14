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
import es.uam.eps.ir.relison.diffusion.io.backup.SimulationReader;
import es.uam.eps.ir.relison.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.relison.diffusion.metrics.distributions.Distribution;
import es.uam.eps.ir.relison.diffusion.simulation.Iteration;
import es.uam.eps.ir.relison.diffusion.simulation.Simulation;
import es.uam.eps.ir.relison.examples.AuxiliarMethods;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.grid.diffusion.SimulationMetricsParameterReader;
import es.uam.eps.ir.relison.grid.diffusion.SimulationMetricsSelector;
import es.uam.eps.ir.relison.io.graph.GraphReader;
import es.uam.eps.ir.relison.io.graph.TextGraphReader;
import es.uam.eps.ir.relison.io.graph.TextMultiGraphReader;
import es.uam.eps.ir.relison.utils.datatypes.Triplet;
import org.jooq.lambda.tuple.Tuple2;
import org.ranksys.formats.parsing.Parsers;

import java.io.*;
import java.util.List;
import java.util.Map;

import static es.uam.eps.ir.relison.examples.AuxiliarVariables.TRUE;

/**
 * Given the outcome of a simulation procedure, measures its different properties and distributions.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DiffusionEvaluation
{
    private final static String REC = "-rec";
    private final static String USERFEATS = "-userfeats";
    private final static String INFOFEATS = "-infofeats";
    private final static String N = "-n";
    private final static String TESTGRAPH = "-test-graph";
    private final static String REALPROP = "-realprop";

    /**
     * Evaluates a group of simulations
     * @param args Execution parameters
     * <ol>
     *  <li><b>configuration:</b> a YAML file containing the evaluation metrics and distributions.</li>
     *  <li><b>graphFile:</b> path to a file containing the graph.</li>
     *  <li><b>multigraph:</b> true if the graph has multiple edges between users, false otherwise.</li>
     *  <li><b>directed:</b> true if the graph is directed, false otherwise.</li>
     *  <li><b>weighted:</b> true if the graph is weighted, false otherwise.</li>
     *  <li><b>selfLoops:</b> true if the graph accepts selfloops, false otherwise.</li>
     *  <li><b>readTypes:</b> true if the graph types have to be read from the file, false otherwise.</li>
     *  <li><b>uIndexPath:</b> route of a file containing the list of users.</li>
     *  <li><b>iIndexPath:</b> route of a file containing the list of identifiers of the different information pieces.</li>
     *  <li><b>inputFolder:</b> directory containing the simulations to evaluate.</li>
     *  <li><b>outputFolder:</b> directory to store the evaluation results.</li>
     *  <li><b>infoFile:</b> file containing the relation between users and information pieces.</li>
     *  <li><b>Optional arguments:</b>
     *      <ul>
     *          <li><b>-rec recFile:</b> path to a recommendation file, whose edges will be added to the network.</li>
     *          <li><b>-n n:</b> the number of links (per user) to add from the recommendation (if any). By default: 10</li>
     *          <li><b>-test-graph file:</b> a folder to a network file containing additional edges (and shall be used for filtering the recommended edges to add).</li>
     *          <li><b>-userfeats file1,file2,...,fileN:</b> a comma-separated list of files containing the features for the users in the network (e.g. communities).</li>
     *          <li><b>-infofeats file1,file2,...,fileN:</b> a comma-separated list of files containing the features for the information pieces (e.g. hashtags).</li>
     *          <li><b>-realprop file:</b> a file indicating which information pieces have been repropagated by users in another information diffusion process.</li>
     *      </ul></li>
     * </ol>
     * @throws IOException if something fails while reading / writing data
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 12)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tconfiguration: a YAML file containing the metrics for the evaluation.");
            System.err.println("\tgraphFile: path to a file containing the graph.");
            System.err.println("\tmultigraph: true if the graph has multiple edges between users, false otherwise.");
            System.err.println("\tdirected: true if the graph is directed, false otherwise.");
            System.err.println("\tweighted: true if the graph is weighted, false otherwise.");
            System.err.println("\tselfLoops: true if the graph accepts selfloops, false otherwise.");
            System.err.println("\treadTypes: true if the graph types have to be read from the file, false otherwise.");
            System.err.println("\tuIndexPath: route of a file containing the list of users.");
            System.err.println("\tiIndexPath: route of a file containing the list of identifiers of the different information pieces.");
            System.err.println("\tinputFolder: a directory containing the simulations to evaluate.");
            System.err.println("\toutputFolder: a directory for storing the outcome of the evaluation.");
            System.err.println("\tinputFolder: a directory containing the simulations to evaluate.");
            System.err.println("Optional arguments:");
            System.err.println("\t" + REC + " recFile: path to a recommendation file, whose edges will be added to the network.");
            System.err.println("\t" + N + " n: the number of links (per user) to add from the recommendation (if any). By default: 10");
            System.err.println("\t" + TESTGRAPH + " file: a folder to a network file containing additional edges (and shall be used for filtering the recommended edges to add).");
            System.err.println("\t" + USERFEATS + " file1,file2,...,fileN: a comma-separated list of files containing the features for the users in the network (e.g. communities).");
            System.err.println("\t" + INFOFEATS + " file1,file2,...,fileN: a comma-separated list of files containing the features for the information pieces (e.g. hashtags).");
            System.err.println("\t" + REALPROP + " file: a file indicating which information pieces have been repropagated by users in another information diffusion process.");
            return;
        }

        // We read the metrics to consider:
        String configuration = args[0];

        // We first read the network:
        String graph = args[1];

        boolean multigraph = args[2].equalsIgnoreCase(TRUE);
        boolean directed = args[3].equalsIgnoreCase(TRUE);
        boolean weighted = args[4].equalsIgnoreCase(TRUE);
        boolean selfLoops = args[5].equalsIgnoreCase(TRUE);
        boolean readTypes = args[6].equalsIgnoreCase(TRUE);

        // Indexes and graphs.
        String uIndexPath = args[7];
        String iIndexPath = args[8];

        // Folder containing the simulations
        String inputFolder = args[9];
        // Folder for storing the metrics.
        String outputFolder = args[10];
        // Relation between users and information pieces.
        String infoFile = args[11];

        String[] userFeatFiles = null;
        String[] infoFeatFiles = null;
        String recFile = null;
        int topN = Integer.MAX_VALUE;
        String realProp = null;
        String testGraphFile = null;

        // Read the optional parameters.
        for(int i = 12; i < args.length; ++i)
        {
            if(args[i].equalsIgnoreCase(REC)) // the recommendation file (if any)
            {
                recFile = args[++i];
            }
            else if(args[i].equalsIgnoreCase(USERFEATS)) // the list of files containing the user features.
            {
                userFeatFiles = args[++i].split(",");
            }
            else if(args[i].equalsIgnoreCase(INFOFEATS)) // the list of files containing the item features.
            {
                infoFeatFiles = args[++i].split(",");
            }
            else if(args[i].equalsIgnoreCase(N)) // the number of links to add from each recommendation (at most)
            {
                topN = Parsers.ip.parse(args[++i]);
            }
            else if(args[i].equalsIgnoreCase(REALPROP)) // the information about repropagations.
            {
                realProp = args[++i];
            }
            else if(args[i].equalsIgnoreCase(TESTGRAPH)) // an additional (test) graph used for filtering data.
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

        GraphReader<Long> greader = multigraph ? new TextMultiGraphReader<>(directed, weighted, selfLoops, "\t", Parsers.lp) : new TextGraphReader<>(directed, weighted, selfLoops, "\t", Parsers.lp);
        Graph<Long> testGraph = testGraphFile == null ? null : greader.read(testGraphFile, weighted, readTypes);

        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" + (timeb-timea) + " ms.)");

        // Obtain the simulations to evaluate
        File folder = new File(inputFolder);
        File[] fileList = folder.listFiles();
        if(!folder.isDirectory() || fileList == null || fileList.length == 0)
        {
            System.err.println("Nothing to evaluate!");
            return;
        }
        
        timea = System.currentTimeMillis();

        // Obtain the different filter, metrics and distributions we will use throughout the evaluation.
        SimulationMetricsParameterReader simReader = new SimulationMetricsParameterReader();
        Map<String, Object> map = AuxiliarMethods.readYAML(configuration);
        simReader.read(map);
        
        SimulationMetricsSelector<Long,Long,Long> selector = new SimulationMetricsSelector<>(Parsers.lp);
        Triplet<DataFilter<Long,Long,Long>,List<SimulationMetric<Long,Long,Long>>,List<Tuple2<Distribution<Long,Long,Long>, List<Integer>>>> triplet = selector.select(simReader, Long.MAX_VALUE, testGraph);
        
        // Retrieve the data filter.
        DataFilter<Long,Long,Long> filter = triplet.v1();
        // Retrieve the simulation metrics.
        List<SimulationMetric<Long,Long,Long>> simMetrics = triplet.v2();
        // Retrieve the distributions to find.
        List<Tuple2<Distribution<Long,Long,Long>,List<Integer>>> distribs = triplet.v3();
        
        // Filter the data.
        Data<Long,Long,Long> filteredData = filter.filter(data); 
        
        timeb = System.currentTimeMillis();
        System.out.println("Starting the evaluation (" + (timeb-timea) + " ms.");

        // For each simulation, evaluate:
        for(File f : fileList)
        {
            String file = f.getAbsolutePath();
            if(f.isDirectory()) continue;

            String[] split = file.split("/");
            //String[] split = file.split("\\Q\\\\E");
            String outputFile = split[split.length - 1];

            // First, read the simulation:
            SimulationReader<Long,Long,Long> sreader = new BinarySimulationReader<>();
            sreader.initialize(file);
            Simulation<Long,Long,Long> sim = sreader.readSimulation(filteredData);
            System.out.println("Simulation " + outputFile + " read");

            // Then, for each iteration, update and compute the metrics.
            try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFolder+outputFile+"-metrics.txt"))))
            {
                bw.write("Iteration");
                
                long a = System.currentTimeMillis();
                // First, we initialize the metrics.
                for(SimulationMetric<Long,Long,Long> metric : simMetrics)
                {
                    metric.clear();
                    metric.initialize(filteredData);
                    bw.write("\t" + metric.getName());
                }
                long b = System.currentTimeMillis();
                System.out.println("Metrics initialized (" + (b-a) + " ms.)");
                
                for(int i = sim.getInitialNumber(); i < sim.getNumIterations() + sim.getInitialNumber(); ++i)
                {
                    a = System.currentTimeMillis();

                    Iteration<Long,Long,Long> iter = sim.getIteration(i);
                    bw.write("\n" + iter.getIterationNumber());
                    for(SimulationMetric<Long,Long,Long> metric : simMetrics)
                    {
                        metric.update(iter);
                        bw.write("\t" + metric.calculate());
                    }
                    
                    b = System.currentTimeMillis();
                    System.out.println("Iteration " + iter.getIterationNumber() + " finished (" + (b-a) + " ms.)");
                }
            }
            
            timeb = System.currentTimeMillis();
            System.out.println(outputFile + " finished (" + (timeb-timea) + " ms.");
            
            // Distributions
            for(Tuple2<Distribution<Long,Long,Long>,List<Integer>> distrib : distribs)
            {
                Distribution<Long,Long,Long> d = distrib.v1();
                d.clear();
                d.initialize(filteredData);
                
                String distribFile = outputFolder + outputFile + d.getName();
                
                List<Integer> iters = distrib.v2();
                int j = 0;
                for(int i = 0; i < sim.getNumIterations() && j < iters.size(); ++i)
                {
                    Iteration<Long,Long,Long> iter = sim.getIteration(i);
                    d.update(iter);
                    
                    if(i == iters.get(j))
                    {
                        d.print(distribFile + "-" + iter.getIterationNumber() + ".txt");
                        ++j;
                    }
                }
            }
        }
    }
}
