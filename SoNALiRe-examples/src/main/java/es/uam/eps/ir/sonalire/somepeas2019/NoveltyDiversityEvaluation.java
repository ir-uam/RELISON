/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.somepeas2019;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.feature.SimpleFeatureData;
import es.uam.eps.ir.ranksys.core.preference.ConcatPreferenceData;
import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.ranksys.diversity.intentaware.FeatureIntentModel;
import es.uam.eps.ir.ranksys.diversity.intentaware.IntentModel;
import es.uam.eps.ir.ranksys.diversity.intentaware.metrics.ERRIA;
import es.uam.eps.ir.ranksys.diversity.other.metrics.SRecall;
import es.uam.eps.ir.ranksys.diversity.sales.metrics.GiniIndex;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.metrics.basic.AverageRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.rel.NoRelevanceModel;
import es.uam.eps.ir.ranksys.metrics.rel.RelevanceModel;
import es.uam.eps.ir.ranksys.novdiv.distance.CosineFeatureItemDistanceModel;
import es.uam.eps.ir.ranksys.novdiv.distance.ItemDistanceModel;
import es.uam.eps.ir.ranksys.novelty.longtail.PCItemNovelty;
import es.uam.eps.ir.ranksys.novelty.unexp.PDItemNovelty;
import es.uam.eps.ir.sonalire.AuxiliarMethods;
import es.uam.eps.ir.sonalire.graph.Adapters;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.io.graph.TextGraphReader;
import es.uam.eps.ir.sonalire.links.data.FastGraphIndex;
import es.uam.eps.ir.sonalire.links.data.GraphIndex;
import es.uam.eps.ir.sonalire.links.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.sonalire.links.recommendation.features.LuceneTfIdfFeaturesReader;
import es.uam.eps.ir.sonalire.links.recommendation.metrics.novdiv.ILD;
import es.uam.eps.ir.sonalire.links.recommendation.metrics.novdiv.LTN;
import es.uam.eps.ir.sonalire.links.recommendation.metrics.novdiv.MeanPredictionDistance;
import es.uam.eps.ir.sonalire.links.recommendation.metrics.novdiv.Unexpectedness;
import es.uam.eps.ir.sonalire.metrics.distance.FastDistanceCalculator;
import org.ranksys.formats.feature.SimpleFeaturesReader;
import org.ranksys.formats.parsing.Parsers;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

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

        // Initialize the maps to store the accuracy values.
        Map<String, Double> LTNValues = new ConcurrentHashMap<>();
        Map<String, Double> UnexpValues = new ConcurrentHashMap<>();
        Map<String, Double> ILDValues = new ConcurrentHashMap<>();
        Map<String, Double> GiniPValues = new ConcurrentHashMap<>();
        Map<String, Double> MPDValues = new ConcurrentHashMap<>();
        Map<String, Double> CRecallValues = new ConcurrentHashMap<>();
        Map<String, Double> ERRIAValues = new ConcurrentHashMap<>();

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

        // Prepare the training and test data
        GraphSimpleFastPreferenceData<Long> trainData;
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

        PreferenceData<Long, Long> totalData = new ConcatPreferenceData<>(trainData, testData);

        FeatureData<Long, String, Double> indexData = SimpleFeatureData.load(LuceneTfIdfFeaturesReader.load(indexFeatures, graph, Parsers.lp));
        FeatureData<Long, String, Double> commData = SimpleFeatureData.load(SimpleFeaturesReader.get().read(commFeatures, Parsers.lp, Parsers.sp));
        FastDistanceCalculator<Long> calculator = new FastDistanceCalculator<>();

        RecommendationFormat<Long, Long> format = new SimpleRecommendationFormat<>(lp, lp);
        int numUsers = testData.numUsersWithPreferences();

        int numFiles = 0;
        for(String rec : recList)
        {
            File auxFile = new File(rec);
            if(!auxFile.isDirectory()) numFiles++;
        }

        Function<Long, Predicate<Long>> filter = x -> y ->  !graph.containsEdge(x,y) && !x.equals(y) && !graph.containsEdge(y, x);

        int i = 0;
        for(String rec : recList)
        {

            Set<Long> users = new HashSet<>();
            graph.getAllNodes().forEach(users::add);
            File auxFile = new File(rec);
            if(auxFile.isDirectory()) continue;

            //FastFiller<Long,Long> filler = new RandomFiller<>(trainData, 0);

            System.out.println("Evaluating " + rec + " (" + i + "/" + numFiles + ")");

            RelevanceModel<Long, Long> relModel = new NoRelevanceModel<>();
            ERRIA.ERRRelevanceModel<Long, Long> erriamodel = new ERRIA.ERRRelevanceModel<>(false, testData, 0.5);
            IntentModel<Long, Long, String> intentModel = new FeatureIntentModel<>(totalData, commData);

            ItemDistanceModel<Long> itemDistanceModel = new CosineFeatureItemDistanceModel<>(indexData);
            SystemMetric<Long, Long> LTN = new LTN<>(maxLength, new PCItemNovelty<>(trainData));
            SystemMetric<Long, Long> Unexp = new Unexpectedness<>(maxLength, new PDItemNovelty<>(true, trainData, itemDistanceModel));
            SystemMetric<Long, Long> ILD = new ILD<>(maxLength, itemDistanceModel);
            SystemMetric<Long, Long> Gini = new GiniIndex<>(maxLength, trainData.numItems());
            SystemMetric<Long, Long> MPD = new MeanPredictionDistance<>(graph, calculator, maxLength);

            int totalNumUsers = Long.valueOf(graph.getVertexCount()).intValue();
            SystemMetric<Long, Long> CommRecall = new AverageRecommendationMetric<>(new SRecall<>(commData,10, relModel), totalNumUsers);
            SystemMetric<Long, Long> ERRIA = new AverageRecommendationMetric<>(new ERRIA<>(10,intentModel, erriamodel), numUsers);

            Map<String, SystemMetric<Long, Long>> metrics = new HashMap<>();
            metrics.put("LTN", LTN);
            metrics.put("Unexpectedness", Unexp);
            metrics.put("ILD", ILD);
            metrics.put("Gini", Gini);
            metrics.put("MPD", MPD);
            metrics.put("CommRecall", CommRecall);
            metrics.put("ERRIA", ERRIA);

            format.getReader(recPath+rec).readAll().forEach(recom ->
            {
                if (recom != null && recom.getItems() != null && !recom.getItems().isEmpty())
                {
                    //Recommendation<Long,Long> defRecom = filler.fill(recom, maxLength, filter);
                    metrics.values().forEach(metric -> metric.add(recom));
                    users.remove(recom.getUser());
                }
            });

            // Fill the remaining users:
            /*for(long user : users)
            {
                Recommendation<Long, Long> recom = new Recommendation<>(user, new ArrayList<>());
                Recommendation<Long,Long> defRecom = filler.fill(recom, maxLength, filter);
                if (defRecom != null && defRecom.getItems() != null && !defRecom.getItems().isEmpty())
                {
                    metrics.values().forEach(metric -> metric.add(recom));
                }
            }*/

            LTNValues.put(rec, metrics.get("LTN").evaluate());
            UnexpValues.put(rec, metrics.get("Unexpectedness").evaluate());
            ILDValues.put(rec, metrics.get("ILD").evaluate());
            GiniPValues.put(rec, metrics.get("Gini").evaluate());
            MPDValues.put(rec, metrics.get("MPD").evaluate());
            CRecallValues.put(rec, metrics.get("CommRecall").evaluate());
            ERRIAValues.put(rec, metrics.get("ERRIA").evaluate());
            ++i;
            System.out.println("Finished evaluating " + rec + " (" + i + "/" + numFiles + ")");

        }

        List<String> ids = new ArrayList<>();
        List<Map<String, Double>> values = new ArrayList<>();
        ids.add("LTN"); values.add(LTNValues);
        ids.add("Unexpectedness"); values.add(UnexpValues);
        ids.add("ILD"); values.add(ILDValues);
        ids.add("Gini"); values.add(GiniPValues);
        ids.add("MPD"); values.add(MPDValues);
        ids.add("CRecall"); values.add(CRecallValues);
        ids.add("ERRIA"); values.add(ERRIAValues);
        // Print the file.
        AuxiliarMethods.printFile(outputPath + "evaluation.txt", values, ids, maxLength);




    }
}
