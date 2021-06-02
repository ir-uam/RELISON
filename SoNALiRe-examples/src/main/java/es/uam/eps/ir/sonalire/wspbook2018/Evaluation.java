/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.wspbook2018;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.metrics.basic.AverageRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.basic.NDCG;
import es.uam.eps.ir.ranksys.metrics.basic.Precision;
import es.uam.eps.ir.ranksys.metrics.basic.Recall;
import es.uam.eps.ir.ranksys.metrics.rel.BinaryRelevanceModel;
import es.uam.eps.ir.ranksys.metrics.rel.IdealRelevanceModel;
import es.uam.eps.ir.sonalire.AuxiliarMethods;
import es.uam.eps.ir.sonalire.graph.Adapters;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.io.graph.TextGraphReader;
import es.uam.eps.ir.sonalire.links.data.FastGraphIndex;
import es.uam.eps.ir.sonalire.links.data.GraphIndex;
import es.uam.eps.ir.sonalire.links.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.sonalire.links.recommendation.metrics.accuracy.TRECAveragePrecision;
import org.ranksys.formats.parsing.Parsers;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;
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
public class Evaluation
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
        String outputPath = args[3];
        boolean directed = args[4].equalsIgnoreCase("true");
        int maxLength = Parsers.ip.parse(args[5]);
        String formatName = args[6];

        // Initialize the maps to store the accuracy values.
        Map<String, Double> PValues = new ConcurrentHashMap<>();
        Map<String, Double> RValues = new ConcurrentHashMap<>();
        Map<String, Double> nDCGValues = new ConcurrentHashMap<>();
        Map<String, Double> MAPValues = new ConcurrentHashMap<>();

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

        RecommendationFormat<Long, Long> format = (formatName.equalsIgnoreCase("trec") ? new TRECRecommendationFormat<>(lp,lp) : new SimpleRecommendationFormat<>(lp,lp));
        int numUsers = testData.numUsersWithPreferences();
        IdealRelevanceModel<Long, Long> idealModel = new BinaryRelevanceModel<>(true, testData, 0.5);
        NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, testData, 0.5);

        int numFiles = 0;
        for(String rec : recList)
        {
            File auxFile = new File(recPath+rec);
            if(!auxFile.isDirectory()) numFiles++;
        }

        System.out.println("Number of recommenders to evaluate: " + numFiles);

        int i = 0;
        for(String rec : recList)
        {
            File auxFile = new File(recPath + rec);
            if(auxFile.isDirectory()) continue;

            System.out.println("Evaluating " + rec + " (" + i + "/" + numFiles + ")");

            SystemMetric<Long, Long> P = new AverageRecommendationMetric<>(new Precision<>(maxLength, idealModel), numUsers);
            SystemMetric<Long, Long> R = new AverageRecommendationMetric<>(new Recall<>(maxLength, idealModel), numUsers);
            SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), numUsers);
            SystemMetric<Long, Long> MAP = new AverageRecommendationMetric<>(new TRECAveragePrecision<>(maxLength, idealModel), numUsers);
            Map<String, SystemMetric<Long, Long>> metrics = new HashMap<>();
            metrics.put("p", P);
            metrics.put("r", R);
            metrics.put("ndcg", nDCG);
            metrics.put("map", MAP);

            format.getReader(recPath+rec).readAll().forEach(recom ->
            {
                if (recom != null && recom.getItems() != null && !recom.getItems().isEmpty())
                {
                    metrics.values().forEach(metric -> metric.add(recom));
                }
            });

            PValues.put(rec, metrics.get("p").evaluate());
            RValues.put(rec, metrics.get("r").evaluate());
            nDCGValues.put(rec, metrics.get("ndcg").evaluate());
            MAPValues.put(rec, metrics.get("map").evaluate());

            ++i;
            System.out.println("Finished evaluating " + rec + " (" + i + "/" + numFiles + ")");

        }

        List<String> ids = new ArrayList<>();
        List<Map<String, Double>> values = new ArrayList<>();
        ids.add("P"); values.add(PValues);
        ids.add("R"); values.add(RValues);
        ids.add("nDCG"); values.add(nDCGValues);
        ids.add("MAP"); values.add(MAPValues);
        // Print the file.
        AuxiliarMethods.printFile(outputPath + "evaluation.txt", values, ids, maxLength);
    }
}
