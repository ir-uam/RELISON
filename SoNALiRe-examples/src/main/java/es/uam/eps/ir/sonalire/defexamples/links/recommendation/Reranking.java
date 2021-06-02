/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.defexamples.links.recommendation;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.feature.SimpleFeatureData;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilters;
import es.uam.eps.ir.sonalire.AuxiliarMethods;
import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.community.io.TextCommunitiesReader;
import es.uam.eps.ir.sonalire.graph.Adapters;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.grid.links.recommendation.metrics.RecommMetricGridReader;
import es.uam.eps.ir.sonalire.grid.links.recommendation.metrics.RecommMetricGridSelector;
import es.uam.eps.ir.sonalire.grid.links.recommendation.metrics.RecommendationMetricFunction;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.GlobalRerankerFunction;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.RerankerGridReader;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.RerankerGridSelector;
import es.uam.eps.ir.sonalire.io.graph.TextGraphReader;
import es.uam.eps.ir.sonalire.io.graph.TextMultiGraphReader;
import es.uam.eps.ir.sonalire.links.data.FastGraphIndex;
import es.uam.eps.ir.sonalire.links.data.GraphIndex;
import es.uam.eps.ir.sonalire.links.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.sonalire.links.recommendation.SocialFastFilters;
import es.uam.eps.ir.sonalire.links.recommendation.features.LuceneTfIdfFeaturesReader;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.global.GlobalReranker;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizers;
import org.ranksys.formats.feature.SimpleFeaturesReader;
import org.ranksys.formats.parsing.Parsers;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static es.uam.eps.ir.sonalire.examples.AuxiliarVariables.TRUE;
import static org.ranksys.formats.parsing.Parsers.lp;

/**
 * Program for applying a given reranking algorithm to the outcome of a contact recommendation algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Reranking
{
    /**
     * Generates rerankers.
     * @param args Execution arguments:
     *             <ol>
     *                  <li><b>Train:</b> Route to the file containing the training graph.</li>
     *                  <li><b>Test:</b> Route to the file containing the test links.</li>
     *                  <li><b>Multigraph:</b> true if the network allows multiple edges, false otherwise.</li>
     *                  <li><b>Directed:</b> true if the network is directed, false otherwise.</li>
     *                  <li><b>Weighted:</b> true if the network is weighted, false otherwise.</li>
     *                  <li><b>Selfloops:</b> true if the network allows self-loops, false otherwise.</li>
     *                  <li><b>ReadTypes:</b> true if we have to read the edge types from the file, false otherwise.</li>
     *                  <li><b>Grid:</b> File containing the configuration for rerankers and evaluation metrics.</li>
     *                  <li><b>Output directory:</b> Directory in which to store the recommendations and the output file.</li>
     *                  <li><b>Cutoff:</b> Number of recommended users to return</li>
     *                  <li><b>Max. length:</b> the number of recommended users to consider.</li>
     *                  <li>Optional arguments:
     *                      <ul>
     *                          <li><b>-reciprocal value: </b> true if we allowed the recommendation of reciprocal links, false otherwise (by default, false)</li>
     *                          <li><b>-distance max directed:</b> if we wanted to limit the maximum distance from the target user to the recommended ones.
     *                              max indicates the distance value whereas directed indicates if we measure the distance over a directed (true) or undirected graph (false).
     *                              By default, distance is not limited.</li>
     *                          <li><b>-feat-data file index:</b> if we want to compute feature-dependant metrics, this contains the feature information.
     *                              File indicates the route to the file, whereas index is true if we have to read the features from an inverted index.</li>
     *                      </ul>
     *                  </li>
     *             </ol>
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 10)
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
            System.err.println("\trecFile/Folder: Route to the recommendation/recommendations to rerank");
            System.err.println("\tCommunities file: Route to a file containing the communities");
            System.err.println("\tGrid: File containing the configuration for rerankers and evaluation metrics.");
            System.err.println("\tOutput folder: Route in which to store the different rerankers");
            System.err.println("\tCutoff: Number of recommended users to return");
            System.err.println("\tMax. length: the number of recommended users to consider.");
            System.err.println("\tNormalize: the normalization function: ranksim, zscore, minmax or none");
            System.err.println("\tOptional arguments:");
            System.err.println("\t\t-reciprocal value: true if we allowed the recommendation of reciprocal links, false otherwise (by default, false)");
            System.err.println("\t\t-distance max directed: if we limited the maximum distance from the target user to the recommended ones. max indicates the distance value whereas directed indicates if we measured the distance over a directed (true) or undirected graph (false)");
            System.err.println("\t\t-feat-data file index: if we want to compute feature-dependant metrics, this contains the feature information. File indicates the route to the file, whereas index is true if we have to read the features from an inverted index.");
            return;
        }

        // First, read the arguments:

        // Training and test set information:
        String trainPath = args[0];
        String testPath = args[1];

        boolean multigraph = args[2].equalsIgnoreCase(TRUE);
        boolean directed = args[3].equalsIgnoreCase(TRUE);
        boolean weighted = args[4].equalsIgnoreCase(TRUE);
        boolean selfloops = args[5].equalsIgnoreCase(TRUE);
        boolean readTypes = args[6].equalsIgnoreCase(TRUE);

        // Folder containing the recommendations:
        String recDataPath = args[7];
        // The community partition:
        String commFile = args[8];


        // Configuration path.
        String configPath = args[9];
        // The output directory for storing the rerankers.
        String output = args[10];

        // The cutoff (length of the definitive ranking) and the number of recommended users to consider:
        int cutoff = Parsers.ip.parse(args[11]);
        int maxLength = Parsers.ip.parse(args[12]);

        // The normalization technique:
        String normalizer = args[13];


        String featData = null;
        boolean isIndexFeatData = false;
        boolean recommendReciprocal = false;
        int maxDistance = -1;
        boolean directedDistance = false;

        // Optional arguments:
        for(int i = 14; i < args.length; ++i)
        {
            switch (args[i])
            {
                case "-feat-data" ->
                {
                    featData = args[++i];
                    isIndexFeatData = args[++i].equalsIgnoreCase(TRUE);
                }
                case "-reciprocal" -> // For evaluation, indicates if we allowed recommending reciprocal links
                    recommendReciprocal = args[++i].equalsIgnoreCase(TRUE);
                case "-distance" -> // For evaluation, indicates the maximum distance allowed for recommending links.
                {
                    maxDistance = Parsers.ip.parse(args[++i]);
                    directedDistance = args[++i].equalsIgnoreCase(TRUE);
                }
            }
        }

        long timea = System.currentTimeMillis();

        // Read the training graph.
        TextGraphReader<Long> greader = (multigraph) ? new TextMultiGraphReader<>(directed, false, selfloops, "\t", Parsers.lp) : new TextGraphReader<>(directed, weighted, selfloops, "\t", Parsers.lp);
        FastGraph<Long> graph = (FastGraph<Long>) greader.read(trainPath, weighted, readTypes);

        FastPreferenceData<Long, Long> trainData = GraphSimpleFastPreferenceData.load(graph);
        GraphIndex<Long> index = new FastGraphIndex<>(graph);

        // Read the test graph
        TextGraphReader<Long> testGraphReader = (multigraph) ? new TextMultiGraphReader<>(directed, false, selfloops, "\t", Parsers.lp) : new TextGraphReader<>(directed, false, false, "\t", Parsers.lp);
        Graph<Long> auxTestGraph = testGraphReader.read(testPath, false, false);
        if (auxTestGraph == null)
        {
            System.err.println("ERROR: Could not read the test graph");
            return;
        }

        // Read the community partition of the network.
        TextCommunitiesReader<Long> creader = new TextCommunitiesReader<>("\t",lp);
        Communities<Long> comms = creader.read(commFile);
        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" +(timeb-timea) + " ms.)");
        timea = System.currentTimeMillis();

        // Select the rerankers to apply
        RerankerGridReader gridreader = new RerankerGridReader();
        Map<String, Object> obj = AuxiliarMethods.readYAML(configPath);
        gridreader.read(obj);

        Map<String, GlobalRerankerFunction<Long>> rerankerMap = new HashMap<>();
        gridreader.getRerankers().forEach(reranker ->
        {
            RerankerGridSelector<Long> rgs = new RerankerGridSelector<>();
            rerankerMap.putAll(rgs.getRerankers(reranker, gridreader.getGrid(reranker)));
        });

        // Obtain the normalizer
        Supplier<Normalizer<Long>> norm = switch(normalizer.toLowerCase())
        {
            case "minmax" -> Normalizers.minmax();
            case "zscore" -> Normalizers.zscore();
            case "ranksim" -> Normalizers.ranksim();
            default -> Normalizers.noNorm();
        };

        // Obtain the recommendation files:
        List<String> recFiles = new ArrayList<>();
        File recs = new File(recDataPath);
        if(!recs.isDirectory()) // if it is a single file
        {
            recFiles.add(recDataPath);
        }
        else // if it is a list of recommenders
        {
            String[] list = recs.list();
            if(list != null)
            {
                for (String rec : list)
                {
                    recFiles.add(recDataPath + rec);
                }
            }
        }

        timeb = System.currentTimeMillis();
        System.out.println("Grid read (" +(timeb-timea) + " ms.)");

        // If there is data, create it:

        FeatureData<Long, String, Double> featureData;
        if(featData != null && isIndexFeatData)
        {
            featureData = SimpleFeatureData.load(LuceneTfIdfFeaturesReader.load(featData, graph, Parsers.lp));
        }
        else if(featData != null)
        {
            featureData = SimpleFeatureData.load(SimpleFeaturesReader.get().read(featData, Parsers.lp, Parsers.sp));
        }
        else
        {
            featureData = SimpleFeatureData.load(Stream.empty());
        }

        // Get the different metrics to compute.
        RecommMetricGridReader metricGridReader = new RecommMetricGridReader();
        metricGridReader.read(obj);

        Map<String, RecommendationMetricFunction<Long, String>> recMetricMap = new HashMap<>();
        metricGridReader.getMetrics().forEach(metric ->
        {
            RecommMetricGridSelector<Long, String> rmgs = new RecommMetricGridSelector<>();
            Map<String, RecommendationMetricFunction<Long, String>> suppliers = rmgs.getMetrics(metric, metricGridReader.getGrid(metric));
            if(suppliers == null)
            {
                System.err.println("ERROR: Metric " + metric + " could not be read.");
            }
            else
            {
                recMetricMap.putAll(suppliers);
            }
        });

        // For each algorithm, stores a set of metrics.
        Map<String, Map<String, Double>> metricValues = new ConcurrentHashMap<>();
        recMetricMap.keySet().forEach(key -> metricValues.put(key, new ConcurrentHashMap<>()));

        // Select the set of users to be recommended, the format, and the filters to apply to the recommendation
        Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(trainData), FastFilters.notSelf(index));
        if(!recommendReciprocal) // if we do not allow recommending reciprocal links:
            filter = FastFilters.and(filter, SocialFastFilters.notReciprocal(graph, index));
        if(maxDistance > 0) // if we do not allow recommending people at distance greater than a value.
            filter = FastFilters.and(filter, SocialFastFilters.limitedDistance(graph, directedDistance, maxDistance));

        // Clean the test graph.
        auxTestGraph = Adapters.onlyTrainUsers(auxTestGraph, graph);
        assert auxTestGraph != null;
        FastGraph<Long> testGraph = (FastGraph<Long>) Adapters.filteredGraph(auxTestGraph, filter);
        FastPreferenceData<Long, Long> testData;
        testData = GraphSimpleFastPreferenceData.load(testGraph);

        // Perform the reranking.
        RecommendationFormat<Long,Long> format = new SimpleRecommendationFormat<>(lp, lp);
        recFiles.forEach(rec ->
        {
            File f = new File(rec);
            if(!f.isDirectory())
            {
                String[] split = rec.split("/");
                //String[] split =  rec.split("\\Q\\\\E");
                String recName = split[split.length - 1];
                try
                {
                    System.out.println("Starting algorithm " + recName);

                    List<Recommendation<Long, Long>> recommendations = format.getReader(rec).readAll().collect(Collectors.toCollection(ArrayList::new));
                    rerankerMap.forEach((name, rerankerSupplier) ->
                    {
                        System.out.println("Running " + name);
                        String recOut = String.format("%s_%s", output + recName, name + ".txt");

                        // First, create the nDCG metric (for measuring accuracy)
                        Map<String, SystemMetric<Long, Long>> metrics = new HashMap<>();
                        recMetricMap.forEach((key, value) -> metrics.put(key, value.apply(graph, testGraph, trainData, testData, featureData, comms)));


                        GlobalReranker<Long, Long> reranker = rerankerSupplier.apply(cutoff, norm, graph, comms);
                        try
                        {
                            long startTime = System.nanoTime();
                            Stream<Recommendation<Long,Long>> reranked = reranker.rerankRecommendations(recommendations.stream(), maxLength);
                            Map<String, Double> metr = AuxiliarMethods.writeAndEvaluate(recOut, reranked, metrics);

                            metr.forEach((metric, value) -> metricValues.get(metric).put(recName, value));
                            long difference = System.nanoTime() - startTime;
                            System.out.println(name + ": " + TimeUnit.NANOSECONDS.toSeconds(difference) + "," + (TimeUnit.NANOSECONDS.toMillis(difference) - TimeUnit.NANOSECONDS.toSeconds(difference) * 1000) + " s.");
                        }
                        catch (IOException ioe)
                        {
                            ioe.printStackTrace();
                        }
                    });

                    System.out.println("Ending algorithm " + rec);

                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }
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
}
