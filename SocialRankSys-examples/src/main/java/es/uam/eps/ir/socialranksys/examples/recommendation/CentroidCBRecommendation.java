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
import es.uam.eps.ir.ranksys.metrics.basic.Precision;
import es.uam.eps.ir.ranksys.metrics.basic.Recall;
import es.uam.eps.ir.ranksys.metrics.rel.BinaryRelevanceModel;
import es.uam.eps.ir.ranksys.metrics.rel.IdealRelevanceModel;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.runner.RecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilterRecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilters;
import es.uam.eps.ir.socialranksys.AuxiliarMethods;
import es.uam.eps.ir.socialranksys.content.index.ForwardIndex;
import es.uam.eps.ir.socialranksys.content.index.individual.WrapperIndividualForwardContentIndex;
import es.uam.eps.ir.socialranksys.content.index.lucene.LuceneForwardIndex;
import es.uam.eps.ir.socialranksys.graph.Adapters;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.links.data.FastGraphIndex;
import es.uam.eps.ir.socialranksys.links.data.GraphIndex;
import es.uam.eps.ir.socialranksys.links.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.socialranksys.links.recommendation.SocialFastFilters;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.contentbased.CentroidCBRecommender;
import es.uam.eps.ir.socialranksys.links.recommendation.metrics.accuracy.TRECAveragePrecision;
import org.ranksys.formats.parsing.Parsers;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

/**
 * Class for reproducing the experiments for the EWC1 axiom.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CentroidCBRecommendation
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
    public static void main(String[] args)
    {
        if (args.length < 7)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tTrain: Route to the file containing the training graph.");
            System.err.println("\tTest: Route to the file containing the test links.");
            System.err.println("\tAlgorithms: Route to an XML file containing the recommender configurations.");
            System.err.println("\tOutput directory: Directory in which to store the recommendations and the output file.");
            System.err.println("\tDirected: True if the network is directed, false otherwise.");
            System.err.println("\tRec. Length: Maximum number of recommendations per user.");
            System.err.println("\tPrint recommendations: True if, additionally to the results, you want to print the recommendations. False otherwise");
            return;
        }

        // Read the execution arguments:
        String trainDataPath = args[0];
        String testDataPath = args[1];
        String outputPath = args[2];
        boolean directed = args[3].equalsIgnoreCase("true");
        boolean weighted = args[4].equalsIgnoreCase("true");
        String indexFolder = args[5];
        boolean own = args[6].equalsIgnoreCase("own");
        EdgeOrientation orient = own ? EdgeOrientation.UND : EdgeOrientation.valueOf(args[6]);
        int maxLength = Parsers.ip.parse(args[7]);
        boolean printRecommenders = args[8].equalsIgnoreCase("true");
        boolean allUsers = args[9].equalsIgnoreCase("true");

        // Initialize the maps to store the accuracy values.
        Map<String, Double> PValues = new ConcurrentHashMap<>();
        Map<String, Double> RValues = new ConcurrentHashMap<>();
        Map<String, Double> nDCGValues = new ConcurrentHashMap<>();
        Map<String, Double> MAPValues = new ConcurrentHashMap<>();

        // First, we do create the directories.
        if (printRecommenders)
        {
            File weightedDirectory = new File(outputPath + "weighted" + File.separator);
            weightedDirectory.mkdirs();
            File unweightedDirectory = new File(outputPath + "unweighted" + File.separator);
            unweightedDirectory.mkdirs();
        }

        // Read the test graph
        TextGraphReader<Long> testGraphReader = new TextGraphReader<>(directed, false, false, "\t", Parsers.lp);
        Graph<Long> auxTestGraph = testGraphReader.read(testDataPath, false, false);

        if (auxTestGraph == null)
        {
            System.err.println("ERROR: Could not read the test graph");
            return;
        }

        System.out.println("-------- Started " + (weighted ? "weighted" : "unweighted") + " variants --------");
        long timea = System.currentTimeMillis();

        // Read the training graph.
        TextGraphReader<Long> greader = new TextGraphReader<>(directed, weighted, false, "\t", Parsers.lp);
        FastGraph<Long> graph = (FastGraph<Long>) greader.read(trainDataPath);
        if (graph == null)
        {
            System.err.println("ERROR: Could not read the training graph");
            return;
        }

        // Read the test graph.
        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" + (timeb - timea) + " ms.)");

        // Prepare the training and test data
        FastPreferenceData<Long, Long> trainData;
        trainData = GraphSimpleFastPreferenceData.load(graph);

        // Clean the test graph.
        FastGraph<Long> testGraph = (FastGraph<Long>) Adapters.onlyTrainUsers(auxTestGraph, graph);
        FastPreferenceData<Long, Long> testData;
        testData = GraphSimpleFastPreferenceData.load(testGraph);
        GraphIndex<Long> index = new FastGraphIndex<>(graph);

        // Select the set of users to be recommended, the format, and the filters to apply to the recommendation
        Set<Long> targetUsers = allUsers ? trainData.getAllUsers().collect(Collectors.toCollection(HashSet::new)) : testData.getUsersWithPreferences().collect(Collectors.toCollection(HashSet::new));
        System.out.println("Num. target users: " + targetUsers.size());

        // Prepare the elements for the recommendation:
        @SuppressWarnings("unchecked") Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(trainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(graph, index));
        RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(index, index, targetUsers.stream(), filter, maxLength);
        int numUsers = testData.numUsersWithPreferences();

        IdealRelevanceModel<Long, Long> idealModel = new BinaryRelevanceModel<>(true, testData, 0.5);
        NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, testData, 0.5);

        try
        {
            ForwardIndex<Long> contentIndex = new LuceneForwardIndex<>(indexFolder, Parsers.lp);
            WrapperIndividualForwardContentIndex<Long, Long> forwardIndex = new WrapperIndividualForwardContentIndex<>(indexFolder, contentIndex, Parsers.lp);

            long a = System.currentTimeMillis();
            String name = "CentroidCB";
            String path = outputPath + File.separator + name + ".txt";

            // First, create the nDCG metric (for measuring accuracy)
            SystemMetric<Long, Long> P = new AverageRecommendationMetric<>(new Precision<>(maxLength, idealModel), numUsers);
            SystemMetric<Long, Long> R = new AverageRecommendationMetric<>(new Recall<>(maxLength, idealModel), numUsers);
            SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), numUsers);
            SystemMetric<Long, Long> MAP = new AverageRecommendationMetric<>(new TRECAveragePrecision<>(maxLength, idealModel), numUsers);
            Map<String, SystemMetric<Long, Long>> metrics = new HashMap<>();
            metrics.put("p", P);
            metrics.put("r", R);
            metrics.put("ndcg", nDCG);
            metrics.put("map", MAP);

            // Prepare the recommender
            Recommender<Long, Long> rec;
            if(own)
                rec = new CentroidCBRecommender<>(graph, forwardIndex);
            else
                rec = new CentroidCBRecommender<>(graph, forwardIndex, orient);

            long b = System.currentTimeMillis();
            System.out.println("The centroids have been built (" + (b-a) + " ms.)");

            // Obtain the nDCG value
            Map<String, Double> values;
            try
            {
                if (printRecommenders)
                {
                    values = AuxiliarMethods.computeAndEvaluate(path, rec, runner, metrics);

                    PValues.put(name, values.get("p"));
                    RValues.put(name, values.get("r"));
                    nDCGValues.put(name, values.get("ndcg"));
                    MAPValues.put(name, values.get("map"));
                }
                else
                {
                    values = AuxiliarMethods.computeAndEvaluate(rec, runner, metrics);

                    PValues.put(name, values.get("p"));
                    RValues.put(name, values.get("r"));
                    nDCGValues.put(name, values.get("ndcg"));
                    MAPValues.put(name, values.get("map"));
                }
            }
            catch (IOException ioe)
            {
                System.err.println("Algorithm " + name + " failed");
            }

            b = System.currentTimeMillis();
            System.err.println("Algorithm " + name + " finished (" + (b - a) + " ms.)");

        }
        catch (IOException ioe)
        {
            System.err.println("Something failed while reading index " + indexFolder);
        }


        List<String> ids = new ArrayList<>();
        List<Map<String, Double>> values = new ArrayList<>();
        ids.add("P");
        values.add(PValues);
        ids.add("R");
        values.add(RValues);
        ids.add("nDCG");
        values.add(nDCGValues);
        ids.add("MAP");
        values.add(MAPValues);
        // Print the file.
        AuxiliarMethods.printFile(outputPath + "evaluation.txt", values, ids, maxLength);
    }
}