/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.defexamples.links.recommendation;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.feature.SimpleFeatureData;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.runner.RecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilterRecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilters;
import es.uam.eps.ir.socialranksys.AuxiliarMethods;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.io.CommunitiesReader;
import es.uam.eps.ir.socialranksys.community.io.TextCommunitiesReader;
import es.uam.eps.ir.socialranksys.graph.Adapters;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics.RecommMetricGridSelector;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics.RecommendationMetricFunction;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics.YAMLRecommMetricGridReader;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.io.graph.TextMultiGraphReader;
import es.uam.eps.ir.socialranksys.links.data.FastGraphIndex;
import es.uam.eps.ir.socialranksys.links.data.GraphIndex;
import es.uam.eps.ir.socialranksys.links.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.socialranksys.links.data.letor.FeatureInformation;
import es.uam.eps.ir.socialranksys.links.data.letor.Instance;
import es.uam.eps.ir.socialranksys.links.data.letor.InstanceSet;
import es.uam.eps.ir.socialranksys.links.data.letor.io.InstanceSetReader;
import es.uam.eps.ir.socialranksys.links.data.letor.io.LETORInstanceReader;
import es.uam.eps.ir.socialranksys.links.recommendation.SocialFastFilters;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.supervised.LambdaMARTJForestsRecommender;
import es.uam.eps.ir.socialranksys.links.recommendation.features.LuceneTfIdfFeaturesReader;
import es.uam.eps.ir.socialranksys.utils.generator.Generators;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.ranksys.formats.feature.SimpleFeaturesReader;
import org.ranksys.formats.parsing.Parsers;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static es.uam.eps.ir.socialranksys.AuxiliarMethods.computeAndEvaluate;
import static es.uam.eps.ir.socialranksys.examples.AuxiliarVariables.TRUE;
import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers.LAMBDAMART;
import static es.uam.eps.ir.socialranksys.links.data.letor.io.LETORFormatConstants.*;

/**
 * A learning to rank experiment that performs ablation on the different variables.
 * It applies it to the LambdaMART algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 */
public class AblationExperimentExample
{
    /**
     * Obtains the error message for the LETORExperiment main function.
     * @return the error message.
     */
    private static String errorMessage()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("ERROR: Invalid arguments");
        builder.append("\n\t");
        builder.append("Training instances: A LETOR file containing the training instances.");
        builder.append("\n\t");
        builder.append("Validation instances: A LETOR file containing the validation instances.");
        builder.append("\n\t");
        builder.append("Test instances: A LETOR file containing the test instances.");
        builder.append("\n\t");
        builder.append("Partitions: A configuration file. On each row, it includes a comma separated list of values. Each value");
        builder.append(" corresponds to the index of a feature in the LETOR files. </li>");
        builder.append("\n\t");
        builder.append("Experiment directory: The route to the directory in which to the output the experiment will be stored.");
        builder.append("\n\t");
        builder.append("Terrier/JForests: The route to the Terrier/JForests binary file.");
        builder.append("\n\t");
        builder.append("JForest properties The route to the properties file for LambdaMART in the JForests/Terrier library.");
        builder.append("\n\t");
        builder.append("Training graph: The route for the training network.");
        builder.append("\n\t");
        builder.append("Test graph: The route to the test graph.");
        builder.append("\n\t");
        builder.append("Directed: true if the network is directed, false otherwise.");
        builder.append("\n\t");
        builder.append("Weighted: true if the network is weighted, false otherwise.");
        builder.append("\n\t");
        builder.append("Rec. ouput: directory in which to store the outcome of the recommendations.");
        builder.append("\n\t");
        builder.append("Cut-off: cut-off of the recommendations.");
        builder.append("\n\t");
        builder.append("Sample: true if we need to generate the instances for each feature selection, false otherwise.");
        builder.append("\n\t");
        builder.append("Copy: true if we need to generate the reduced instances for each feature selection, false otherwise.");
        return builder.toString();
    }

    /**
     * Executes the learning to rank experiment.
     * @param args execution arguments
     *             <ol>
     *              <li><b>Training instances:</b> A LETOR file containing the training instances.</li>
     *              <li><b>Validation instances:</b> A LETOR file containing the validation instances.</li>
     *              <li><b>Test instances:</b> A LETOR file containing the test instances.</li>
     *              <li><b>Partitions: </b> A configuration file. On each row, it includes a comma separated list of values. Each value
     *                                      corresponds to the index of a feature in the LETOR files. </li>
     *              <li><b>Experiment directory:</b> The route to the directory in which to the output the experiment will be stored.</li>
     *              <li><b>Terrier/JForests:</b> The route to the Terrier/JForests binary file.</li>
     *              <li><b>JForest properties:</b> The route to the properties file for LambdaMART in the JForests/Terrier library</li>
     *              <li><b>Training graph:</b> The route for the training network.</li>
     *              <li><b>Test graph:</b> The route to the test graph.</li>
     *              <li><b>Directed:</b> true if the network is directed, false otherwise.</li>
     *              <li><b>Weighted:</b> true if the network is weighted, false otherwise.</li>
     *              <li><b>Rec. ouput:</b> directory in which to store the outcome of the recommendations.</li>
     *              <li><b>Cut-off:</b> cut-off of the recommendations.</li>
     *              <li><b>Sample: </b> true if we need to generate the instances for each feature selection, false otherwise.</li>
     *              <li><b>Copy: </b> true if we need to generate the reduced instances for each feature selection, false otherwise.</li>
     *             </ol>
     * @throws IOException if something fails while reading/ writing.
     */
    public static void main(String[] args) throws IOException, InterruptedException
    {
        if(args.length < 15)
        {
            System.err.println(errorMessage());
            return;
        }

        // Read the parameters

        // First, we read the training and test networks:
        String trainGraphFile = args[0];
        String testGraphFile = args[1];
        boolean multigraph = args[2].equalsIgnoreCase(TRUE);
        boolean directed = args[3].equalsIgnoreCase(TRUE);
        boolean selfloops = args[4].equalsIgnoreCase(TRUE);

        // First, read the instance sets.
        String trainFile = args[5];
        String validFile = args[6];
        String testFile = args[7];

        // A file containing lists of comma separated features.
        String partitionsFile = args[8];

        // A file containing the jforest properties file (parameters for the jforest file)
        String jforestprop = args[9];

        // Output folder, and maximum recommendation length.
        String output = args[10];
        int maxLength = Parsers.ip.parse(args[11]);

        // The file containing the metric configurations.
        String metricFile = args[12];

        boolean recommendReciprocal = false;
        int maxDistance = -1;
        boolean directedDistance = false;

        String featData = null;
        boolean isIndexFeatData = false;
        String commFile = null;

        boolean allUsers = true;

        // Optional arguments:
        for(int i = 13; i < args.length; ++i)
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

        // Before starting, we obtain the different partitions we have to make for the features.
        // The map has each feature as index, and the set of partitions it appears in as the value.
        Map<Integer, Set<Integer>> partitions = new HashMap<>();
        int numPartitions = AblationExperimentExample.readPartitionsFile(partitionsFile, partitions);

        /* STEP 1: Generate the training/validation/test sets for each different partition.*/
        long bb;
        long aa = System.currentTimeMillis();
        int numFeats;

        // First, create the directories to store all the datasets.
        for (int i = 0; i < partitions.size(); ++i)
        {
            String directoryName = output + i + File.separator;
            File dir = new File(directoryName);
            boolean createddir = dir.mkdirs();
        }

        // Read and generate the training features.
        AblationExperimentExample.readAndSample(trainFile, output, partitions, numPartitions, "train.letor");
        bb = System.currentTimeMillis();
        System.out.println("Train features computed (" + (bb - aa) / 1000.0 + " s.)");

        // Read and generate the validation features.
        AblationExperimentExample.readAndSample(validFile, output, partitions, numPartitions, "valid.letor");
        bb = System.currentTimeMillis();
        System.out.println("Validation features computed (" + (bb - aa) / 1000.0 + " s.)");

        // Read and generate the test features.
        AblationExperimentExample.readAndSample(testFile, output, partitions, numPartitions, "test.letor");
        bb = System.currentTimeMillis();
        System.out.println("Test features computed (" + (bb - aa) / 1000.0 + " s.)");

        // Read the training graph.
        TextGraphReader<Long> weightedReader = multigraph ? new TextGraphReader<>(directed, false, selfloops, "\t", Parsers.lp) : new TextGraphReader<>(directed, false, selfloops, "\t", Parsers.lp);
        FastGraph<Long> trainGraph = (FastGraph<Long>) weightedReader.read(trainGraphFile, false, false);
        if (trainGraph == null)
        {
            System.err.println("ERROR: Could not read the training graph");
            return;
        }
        GraphIndex<Long> index = new FastGraphIndex<>(trainGraph);

        // Now, we read the training and test data:
        // Read the test graph
        TextGraphReader<Long> testGraphReader = (multigraph) ? new TextMultiGraphReader<>(directed, false, selfloops, "\t", Parsers.lp) : new TextGraphReader<>(directed, false, false, "\t", Parsers.lp);
        Graph<Long> auxTestGraph = testGraphReader.read(testGraphFile, false, false);

        if (auxTestGraph == null)
        {
            System.err.println("ERROR: Could not read the test graph");
            return;
        }

        FastPreferenceData<Long, Long> trainPrefData = GraphSimpleFastPreferenceData.load(trainGraph);

        FeatureData<Long, String, Double> featureData;
        if(featData != null && isIndexFeatData)
        {
            featureData = SimpleFeatureData.load(LuceneTfIdfFeaturesReader.load(featData, trainGraph, Parsers.lp));
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
            trainGraph.getAllNodes().forEach(node -> comms.add(node, 0));
        }

        // Read the metrics to compute:
        YAMLRecommMetricGridReader gridreader = new YAMLRecommMetricGridReader();
        Map<String, Object> yaml = AuxiliarMethods.readYAML(metricFile);
        gridreader.read(yaml);

        Map<String, RecommendationMetricFunction<Long, String>> recMetricMap = new HashMap<>();
        gridreader.getMetrics().forEach(metric ->
        {
            RecommMetricGridSelector<Long, String> rmgs = new RecommMetricGridSelector<>();
            Map<String, RecommendationMetricFunction<Long, String>> suppliers = rmgs.getMetrics(metric, gridreader.getGrid(metric));
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
        Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(trainPrefData), FastFilters.notSelf(index));
        if(!recommendReciprocal) // if we do not allow recommending reciprocal links:
            filter = FastFilters.and(filter, SocialFastFilters.notReciprocal(trainGraph, index));
        if(maxDistance > 0) // if we do not allow recommending people at distance greater than a value.
            filter = FastFilters.and(filter, SocialFastFilters.limitedDistance(trainGraph, directedDistance, maxDistance));

        // Clean the test graph.
        auxTestGraph = Adapters.onlyTrainUsers(auxTestGraph, trainGraph);
        assert auxTestGraph != null;
        FastGraph<Long> testGraph = (FastGraph<Long>) Adapters.filteredGraph(auxTestGraph, filter);
        FastPreferenceData<Long, Long> testData;
        testData = GraphSimpleFastPreferenceData.load(testGraph);

        Set<Long> targetUsers = allUsers ? trainPrefData.getAllUsers().collect(Collectors.toCollection(HashSet::new)) : testData.getUsersWithPreferences().collect(Collectors.toCollection(HashSet::new));
        System.out.println("Num. target users: " + targetUsers.size());
        RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(index, index, targetUsers.stream(), filter, maxLength);

        /* STEP 2: For each partition, we execute LambdaMART */
        IntStream.range(0, numPartitions).parallel().forEach(partition ->
        {
            try
            {
                long a = System.currentTimeMillis();
                String name = LAMBDAMART + "_" + partition;

                // First, create the nDCG metric (for measuring accuracy)
                Map<String, SystemMetric<Long, Long>> metrics = new HashMap<>();
                recMetricMap.forEach((key, value) -> metrics.put(key, value.apply(trainGraph, testGraph, trainPrefData, testData, featureData, comms)));

                String path = output + File.separator + name + ".txt";

                String dir = output + partition + File.separator;
                Recommender<Long, Long> rec = new LambdaMARTJForestsRecommender<>(trainGraph, dir + "train", dir + "valid", dir + "test", jforestprop, dir + "tmp", Parsers.lp);

                // Obtain the metric values
                Map<String, Double> values;
                try
                {
                    values = computeAndEvaluate(path, rec, runner, metrics);
                    values.forEach((metric, value) -> metricValues.get(metric).put(name, value));
                }
                catch (IOException ioe)
                {
                    System.err.println("Algorithm " + name + " failed");
                }

                long b = System.currentTimeMillis();
                System.err.println("Algorithm " + partition + "/" + numPartitions + ": " + name + " finished (" + (b - a) + " ms.)");
            }
            catch(Exception ex)
            {
                System.err.println("ERROR: Something failed while processing the " + partition + " partition.");
            }
        });

        // Print the experimental results.
        List<String> ids = new ArrayList<>();
        List<Map<String, Double>> values = new ArrayList<>();

        metricValues.forEach((metric, map) ->
        {
            ids.add(metric);
            values.add(map);
        });

        // Print the file.
        AuxiliarMethods.printFile(output + "evaluation.txt", values, ids);

        // Clean all the auxiliary folders:

    }

    /**
     * Reads a file specifying the partitions.
     * @param partitionsFile the partitions file.
     * @param map the map to configure. It has the feature index as key, and the numbers of configurations containing the feature as values.
     * @return the number of partitions.
     * @throws IOException if something fails while reading the file.
     */
    private static int readPartitionsFile(String partitionsFile, Map<Integer, Set<Integer>> map) throws IOException
    {
        int i = 0;
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(partitionsFile))))
        {
            String line;

            while((line = br.readLine()) != null)
            {
                String[] split  = line.split(",");
                for(String num : split)
                {
                    int j = Parsers.ip.parse(num);
                    if(!map.containsKey(j)) map.put(j, new IntOpenHashSet());
                    map.get(j).add(i);
                }
                ++i;
            }
        }
        return i;
    }


    /**
     * Given a LETOR file, and some feature selections, it generates a LETOR file for each selection.
     *
     * @param file          the route to the LETOR file.
     * @param expDirectory  a directory in which we want to store the files.
     * @param partitions    the different feature selections.
     * @param numPartitions the number of different feature selections.
     * @param filename      the name of the new files.
     * @return the feature information for the whole LETOR file.
     * @throws IOException  if something fails while reading / writing.
     */
    private static void readAndSample(String file, String expDirectory, Map<Integer, Set<Integer>> partitions, int numPartitions, String filename) throws IOException
    {
        long a = System.currentTimeMillis();
        long b;

        // We initialize:
        // a) a list of Writers, so we can write the new LETOR files.
        // b) a list of StringBuilders (one for each feature selection)
        // c) a list with counters.
        List<Writer> writers = new ArrayList<>();
        List<StringBuilder> strBuilders = new ArrayList<>();
        List<Map<Integer, Integer>> counters = new ArrayList<>();

        // We read the feature file.
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file))))
        {
            // Prepare the reader.
            InstanceSetReader<Long> reader = new LETORInstanceReader<>(Parsers.lp);
            for(int i = 0; i < numPartitions; ++i)
            {
                writers.add(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(expDirectory + i + File.separator + filename))));
                strBuilders.add(new StringBuilder());
                counters.add(new HashMap<>());
            }

            String line;
            FeatureInformation featInfo;
            List<String> header = new ArrayList<>();
            int i = 0;

            // Read the header of the LETOR file.
            while ((line = br.readLine()).startsWith(COMMENT))
            {
                String defLine = line;
                header.add(line);
                int idx = i;

                partitions.getOrDefault(i, new HashSet<>()).forEach(j ->
                {
                    counters.get(j).put(idx, counters.get(j).size()+1);
                    String aux = defLine + "";
                    aux = aux.replace("#"+(idx+1), "#"+counters.get(j).get(idx));
                    strBuilders.get(j).append(aux);
                    strBuilders.get(j).append("\n");
                });
                ++i;
            }

            featInfo = reader.readHeader(header);

            // Write the header of each feature selection.
            for(i = 0; i < numPartitions; ++i)
            {
                writers.get(i).write(strBuilders.get(i).toString());
            }

            b = System.currentTimeMillis();
            System.out.println("Read header (" + (b-a)/1000.0 + " s.)");

            AtomicInteger atom = new AtomicInteger(0);
            int numFeats = featInfo.numFeats();
            do // for each instance
            {
                // Read the instance
                Instance<Long> pattern = reader.readInstance(line, numFeats);
                // a) We update the statistics for the different fields.
                featInfo.updateStats(pattern);

                // b) We initialize the builders for the feature selections.
                for(int j = 0; j < numPartitions; ++j)
                {
                    StringBuilder builder = new StringBuilder();
                    builder.append(pattern.getCategory());
                    builder.append(SEPARATOR);
                    builder.append(QID);
                    builder.append(pattern.getOrigin());

                    strBuilders.set(j, builder);
                }

                // c) Generate the feature vector for each feature selection.
                List<Double> values = pattern.getValues();
                for(int j = 0; j < numFeats; ++j)
                {
                    double value = values.get(j);
                    int jidx = j;
                    partitions.getOrDefault(j, new HashSet<>()).forEach(k ->
                    {
                        StringBuilder builder = strBuilders.get(k);
                        builder.append(SEPARATOR);
                        builder.append(counters.get(k).get(jidx));
                        builder.append(IDSEP);
                        builder.append(value);
                    });
                }

                // d) Obtain the destination of the edge.
                String dest = COMMENT + DOCID + pattern.getDest();

                // e) Write the feature vectors.
                for(int j = 0; j < numPartitions; ++j)
                {
                    writers.get(j).write(strBuilders.get(j)+ SEPARATOR + dest + "\n");
                }

                // Increment the instance counter.
                int count = atom.incrementAndGet();
                if(count%10000 == 0)
                {
                    b = System.currentTimeMillis();
                    System.out.println("Computed " + count + " instances (" + (b-a)/1000.0 + " s.)");
                }

            }
            while ((line = br.readLine()) != null);
        }
        finally
        {
            for (Writer writer : writers) writer.close();
        }
    }


    /**
     * Generates the samples for a collection of datasets.
     * @param file the file.
     * @param expDirectory the directory in which to store the file.
     * @param partitions the different partitions.
     * @param numPartitions the number of partitions.
     * @param filename the name of the file.
     * @throws IOException if something fails while reading / writing.
     */
    private static void readAndSampleReducedDiscrete(String file, String expDirectory, Map<Integer, Set<Integer>> partitions, int numPartitions, int numFeats, String filename) throws IOException
    {
        long a = System.currentTimeMillis();
        long b;
        List<Writer> writers = new ArrayList<>();
        List<StringBuilder> strBuilders = new ArrayList<>();
        List<Map<Integer, Integer>> counters = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file))))
        {
            InstanceSet<Long> instanceSet;
            InstanceSetReader<Long> reader = new LETORInstanceReader<>(Parsers.lp, numFeats, Generators.longgen);

            for (int i = 0; i < numPartitions; ++i)
            {
                writers.add(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(expDirectory + i + File.separator + "jforests-discrete-" + filename))));
                strBuilders.add(new StringBuilder());
                counters.add(new HashMap<>());
            }

            for (int i = 0; i < numFeats; ++i)
            {
                int idx = i;
                partitions.getOrDefault(i, new HashSet<>()).forEach(j -> counters.get(j).put(idx,counters.get(j).size()+1));
            }

            // In this case, we have no headers:
            String line;
            int counter = 0;
            // Then, read each instance.
            while((line = br.readLine()) != null)
            {
                Instance<Long> pattern = reader.readInstance(line, numFeats);

                for(int i = 0; i < numPartitions; ++i)
                {
                    StringBuilder builder = new StringBuilder();
                    builder.append(pattern.getCategory());
                    builder.append(SEPARATOR);
                    builder.append(QID);
                    builder.append(pattern.getOrigin());

                    strBuilders.set(i, builder);
                }

                String dest = COMMENT + DOCID + pattern.getDest();

                // Run over the list of values.
                List<Double> values = pattern.getValues();
                for(int j = 0; j < numFeats; ++j)
                {
                    double value = values.get(j);
                    int jidx = j;
                    partitions.getOrDefault(j, new HashSet<>()).forEach(k ->
                    {
                        StringBuilder builder = strBuilders.get(k);
                        builder.append(SEPARATOR);
                        builder.append(counters.get(k).get(jidx));
                        builder.append(IDSEP);
                        builder.append(value);
                    });
                }

                for(int i = 0; i < numPartitions; ++i)
                {
                    writers.get(i).write(strBuilders.get(i) + dest + "\n");
                }

                counter++;
                if(counter % 10000 == 0)
                {
                    b = System.currentTimeMillis();
                    System.out.println("Computed " + counter + " instances (" + (b-a)/1000.0 + " s.)");
                }
            }
        }
        finally
        {
            for (Writer writer : writers) writer.close();
        }
    }
}
