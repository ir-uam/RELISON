/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.examples.links.prediction;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.feature.SimpleFeatureData;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.community.io.CommunitiesReader;
import es.uam.eps.ir.sonalire.community.io.TextCommunitiesReader;
import es.uam.eps.ir.sonalire.examples.AuxiliarMethods;
import es.uam.eps.ir.sonalire.graph.Adapters;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.grid.links.prediction.metrics.LinkPredictionMetricFunction;
import es.uam.eps.ir.sonalire.grid.links.prediction.metrics.LinkPredictionMetricGridSelector;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.AlgorithmGridReader;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.AlgorithmGridSelector;
import es.uam.eps.ir.sonalire.grid.links.recommendation.metrics.RecommMetricGridReader;
import es.uam.eps.ir.sonalire.io.graph.TextGraphReader;
import es.uam.eps.ir.sonalire.io.graph.TextMultiGraphReader;
import es.uam.eps.ir.sonalire.links.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.sonalire.links.linkprediction.LinkPredictor;
import es.uam.eps.ir.sonalire.links.linkprediction.Prediction;
import es.uam.eps.ir.sonalire.links.linkprediction.RecommendationLinkPredictor;
import es.uam.eps.ir.sonalire.links.linkprediction.filter.LinkPredFastFilters;
import es.uam.eps.ir.sonalire.links.linkprediction.io.LinkPredictionFormat;
import es.uam.eps.ir.sonalire.links.linkprediction.io.SimpleLinkPredictionFormat;
import es.uam.eps.ir.sonalire.links.linkprediction.metrics.LinkPredictionMetric;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.sonalire.links.recommendation.features.LuceneTfIdfFeaturesReader;
import es.uam.eps.ir.sonalire.utils.datatypes.Pair;
import org.ranksys.formats.feature.SimpleFeaturesReader;
import org.ranksys.formats.parsing.Parsers;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static es.uam.eps.ir.sonalire.examples.AuxiliarVariables.TRUE;

/**
 * Class for executing and evaluating link prediction approaches.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class LinkPrediction
{
    /**
     * Program for recommending and evaluating contact recommendation approaches.
     *
     * @param args Execution arguments:
     *             <ol>
     *                  <li><b>Train:</b> Route to the file containing the training graph.</li>
     *                  <li><b>Test:</b> Route to the file containing the test links.</li>
     *                  <li><b>Multigraph:</b> true if the network allows multiple edges, false otherwise.</li>
     *                  <li><b>Directed:</b> true if the network is directed, false otherwise.</li>
     *                  <li><b>Weighted:</b> true if the network is weighted, false otherwise.</li>
     *                  <li><b>Selfloops:</b> true if the network allows self-loops, false otherwise.</li>
     *                  <li><b>ReadTypes:</b> true if we have to read the edge types from the file, false otherwise.</li>
     *                  <li><b>Algorithms:</b> Route to a YAML file containing the recommender configurations.</li>
     *                  <li><b>Output directory:</b> Directory in which to store the recommendations and the output file.</li>
     *                  <li>Optional arguments:
     *                      <ul>
     *                          <li><b>-users test/all:</b> selects the subset of users for whom we generate recommendation (all of them, or just those in test.</li>
     *                          <li><b>-print value:</b> true if, additionally to the results, you want to print the recommendations, false otherwise (by default, true)</li>
     *                          <li><b>-reciprocal value: </b> true if we want to recommend reciprocal links, false otherwise (by default, false)</li>
     *                          <li><b>-distance max directed:</b> if we want to limit the maximum distance from the target user to the recommended ones.
     *                              max indicates the distance value whereas directed indicates if we measure the distance over a directed (true) or undirected graph (false).
     *                              By default, distance is not limited.</li>
     *                          <li><b>-feat-data file index:</b> if we want to compute feature-dependant metrics, this contains the feature information.
     *                              File indicates the route to the file, whereas index is true if we have to read the features from an inverted index.</li>
     *                          <li><b>-comms commFile:</b> a route to a file specifying the communities of the users (by default, all users belong to the same community)</li>
     *                      </ul>
     *                  </li>
     *             </ol>
     */
    public static void main(String[] args) throws IOException
    {
        if (args.length < 10)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tTrain: Route to the file containing the training graph.");
            System.err.println("\tTest: Route to the file containing the test links.");
            System.err.println("\tMultigraph: true if the network allows multiple edges, false otherwise.");
            System.err.println("\tDirected: true if the network is directed, false otherwise.");
            System.err.println("\tWeighted: true if the network is weighted, false otherwise.");
            System.err.println("\tSelfloops: true if the network allows self-loops, false otherwise.");
            System.err.println("\tReadTypes: true if we have to read the edge types from the file, false otherwise.");
            System.err.println("\tAlgorithms: Route to a YAML file containing the link prediction configurations.");
            System.err.println("\tOutput directory: Directory in which to store the link predictions and the output file.");
            System.err.println();
            System.err.println("\tOptional arguments:");
            System.err.println("\t\t-users test/all: selects the subset of users for whom we generate link prediction (all of them, or just those in test.");
            System.err.println("\t\t-print value: true if, additionally to the results, you want to print the link predictions, false otherwise (by default, true)");
            System.err.println("\t\t-reciprocal value: true if we want to predict reciprocal links, false otherwise (by default, false)");
            System.err.println("\t\t-distance max directed: if we want to limit the maximum distance between the endpoints of the predicted links. max indicates the distance value whereas directed indicates if we measure the distance over a directed (true) or undirected graph (false)");
            System.err.println("\t\t-feat-data file index: if we want to compute feature-dependant metrics, this contains the feature information. File indicates the route to the file, whereas index is true if we have to read the features from an inverted index.");
            System.err.println("\t\t-comms commFile: a route to a file specifying the communities of the users (by default, all users belong to the same community)");
            return;
        }

        // Read the execution arguments:
        String trainPath = args[0];
        String testPath = args[1];

        boolean multigraph = args[2].equalsIgnoreCase(TRUE);
        boolean directed = args[3].equalsIgnoreCase(TRUE);
        boolean weighted = args[4].equalsIgnoreCase(TRUE);
        boolean selfloops = args[5].equalsIgnoreCase(TRUE);
        boolean readTypes = args[6].equalsIgnoreCase(TRUE);

        String algorithmConf = args[7];
        String output = args[8];

        boolean allUsers = true;
        boolean printRecommenders = true;

        boolean recommendReciprocal = false;
        int maxDistance = -1;
        boolean directedDistance = false;

        String featData = null;
        boolean isIndexFeatData = false;
        String commFile = null;

        // Optional arguments:
        for(int i = 9; i < args.length; ++i)
        {
            switch (args[i])
            {
                case "-users":
                    switch (args[++i])
                    {
                        case "test" -> allUsers = false;
                        case "all" -> allUsers = true;
                        default -> {
                            System.err.println("ERROR: invalid set of selected users. Choose all / test");
                            return;
                        }
                    }
                    break;
                case "-print":
                    printRecommenders = args[++i].equalsIgnoreCase(TRUE);
                    break;
                case "-reciprocal":
                    recommendReciprocal = args[++i].equalsIgnoreCase(TRUE);
                    break;
                case "-distance":
                    maxDistance = Parsers.ip.parse(args[++i]);
                    directedDistance = args[++i].equalsIgnoreCase(TRUE);
                    break;
                case "-feat-data":
                    featData = args[++i];
                    isIndexFeatData = args[++i].equalsIgnoreCase(TRUE);
                    break;
                case "-comms":
                    commFile = args[++i];
                    break;
            }
        }

        // First, we do create the directories.
        if (printRecommenders)
        {
            File directory = new File(output + "recs" + File.separator);
            if(!directory.exists())
            {
                boolean create = directory.mkdirs();
                if(!create)
                {
                    System.err.println("ERROR: Program could not create the recommendation folder");
                    return;
                }
            }

        }

        // Read the test graph
        TextGraphReader<Long> testGraphReader = (multigraph) ? new TextMultiGraphReader<>(directed, false, selfloops, "\t", Parsers.lp) : new TextGraphReader<>(directed, false, false, "\t", Parsers.lp);
        Graph<Long> auxTestGraph = testGraphReader.read(testPath, false, false);

        if (auxTestGraph == null)
        {
            System.err.println("ERROR: Could not read the test graph");
            return;
        }

        System.out.println("-------- Started " + (weighted ? "weighted" : "unweighted") + " variants --------");
        long timea = System.currentTimeMillis();

        // Read the training graph.
        TextGraphReader<Long> weightedReader = multigraph ? new TextGraphReader<>(directed, weighted, selfloops, "\t", Parsers.lp) : new TextGraphReader<>(directed, weighted, selfloops, "\t", Parsers.lp);
        FastGraph<Long> weightedGraph = (FastGraph<Long>) weightedReader.read(trainPath, weighted, readTypes);
        if (weightedGraph == null)
        {
            System.err.println("ERROR: Could not read the training graph");
            return;
        }

        // Generate the unweighted network.
        FastGraph<Long> unweightedGraph;
        if(!weighted)
        {
            unweightedGraph = weightedGraph;
        }
        else
        {
            unweightedGraph = (FastGraph<Long>) Adapters.unweighted(weightedGraph);
        }


        // Read the test graph.
        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" + (timeb - timea) + " ms.)");
        timea = System.currentTimeMillis();

        // Prepare the training and test data
        FastPreferenceData<Long, Long> weightedTrainData = GraphSimpleFastPreferenceData.load(weightedGraph);
        FastPreferenceData<Long, Long> unweightedTrainData = weighted ? GraphSimpleFastPreferenceData.load(unweightedGraph) : weightedTrainData;

        // If there is data, create it:

        FeatureData<Long, String, Double> featureData;
        if(featData != null && isIndexFeatData)
        {
            assert unweightedGraph != null;
            featureData = SimpleFeatureData.load(LuceneTfIdfFeaturesReader.load(featData, unweightedGraph, Parsers.lp));
        }
        else if(featData != null)
        {
            featureData = SimpleFeatureData.load(SimpleFeaturesReader.get().read(featData, Parsers.lp, Parsers.sp));
        }
        else
        {
            featureData = SimpleFeatureData.load(Stream.empty());
        }

        Communities<Long> comms;
        if(commFile != null)
        {
            CommunitiesReader<Long> cReader = new TextCommunitiesReader<>("\t", Parsers.lp);
            comms = cReader.read(commFile);
        }
        else // all nodes in the same community
        {
            comms = new Communities<>();
            comms.addCommunity();
            assert unweightedGraph != null;
            unweightedGraph.getAllNodes().forEach(node -> comms.add(node, 0));
        }

        // Read the configuration file:

        // Read the YAML containing the parameter grid for each algorithm
        AlgorithmGridReader gridreader = new AlgorithmGridReader();
        Map<String, Object> yaml = AuxiliarMethods.readYAML(algorithmConf);
        gridreader.read(yaml);

        // Read the YAML containing the metric grid.
        RecommMetricGridReader metricGridReader = new RecommMetricGridReader();
        metricGridReader.read(yaml);

        Map<String, RecommendationAlgorithmFunction<Long>> recMap = new HashMap<>();
        // Get the different recommenders to execute
        gridreader.getAlgorithms().forEach(algorithm ->
        {
            AlgorithmGridSelector<Long> ags = new AlgorithmGridSelector<>(Parsers.lp);
            Map<String, RecommendationAlgorithmFunction<Long>> suppliers = ags.getRecommenders(algorithm, gridreader.getGrid(algorithm));
            if (suppliers == null)
            {
                System.err.println("ERROR: Algorithm " + algorithm + " could not be read");
            }
            else
            {
                recMap.putAll(ags.getRecommenders(algorithm, gridreader.getGrid(algorithm)));
            }
        });

        // Get the different metrics to compute.
        Map<String, LinkPredictionMetricFunction<Long, String>> recMetricMap = new HashMap<>();
        metricGridReader.getMetrics().forEach(metric ->
        {
            LinkPredictionMetricGridSelector<Long, String> rmgs = new LinkPredictionMetricGridSelector<>();
            Map<String, LinkPredictionMetricFunction<Long, String>> suppliers = rmgs.getMetrics(metric, metricGridReader.getGrid(metric));
            if(suppliers == null)
            {
                System.err.println("ERROR: Metric " + metric + " could not be read.");
            }
            else
            {
                recMetricMap.putAll(suppliers);
            }
        });

        // Clean the test graph.
        FastGraph<Long> testGraph = (FastGraph<Long>) Adapters.onlyTrainUsers(auxTestGraph, weightedGraph);
        FastPreferenceData<Long, Long> testData;
        testData = GraphSimpleFastPreferenceData.load(testGraph);
        Set<Long> targetUsers = allUsers ? weightedTrainData.getAllUsers().collect(Collectors.toCollection(HashSet::new)) : testData.getUsersWithPreferences().collect(Collectors.toCollection(HashSet::new));

        // For each algorithm, stores a set of metrics.
        Map<String, Map<String, Double>> metricValues = new ConcurrentHashMap<>();
        recMetricMap.keySet().forEach(key -> metricValues.put(key, new ConcurrentHashMap<>()));

        timeb = System.currentTimeMillis();
        System.out.println("Algorithms selected (" + (timeb - timea) + " ms.)");

        // Select the set of viable links:
        Predicate<Pair<Long>> filter = LinkPredFastFilters.and(LinkPredFastFilters.onlyNewLinks(unweightedGraph), LinkPredFastFilters.notSelf());
        if(!recommendReciprocal)
            filter = LinkPredFastFilters.and(filter, LinkPredFastFilters.notReciprocal(unweightedGraph));
        if(maxDistance > 0)
            filter = LinkPredFastFilters.and(filter, LinkPredFastFilters.limitDistance(unweightedGraph, directedDistance, maxDistance));
        if(!allUsers)
            filter = LinkPredFastFilters.and(filter, LinkPredFastFilters.onlyUsers(targetUsers));

        System.out.println("Num. target users: " + targetUsers.size());

        AtomicInteger counter = new AtomicInteger(0);
        int totalCount = recMap.size();
        // Execute the recommendations
        boolean finalPrintRecommenders = printRecommenders;
        Predicate<Pair<Long>> finalFilter = filter;
        recMap.entrySet().parallelStream().forEach(entry ->
        {
            long a = System.currentTimeMillis();
            String name = entry.getKey();

            // First, create the nDCG metric (for measuring accuracy)
            Map<String, LinkPredictionMetric<Long>> metrics = new HashMap<>();
            recMetricMap.forEach((key, value) -> metrics.put(key, value.apply(unweightedGraph, testGraph, unweightedTrainData, testData, featureData, comms)));

            // Prepare the recommender
            RecommendationAlgorithmFunction<Long> supplier = entry.getValue();
            boolean isWeighted = supplier.isWeighted();
            Recommender<Long, Long> rec = isWeighted ? supplier.apply(weightedGraph, weightedTrainData) : supplier.apply(unweightedGraph, unweightedTrainData);
            RecommendationLinkPredictor<Long> lp = new RecommendationLinkPredictor<>(isWeighted ? weightedGraph : unweightedGraph, rec);

            String path = output + File.separator + name + ".txt";

            // Obtain the metric values
            Map<String, Double> values;
            try
            {
                if (finalPrintRecommenders)
                {
                    values = computeAndEvaluate(path, lp, finalFilter, testGraph, metrics);
                    values.forEach((metric, value) -> metricValues.get(metric).put(name, value));
                }
                else
                {
                    values = computeAndEvaluate(lp, finalFilter, testGraph, metrics);
                    values.forEach((metric, value) -> metricValues.get(metric).put(name, value));
                }
            }
            catch (IOException ioe)
            {
                System.err.println("Algorithm " + name + " failed");
            }

            long b = System.currentTimeMillis();
            System.err.println("Algorithm " + counter.incrementAndGet() + "/" + totalCount + ": " + name + " finished (" + (b-a) + " ms.)");
        });

        List<String> ids = new ArrayList<>();
        List<Map<String, Double>> values = new ArrayList<>();

        metricValues.forEach((metric, map) ->
        {
            ids.add(metric);
            values.add(map);
        });

        // Print the file.
        AuxiliarMethods.printFile(output + "evaluation.txt", values, ids);
    }

    /**
     * Computes a link prediction and evaluates it using metrics.
     *
     * @param output        route of the file in which to store the link prediction.
     * @param predictor     the link prediction algorithm to apply.
     * @param filter        the filter for selecting which links can be predicted.
     * @param metrics       the metrics to evaluate
     *
     * @return the value of the metrics.
     *
     * @throws IOException if something fails during the writing / reading of the recommendation file.
     */
    public static Map<String, Double> computeAndEvaluate(String output, LinkPredictor<Long> predictor, Predicate<Pair<Long>> filter, Graph<Long> testGraph, Map<String, LinkPredictionMetric<Long>> metrics) throws IOException
    {
        LinkPredictionFormat<Long> format = new SimpleLinkPredictionFormat<>(Parsers.lp);
        LinkPredictionFormat.Writer<Long> writer;

        Map<String, Double> values = new HashMap<>();

        writer = format.getWriter(output);
        Prediction<Long> pred = predictor.getPrediction(filter);
        writer.write(pred);
        writer.close();

        metrics.forEach((key, metric) -> values.put(key, metric.evaluate(testGraph, pred, filter)));
        return values;
    }

    /**
     * Computes a link prediction and evaluates it using metrics.
     *
     * @param predictor     the link prediction algorithm to apply.
     * @param filter        the filter for selecting which links can be predicted.
     * @param metrics       the metrics to evaluate
     *
     * @return the value of the metrics.
     *
     */
    public static Map<String, Double> computeAndEvaluate(LinkPredictor<Long> predictor, Predicate<Pair<Long>> filter, Graph<Long> testGraph, Map<String, LinkPredictionMetric<Long>> metrics)
    {
        Map<String, Double> values = new HashMap<>();
        Prediction<Long> pred = predictor.getPrediction(filter);
        metrics.forEach((key, metric) -> values.put(key, metric.evaluate(testGraph, pred, filter)));
        return values;
    }


}
