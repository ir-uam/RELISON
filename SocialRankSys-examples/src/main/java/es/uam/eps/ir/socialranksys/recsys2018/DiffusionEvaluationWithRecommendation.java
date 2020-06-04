/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.recsys2018;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.socialranksys.diffusion.io.DataReader;
import es.uam.eps.ir.socialranksys.diffusion.io.backup.BinarySimulationReader;
import es.uam.eps.ir.socialranksys.diffusion.io.backup.SimulationReader;
import es.uam.eps.ir.socialranksys.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.metrics.distributions.Distribution;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Simulation;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.grid.diffusion.SimulationMetricsParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.SimulationMetricsSelector;
import es.uam.eps.ir.socialranksys.io.graph.GraphReader;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.io.graph.TextMultiGraphReader;
import es.uam.eps.ir.socialranksys.utils.datatypes.Triplet;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import org.ranksys.formats.parsing.Parsers;

import java.io.*;
import java.util.List;

/**
 * Executes simulations over a given graph.
 * @author Javier Sanz-Cruzado Puig
 */
public class DiffusionEvaluationWithRecommendation
{
    /**
     * Evaluates a group of simulations
     * @param args Execution parameters
     * <ol>
     *  <li><b>User Index Path:</b> Route to the file containing the list of users</li>
     *  <li><b>Information Index Path:</b> Route to the file containing the identifiers of the information pieces</li>
     *  <li><b>Graph file:</b> The route to the file where the training graph is stored </li>
     *  <li><b>Multigraph:</b> True if the graph is multigraph, false if it is not.</li>
     *  <li><b>Directed:</b> True if the graph is directed, false if it is not</li>
     *  <li><b>Weighted:</b> True if the graph is weighted, false if it is not</li>
     *  <li><b>Read Types:</b> True if the types of the edges have to be read, false it not</li>
     *  <li><b>Recommendation file:</b> File containing the recommended files, in TREC format</li>
     *  <li><b>Top N:</b> The maximum number of links to pick on each recommendation</li>
     *  <li><b>Information file: </b> Route to the file where the relation between users and information pieces is stored </li>
     *  <li><b>User feature files: </b> Separated by commas, the list of files which contain the features for the nodes in the graph </li>
     *  <li><b>Information pieces features files: </b> Separated by commas, the list of files containing the features for the information pieces</li>
     *  <li><b>Real propagated information:</b> A file containing a relation between users and propagated information</li>
     *  <li><b>Configuration:</b> Configuration file for the simulations to run</li>
     *  <li><b>Input folder: </b> Folder in which the simulations are stored.</li>
     *  <li><b>Output folder: </b> Folder in which the output metrics files will be stored.</li>
     * </ol>
     * @throws IOException if something fails while reading / writing data
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 17)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tuIndexPath: Path to the a file containing the list of users");
            System.err.println("\tiIndexPath: Path to a file containing a list of information pieces identifiers");
            System.err.println("\tgraphFile: Path to a file containing a graph");
            System.err.println("\tmultigraph: Indicates if the graph is (true) or not (false) a multigraph");
            System.err.println("\tdirected: Indicates if the graph is directed (true) or not (false)");
            System.err.println("\tweighted: Indicates if the graph is weighted (true) or not (false)");
            System.err.println("\treadTypes: Indicates if the graph types have to be read from file (true) or not (false)");
            System.err.println("\trecFile: Path to the recommendation (in TREC format)");
            System.err.println("\ttopN: Maximum number of users to obtain from each recommendation");
            System.err.println("\tinfoFile: File containing the relation between users and info. pieces");
            System.err.println("\tuserFeatureFiles: Comma-separated, files containing the features for the nodes in the graph");
            System.err.println("\tinfoPiecesFeatureFiles: Comma-separated, files containing the features for the information pieces");
            System.err.println("\trealPropagatedInfoFile: A file containing a relation between users and propagated information");
            System.err.println("\tconfiguration: XML file containing the configuration for the simulation evaluation");
            System.err.println("\tinputFolder: Directory containing the simulations to evaluate");
            System.err.println("\toutputFolder: Directory for storing the outcomes of the evaluation");
            return;
        }
        
        // Indexes and graphs.
        String uIndexPath = args[0];
        String iIndexPath = args[1];
        String graphFile = args[2];
        String testGraphFile = args[3];

        // Parameters of the graph.
        boolean multigraph = args[4].equalsIgnoreCase("true");
        boolean directed = args[5].equalsIgnoreCase("true");
        boolean weighted = args[6].equalsIgnoreCase("true");
        boolean readTypes = args[7].equalsIgnoreCase("true");

        // The recommendation file
        String recFile = args[8];
        // Number of users to take from each recommendation
        int topN = Parsers.ip.parse(args[9]);
        // Information pieces file
        String infoFile = args[10];
        // User and information pieces features
        String[] userParamFiles = args[11].split(",");
        String[] infoPiecesParamFiles = args[12].split(",");
        // Real propagated information
        String realPropInfo = args[13];

        // Metric configuration
        String configuration = args[14];

        // Folder containing the simulations
        String inputFolder = args[15];
        // Folder for storing the metrics.
        String outputFolder = args[16];
                
        // Read the data
        long timea = System.currentTimeMillis();
        DataReader<Long, Long, Long> datareader = new DataReader<>();
        Data<Long,Long,Long> data = datareader.readData(multigraph, directed, weighted, readTypes, uIndexPath, iIndexPath, graphFile, recFile, topN, infoFile, userParamFiles, infoPiecesParamFiles, realPropInfo, Parsers.lp, Parsers.lp, Parsers.lp);
        GraphReader<Long> greader = multigraph ? new TextMultiGraphReader<>(directed, weighted, false, "\t", Parsers.lp) : new TextGraphReader<>(directed, weighted, false, "\t", Parsers.lp);
        Graph<Long> testGraph = greader.read(testGraphFile, weighted, readTypes);
        long timeb = System.currentTimeMillis();
        
        System.out.println("Data read (" + (timeb-timea) + " ms.)");

        // Obtain the simulations to evaluate
        File folder = new File(inputFolder);
        if(!folder.isDirectory() || folder.listFiles() == null || folder.listFiles().length == 0)
        {
            System.err.println("Nothing to evaluate!");
            return;
        }
        
        timea = System.currentTimeMillis();

        // Obtain the different filter, metrics and distributions we will use throughout the evaluation.
        SimulationMetricsParamReader simReader = new SimulationMetricsParamReader(configuration);
        simReader.readDocument();
        
        SimulationMetricsSelector<Long,Long,Long> selector = new SimulationMetricsSelector<>(Parsers.lp);
        Triplet<DataFilter<Long,Long,Long>,List<SimulationMetric<Long,Long,Long>>,List<Tuple2oo<Distribution<Long,Long,Long>, List<Integer>>>> triplet = selector.select(simReader, Long.MAX_VALUE, testGraph);
        
        // Retrieve the data filter.
        DataFilter<Long,Long,Long> filter = triplet.v1();
        
        // Retrieve the simulation metrics.
        List<SimulationMetric<Long,Long,Long>> simMetrics = triplet.v2();
        
        // Retrieve the distributions to find.
        List<Tuple2oo<Distribution<Long,Long,Long>,List<Integer>>> distribs = triplet.v3();
        
        // Filter the data.
        Data<Long,Long,Long> filteredData = filter.filter(data); 
        
        timeb = System.currentTimeMillis();
        System.out.println("Starting the evaluation (" + (timeb-timea) + " ms.");

        // For each simulation, evaluate:
        for(String file : folder.list())
        {
            String[] split = file.split("/");
            //String[] split = file.split("\\Q\\\\E");
            String outputFile = split[split.length - 1];

            // First, read the simulation:
            SimulationReader<Long,Long,Long> sreader = new BinarySimulationReader<>();
            sreader.initialize(inputFolder + file);
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
            for(Tuple2oo<Distribution<Long,Long,Long>,List<Integer>> distrib : distribs)
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
