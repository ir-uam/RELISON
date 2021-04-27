/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.examples.recommendation;


import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.metrics.basic.AverageRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.basic.NDCG;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.runner.RecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilterRecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilters;
import es.uam.eps.ir.socialranksys.AuxiliarMethods;
import es.uam.eps.ir.socialranksys.graph.Adapters;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Configurations;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridReader;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridSelector;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.links.data.FastGraphIndex;
import es.uam.eps.ir.socialranksys.links.data.GraphIndex;
import es.uam.eps.ir.socialranksys.links.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.socialranksys.links.recommendation.SocialFastFilters;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.basic.Random;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import org.ranksys.core.util.tuples.Tuple2od;
import org.ranksys.formats.parsing.Parsers;

import java.io.*;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * Class that executes the validation process for the different contact recommendation algorithms.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Validation
{
    /**
     * Program that performs the validation process for different contact recommendation algorithms.
     * Generates a file comparing weigthed and unweighted algorithm variants.
     *
     * @param args Execution arguments:
     *             <ol>
     *               <li><b>Train:</b> Route to the file containing the training graph.</li>
     *               <li><b>Validation:</b> Route to the file containing the validation links.</li>
     *               <li><b>Algorithms:</b> Route to an XML file containing the recommender configurations</li>
     *               <li><b>Output directory:</b> Directory in which to store the recommendations and the output file.</li>
     *               <li><b>Directed:</b> True if the network is directed, false otherwise.</li>
     *               <li><b>Weighted:</b> True if the network is weighted, false otherwise.</li>
     *               <li><b>Max. Length:</b> Maximum number of recommendations per user.</li>
     *               <li><b>Print recommendations:</b> True if, additionally to the results, you want to print the recommendations. False otherwise</li>
     *             </ol>
     */
    public static void main(String[] args)
    {
        if (args.length < 7)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tTrain: Route to the file containing the training graph.");
            System.err.println("\tValidation: Route to the file containing the validation links.");
            System.err.println("\tAlgorithms: Route to an XML file containing the recommender configuration. Only algorithms with a version without term discrimination will be executed.");
            System.err.println("\tOutput directory: Directory in which to store the recommendations and the output files.");
            System.err.println("\tDirected: True if the network is directed, false otherwise.");
            System.err.println("\tWeighted: True if the network is weighted, false otherwise.");
            System.err.println("\tRec. Length: Maximum number of recommendations per user.");
            System.err.println("\tPrint recommendations: True if, additionally to the results, you want to print the recommendations. False otherwise");
            return;
        }

        // Read the program arguments.
        String trainDataPath = args[0];
        String validationDataPath = args[1];
        String algorithmsPath = args[2];
        String outputPath = args[3];
        boolean directed = args[4].equalsIgnoreCase("true");
        boolean weighted = args[5].equalsIgnoreCase("true");
        int maxLength = Parsers.ip.parse(args[6]);
        boolean printRecs = args[7].equalsIgnoreCase("true");

        long timea = System.currentTimeMillis();
        // Read the training graph.
        TextGraphReader<Long> weightedReader = new TextGraphReader<>(directed, true, false, "\t", Parsers.lp);
        FastGraph<Long> weightedGraph = (FastGraph<Long>) weightedReader.read(trainDataPath, true, false);
        if (weightedGraph == null)
        {
            System.err.println("ERROR: Could not read the training graph");
            return;
        }

        TextGraphReader<Long> unweightedReader = new TextGraphReader<>(directed, false, false, "\t", Parsers.lp);
        FastGraph<Long> unweightedGraph = (FastGraph<Long>) unweightedReader.read(trainDataPath, false, false);
        if (unweightedGraph == null)
        {
            System.err.println("ERROR: Could not read the training graph");
            return;
        }

        // Read the test graph.
        Graph<Long> auxgraph = unweightedReader.read(validationDataPath, false, false);
        FastGraph<Long> validationGraph = (FastGraph<Long>) Adapters.onlyTrainUsers(auxgraph, unweightedGraph);
        if (validationGraph == null)
        {
            System.err.println("ERROR: Could not remove users from the test graph");
            return;
        }

        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" + (timeb - timea) + " ms.)");

        // Read the training and test data
        FastPreferenceData<Long, Long> unweightedTrainData = GraphSimpleFastPreferenceData.load(unweightedGraph);
        FastPreferenceData<Long, Long> weightedTrainData = GraphSimpleFastPreferenceData.load(weightedGraph);

        FastPreferenceData<Long, Long> validationData;
        validationData = GraphSimpleFastPreferenceData.load(validationGraph);
        GraphIndex<Long> index = new FastGraphIndex<>(unweightedGraph);

        // Read the XML containing the parameter grid for each algorithm
        AlgorithmGridReader gridreader = new AlgorithmGridReader(algorithmsPath);
        gridreader.readDocument();

        Set<String> algorithms = gridreader.getAlgorithms();

        int numUsers = validationData.numUsersWithPreferences();

        // For each algorithm.
        algorithms.forEach(algorithm ->
        {
            String directory = outputPath + algorithm + File.separator;
            if(printRecs)
            {
                File file = new File(directory);
                file.mkdir();
            }

            System.out.println("-------- Starting algorithm " + algorithm + " --------");
            long timeaa = System.currentTimeMillis();
            Grid grid = gridreader.getGrid(algorithm);
            Configurations confs = grid.getConfigurations();
            AlgorithmGridSelector<Long> algorithmSelector = new AlgorithmGridSelector<>(Parsers.lp);

            // Configure the recommender runner
            @SuppressWarnings("unchecked")
            Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(unweightedTrainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(unweightedGraph, index));
            RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(index, index, validationData.getUsersWithPreferences(), filter, maxLength);
            List<Parameters> configurations = confs.getConfigurations();

            if(algorithm.equals(AlgorithmIdentifiers.IB) || algorithm.equals(AlgorithmIdentifiers.UB))
            {
                Map<String, List<Parameters>> similarityDivision = new HashMap<>();
                configurations.forEach(conf ->
                {
                    String name = conf.getParamValues().get("sim").v1();
                    if(!similarityDivision.containsKey(name)) similarityDivision.put(name, new ArrayList<>());
                    similarityDivision.get(name).add(conf);
                });

                similarityDivision.keySet().forEach(sim ->
                {
                    System.out.println("-------- Starting algorithm " + algorithm + " with similarity " + sim + " --------");
                    String directorySim = outputPath + algorithm + File.separator + sim + File.separator;
                    if(printRecs)
                    {
                        File file = new File(directorySim);
                        file.mkdir();
                    }

                    List<Parameters> configs = similarityDivision.get(sim);

                    Validation.executeValidation(algorithm, configs, algorithmSelector,
                                                    validationData, runner,
                                                    weightedGraph, unweightedGraph, weightedTrainData, unweightedTrainData,
                                                    numUsers, maxLength, weighted, printRecs,
                                                    directorySim, outputPath, algorithm+"_"+sim);

                });
            }
            else
            {
                Validation.executeValidation(algorithm, configurations, algorithmSelector,
                        validationData, runner,
                        weightedGraph, unweightedGraph, weightedTrainData, unweightedTrainData,
                        numUsers, maxLength, weighted, printRecs,
                        directory, outputPath, algorithm);

            }
            long timecc = System.currentTimeMillis();
            System.out.println("-------- Finished algorithm " + algorithm + " (" + (timecc-timeaa) + " ms.) --------");
        });
    }


    /**
     * Executes the validation for an algorithm.
     * @param algorithm the algorithm identifier.
     * @param configurations the list of configurations of the algorithm.
     * @param algorithmSelector algorithm selector.
     * @param validationData validation data.
     * @param runner recommder runner.
     * @param weightedGraph weighted training graph.
     * @param unweightedGraph unweighted training graph.
     * @param unweightedTrainData unweighted training data.
     * @param weightedTrainData weighted training data.
     * @param numUsers number of users.
     * @param maxLength maximum length.
     * @param weighted true if the graph is weighted, false otherwise.
     * @param printRecs print the recommendations.
     * @param directory directory to store the recommendations.
     * @param outputPath directory where to store the ranking.
     * @param confname name of the ranking file.
     */
    private static void executeValidation(String algorithm, List<Parameters> configurations, AlgorithmGridSelector<Long> algorithmSelector,
                                          FastPreferenceData<Long,Long> validationData, RecommenderRunner<Long, Long> runner,
                                          FastGraph<Long> weightedGraph, FastGraph<Long> unweightedGraph,
                                          FastPreferenceData<Long, Long> weightedTrainData, FastPreferenceData<Long, Long> unweightedTrainData,
                                          int numUsers, int maxLength, boolean weighted,
                                          boolean printRecs, String directory, String outputPath, String confname)
    {
        int totalCount = configurations.size();
        AtomicInteger counter = new AtomicInteger();
        long timeaa = System.currentTimeMillis();
        PriorityBlockingQueue<Tuple2od<String>> ranking = new PriorityBlockingQueue<>(totalCount, (x, y) -> Double.compare(y.v2, x.v2));

        // Now, execute each possible variant.
        configurations.parallelStream().forEach(parameters ->
        {
            Tuple2oo<String, RecommendationAlgorithmFunction<Long>> algSupp = algorithmSelector.getRecommender(algorithm, parameters);
            String algorithmName = algSupp.v1();

            // First, obtain the metric.
            NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, validationData, 0.5);
            SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), numUsers);

            try {
                Recommender<Long, Long> weightedAlg = new Random<>(unweightedGraph);
                Recommender<Long, Long> unweightedAlg = algSupp.v2().apply(unweightedGraph, unweightedTrainData);

                if (weighted) {
                    weightedAlg = algSupp.v2().apply(weightedGraph, weightedTrainData);
                }

                double weightedValue = 0;
                double unweightedValue;

                if (printRecs) // If we want to print the recommendations
                {
                    if (weighted) {
                        weightedValue = AuxiliarMethods.computeAndEvaluate(directory + "wei_" + algorithmName + ".txt", weightedAlg, runner, nDCG);
                    }
                    unweightedValue = AuxiliarMethods.computeAndEvaluate(directory + (weighted ? "unw_" : "") + algorithmName + ".txt", unweightedAlg, runner, nDCG);
                } else // Otherwise
                {
                    if (weighted) {
                        weightedValue = AuxiliarMethods.computeAndEvaluate(weightedAlg, runner, nDCG);
                    }
                    unweightedValue = AuxiliarMethods.computeAndEvaluate(unweightedAlg, runner, nDCG);
                }


                // Store the nDCG values.
                if (weighted) {
                    ranking.add(new Tuple2od<>("wei_" + algorithmName, weightedValue));
                    ranking.add(new Tuple2od<>("unw_" + algorithmName, unweightedValue));
                } else {
                    ranking.add(new Tuple2od<>(algorithmName, unweightedValue));
                }

            } catch (IOException ioe) {
                System.err.println("ERROR: Something failed while executing " + algorithmName);
            }

            long timebb = System.currentTimeMillis();
            System.out.println("Algorithm " + counter.incrementAndGet() + "/" + totalCount + ": " + algorithmName + " finished (" + (timebb - timeaa) + " ms.)");
        });

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "validation_" + confname + ".txt")))) {
            bw.write("Ranking\tVariant\tnDCG@" + maxLength);
            int i = 1;
            while (!ranking.isEmpty()) {
                Tuple2od<String> tuple = ranking.poll();
                bw.write("\n" + i + "\t" + tuple.v1 + "\t" + tuple.v2);
                ++i;
            }
        } catch (IOException ioe) {
            System.err.println("ERROR: Something failed while writing the output file for algorithm " + confname);
        }
    }


}
