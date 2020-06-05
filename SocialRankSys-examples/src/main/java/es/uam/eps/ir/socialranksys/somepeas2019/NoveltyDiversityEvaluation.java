/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.somepeas2019;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.feature.SimpleFeatureData;
import es.uam.eps.ir.ranksys.diversity.sales.metrics.GiniIndex;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.novdiv.distance.CosineFeatureItemDistanceModel;
import es.uam.eps.ir.ranksys.novdiv.distance.ItemDistanceModel;
import es.uam.eps.ir.ranksys.novelty.longtail.PCItemNovelty;
import es.uam.eps.ir.ranksys.novelty.unexp.PDItemNovelty;
import es.uam.eps.ir.socialranksys.AuxiliarMethods;
import es.uam.eps.ir.socialranksys.graph.Adapters;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.links.data.FastGraphIndex;
import es.uam.eps.ir.socialranksys.links.data.GraphIndex;
import es.uam.eps.ir.socialranksys.links.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.socialranksys.links.recommendation.features.LuceneFeaturesReader;
import es.uam.eps.ir.socialranksys.links.recommendation.metrics.novdiv.ILD;
import es.uam.eps.ir.socialranksys.links.recommendation.metrics.novdiv.LTN;
import es.uam.eps.ir.socialranksys.links.recommendation.metrics.novdiv.MeanPredictionDistance;
import es.uam.eps.ir.socialranksys.links.recommendation.metrics.novdiv.Unexpectedness;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import org.ranksys.formats.feature.SimpleFeaturesReader;
import org.ranksys.formats.parsing.Parsers;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.TRECRecommendationFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.ranksys.formats.parsing.Parsers.lp;

/**
 * Class for reproducing the experiments for the EWC1 axiom.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class NoveltyDiversityEvaluation
{
    /**
     * Program that reproduces the experiments for the EWC1 axiom.
     * Generates a file comparing weigthed and unweighted algorithm variants.
     *
     * @param args Execution arguments:
     *             <ol>
     *               <li><b>Train:</b> Route to the file containing the training graph.</li>
     *               <li><b>Test:</b> Route to the file containing the test links.</li>
     *               <li><b>Recommendation directory:</b> Directory from which to retrieve the recommendations.</li>
     *               <li><b>Index Feature files:</b> index with features</li>
     *               <li><b>User features:</b> Community-based features.</li>
     *               <li><b>Output file:</b> Route to the output file</li>
     *               <li><b>Directed:</b> True if the network is directed, false otherwise.</li>
     *               <li><b>Rec. Length:</b> Maximum number of recommendations per user.</li>
     *               <li><b>All users: </b> true if we want to generate recommendations for all users in the training set, false if only for those who have test links</li>
     *             </ol>
     */
    public static void main(String[] args) throws IOException
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
        String recPath = args[2];
        String indexFeatures = args[3];
        String commFeatures = args[4];
        String outputPath = args[5];
        boolean directed = args[6].equalsIgnoreCase("true");
        int maxLength = Parsers.ip.parse(args[7]);
        boolean allUsers = args[8].equalsIgnoreCase("true");

        // Initialize the maps to store the accuracy values.
        Map<String, Double> LTNValues = new ConcurrentHashMap<>();
        Map<String, Double> UnexpValues = new ConcurrentHashMap<>();
        Map<String, Double> ILDValues = new ConcurrentHashMap<>();
        Map<String, Double> GiniPValues = new ConcurrentHashMap<>();
        Map<String, Double> MPDValues = new ConcurrentHashMap<>();

        // Read the test graph
        TextGraphReader<Long> testGraphReader = new TextGraphReader<>(directed, false, false, "\t", Parsers.lp);
        Graph<Long> auxTestGraph = testGraphReader.read(testDataPath, false, false);

        if (auxTestGraph == null)
        {
            System.err.println("ERROR: Could not read the test graph");
            return;
        }


        System.out.println("-------- Started evaluating variants --------");
        long timea = System.currentTimeMillis();

        // Read the training graph.
        TextGraphReader<Long> greader = new TextGraphReader<>(directed, false, false, "\t", Parsers.lp);
        FastGraph<Long> graph = (FastGraph<Long>) greader.read(trainDataPath);
        if (graph == null)
        {
            System.err.println("ERROR: Could not read the training graph");
            return;
        }

        // Read the test graph.
        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" + (timeb - timea) + " ms.)");
        timea = System.currentTimeMillis();

        // Prepare the training and test data
        FastPreferenceData<Long, Long> trainData;
        trainData = GraphSimpleFastPreferenceData.load(graph);

        // Clean the test graph.
        FastGraph<Long> testGraph = (FastGraph<Long>) Adapters.onlyTrainUsers(auxTestGraph, graph);
        FastPreferenceData<Long, Long> testData;
        testData = GraphSimpleFastPreferenceData.load(testGraph);
        GraphIndex<Long> index = new FastGraphIndex<>(graph);

        File file = new File(recPath);
        if(!file.isDirectory())
        {
            System.err.println("Nothing to evaluate!");
            return;
        }

        String[] recList = file.list();
        if(recList == null)
        {
            System.err.println("Nothing to evaluate!");
            return;
        }

        FeatureData<Long, String, Double> indexData = SimpleFeatureData.load(LuceneFeaturesReader.load(indexFeatures, graph));
        FeatureData<Long, String, Double> commData = SimpleFeatureData.load(SimpleFeaturesReader.get().read(commFeatures, Parsers.lp, Parsers.sp));
        DistanceCalculator<Long> calculator = new DistanceCalculator<>();

        RecommendationFormat<Long, Long> format = new TRECRecommendationFormat<>(lp,lp);
        int numUsers = testData.numUsersWithPreferences();

        int numFiles = 0;
        for(String rec : recList)
        {
            File auxFile = new File(rec);
            if(!auxFile.isDirectory()) numFiles++;
        }

        int i = 0;
        for(String rec : recList)
        {
            File auxFile = new File(rec);
            if(!auxFile.isDirectory()) continue;

            System.out.println("Evaluating " + rec + " (" + i + "/" + numFiles + ")");

            SystemMetric<Long, Long> LTN = new LTN<>(maxLength, new PCItemNovelty<>(trainData));
            ItemDistanceModel<Long> itemDistanceModel = new CosineFeatureItemDistanceModel<>(indexData);
            SystemMetric<Long, Long> Unexp = new Unexpectedness<>(maxLength, new PDItemNovelty<>(true, trainData, itemDistanceModel));
            SystemMetric<Long, Long> ILD = new ILD<>(maxLength, itemDistanceModel);
            SystemMetric<Long, Long> Gini = new GiniIndex<>(maxLength, trainData.numItems());
            SystemMetric<Long, Long> MPD = new MeanPredictionDistance<>(graph, calculator, maxLength);

            Map<String, SystemMetric<Long, Long>> metrics = new HashMap<>();
            metrics.put("LTN", LTN);
            metrics.put("Unexpectedness", Unexp);
            metrics.put("ILD", ILD);
            metrics.put("Gini", Gini);
            metrics.put("MPD", MPD);

            format.getReader(recPath+rec).readAll().forEach(recom ->
            {
                if (recom != null && recom.getItems() != null && !recom.getItems().isEmpty())
                {
                    metrics.values().forEach(metric -> metric.add(recom));
                }
            });

            LTNValues.put(rec, metrics.get("LTN").evaluate());
            UnexpValues.put(rec, metrics.get("Unexpectedness").evaluate());
            ILDValues.put(rec, metrics.get("ILD").evaluate());
            GiniPValues.put(rec, metrics.get("Gini").evaluate());
            MPDValues.put(rec, metrics.get("MPD").evaluate());
            System.out.println("Finished evaluating " + rec + " (" + i + "/" + numFiles + ")");

        }

        List<String> ids = new ArrayList<>();
        List<Map<String, Double>> values = new ArrayList<>();
        ids.add("LTN"); values.add(LTNValues);
        ids.add("Unexpectedness"); values.add(UnexpValues);
        ids.add("ILD"); values.add(ILDValues);
        ids.add("Gini"); values.add(GiniPValues);
        ids.add("MPD"); values.add(MPDValues);
        // Print the file.
        AuxiliarMethods.printFile(outputPath + "evaluation.txt", values, ids, maxLength);




    }
}
