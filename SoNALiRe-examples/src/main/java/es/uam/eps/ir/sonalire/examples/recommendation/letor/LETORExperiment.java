/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.examples.recommendation.letor;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.runner.RecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilterRecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilters;
import es.uam.eps.ir.sonalire.graph.Adapters;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.io.graph.TextGraphReader;
import es.uam.eps.ir.sonalire.links.data.FastGraphIndex;
import es.uam.eps.ir.sonalire.links.data.GraphIndex;
import es.uam.eps.ir.sonalire.links.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.sonalire.links.data.letor.FeatureInformation;
import es.uam.eps.ir.sonalire.links.data.letor.Instance;
import es.uam.eps.ir.sonalire.links.data.letor.InstanceSet;
import es.uam.eps.ir.sonalire.links.data.letor.io.InstanceSetReader;
import es.uam.eps.ir.sonalire.links.data.letor.io.LETORInstanceReader;
import es.uam.eps.ir.sonalire.links.recommendation.SocialFastFilters;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.supervised.LambdaMARTRecommender;
import es.uam.eps.ir.sonalire.utils.generator.Generators;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.ranksys.formats.parsing.Parsers;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static es.uam.eps.ir.sonalire.links.data.letor.io.LETORFormatConstants.*;
import static org.ranksys.formats.parsing.Parsers.lp;

/**
 * Learning to rank experiment.
 *
 * The experiment takes some training/validation/test LETOR files, and another one
 * specifying selections of features.
 * Then, for each possible feature selection, applies the LambdaMART algorithm.
 *
 * It requires the binary for the JForests library <a href=https://github.com/yasserg/jforests>https://github.com/yasserg/jforests</a>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 */
public class LETORExperiment
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

        // First, read the instance set parameters
        String trainFile = args[0];
        String validFile = args[1];
        String testFile = args[2];

        // A file containing lists of comma separated features.
        String partitionsFile = args[3];

        // Terrier execution parameters
        String expDirectory = args[4];
        String terrier = args[5];
        String jforestprop = args[6];

        // Ranksys execution parameters.
        String recTrain = args[7];
        String recTest = args[8];
        boolean directed = args[9].equalsIgnoreCase("true");
        boolean weighted = args[10].equalsIgnoreCase("true");
        String recOutput = args[11];
        int maxLength = Parsers.ip.parse(args[12]);

        boolean sample = args[13].equalsIgnoreCase("true");
        boolean copy = args[14].equalsIgnoreCase("true");

        // Obtain the different partitions. The map has each feature as index,
        // and the set of partitions it appears in as value.
        Map<Integer, Set<Integer>> partitions = new HashMap<>();
        int numPartitions = LETORExperiment.readPartitionsFile(partitionsFile, partitions);

        /* STEP 1: Generate the training/validation/test sets for each different partition
           In this step, we also build the jforest-discrete LETOR file for training/validation/test
           sets (only for the whole set).
         */

        // Obtain the directory containing the learning to rank features.
        File f = new File(trainFile);
        String parent = f.getParent();
        if (!parent.endsWith(File.separator)) parent += File.separator;
        String trainFileRaw = new File(trainFile).getName();
        String validFileRaw = new File(validFile).getName();
        String testFileRaw = new File(testFile).getName();
        long bb;
        long aa = System.currentTimeMillis();

        int numFeats;
        if(sample)
        {

            // First, create the directories to store all the datasets.
            for (int i = 0; i < partitions.size(); ++i)
            {
                String directoryName = expDirectory + i + File.separator;
                File dir = new File(directoryName);
                boolean createddir = dir.mkdir();
            }

            // Read and generate the training features.
            FeatureInformation trainFeatInfo = LETORExperiment.readAndSample(trainFile, expDirectory, partitions, numPartitions, "train.letor");
            bb = System.currentTimeMillis();
            System.out.println("Train features computed (" + (bb - aa) / 1000.0 + " s.)");

            // Read and generate the validation features.
            FeatureInformation validFeatInfo = LETORExperiment.readAndSample(validFile, expDirectory, partitions, numPartitions,"valid.letor");
            bb = System.currentTimeMillis();
            System.out.println("Validation features computed (" + (bb - aa) / 1000.0 + " s.)");

            // Read and generate the test features.
            FeatureInformation testFeatInfo = LETORExperiment.readAndSample(testFile, expDirectory, partitions, numPartitions, "test.letor");
            bb = System.currentTimeMillis();
            System.out.println("Test features computed (" + (bb - aa) / 1000.0 + " s.)");

            numFeats = trainFeatInfo.numFeats();

            List<StringBuilder> builders = new ArrayList<>();
            List<Integer> counters = new ArrayList<>();
            for(int i = 0; i < numPartitions; ++i)
            {
                StringBuilder builder = new StringBuilder();
                builder.append("FeatureIndex\tname\tMin\tMax");
                builders.add(builder);
                counters.add(1);
            }

            // Now, we generate, for each feature selection, the corresponding jforests-feature-stats.txt, which includes
            // the statistics for each set.
            for(int i = 0; i < numFeats; ++i)
            {
                double min = Math.min(trainFeatInfo.getStats(i).getMin(), validFeatInfo.getStats(i).getMin());
                double max = Math.max(trainFeatInfo.getStats(i).getMax(), validFeatInfo.getStats(i).getMax());
                partitions.getOrDefault(i, new HashSet<>()).forEach(j ->
                {
                    int counter = counters.get(j);
                    StringBuilder builder = builders.get(j);
                    builder.append("\n");
                    builder.append(counter);
                    builder.append("\tnull\t");
                    builder.append(min);
                    builder.append("\t");
                    builder.append(max);
                    counters.set(j, counter+1);
                });
            }

            for(int i = 0; i < numPartitions; ++i)
            {
                try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(expDirectory + i + File.separator + "jforests-feature-stats.txt"))))
                {
                    bw.write(builders.get(i).toString());
                }
            }
        }
        else // In case the previous step had already been done, we just read the global jforest-feature-stats.txt file.
        {
            // Read the jforests-feature-stats.txt
            try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(parent + "jforests-feature-stats.txt"))))
            {
                StringBuilder strBuilder = new StringBuilder();
                // Read the header
                String line = br.readLine();
                strBuilder.append(line);
                int i = 0;
                while((line = br.readLine()) != null)
                {
                    strBuilder.append("\n");
                    strBuilder.append(line);
                    if(i > 0)
                    {
                        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(expDirectory + i + File.separator + "jforests-feature-stats.txt"))))
                        {
                            bw.write(strBuilder.toString());
                        }
                    }
                    ++i;
                }

                numFeats = i;
            }
        }


        /*
         * STEP 2: Generate the corresponding binary files for each training/validation/test sets.
         * For this, we require the use of the Terrier binary. The Terrier binary shall generate these files.
         */
        if(copy)
        {
            // a) We generate bin files for the training and validation sets.
            String cmda = terrier + " --config-file " + jforestprop + " --cmd=generate-bin --ranking --folder " + parent + " --file " + trainFileRaw + " --file " + validFileRaw;
            Process processa = Runtime.getRuntime().exec(cmda);
            processa.waitFor();
            bb = System.currentTimeMillis();
            System.out.println("Generated bins for train and validation with all features (" + (bb - aa) / 1000.0 + " s.)");

            // b) We generate bin files for the test set.
            cmda = terrier + " --config-file " + jforestprop + " --cmd=generate-bin --ranking --folder " + parent + " --file " + testFileRaw;
            processa = Runtime.getRuntime().exec(cmda);
            processa.waitFor();
            bb = System.currentTimeMillis();
            System.out.println("Generated bins for test with all features (" + (bb - aa) / 1000.0 + " s.)");

            // And read and split in the same way as above (read this only once):
            trainFileRaw = "jforests-discrete-" + trainFileRaw;
            validFileRaw = "jforests-discrete-" + validFileRaw;
            testFileRaw = "jforests-discrete-" + testFileRaw;

            LETORExperiment.readAndSampleReducedDiscrete(parent + trainFileRaw, expDirectory, partitions, numPartitions, numFeats, "train.letor");
            bb = System.currentTimeMillis();
            System.out.println("Train reduced features computed (" + (bb - aa) / 1000.0 + " s.)");
            LETORExperiment.readAndSampleReducedDiscrete(parent + validFileRaw, expDirectory, partitions, numPartitions, numFeats, "valid.letor");
            bb = System.currentTimeMillis();
            System.out.println("Validation reduced features computed (" + (bb - aa) / 1000.0 + " s.)");
            LETORExperiment.readAndSampleReducedDiscrete(parent + testFileRaw, expDirectory, partitions, numPartitions, numFeats, "test.letor");
            bb = System.currentTimeMillis();
            System.out.println("Test reduced features computed (" + (bb - aa) / 1000.0 + " s.)");
        }

        /*
         * Step 3: We read the graphs.
         */
        TextGraphReader<Long> greader = new TextGraphReader<>(directed, weighted, true, "\t", Parsers.lp);
        Graph<Long> auxgraph = greader.read(recTrain, weighted, false);
        if(auxgraph == null)
        {
            System.err.println("ERROR: Could not read the training graph");
            return;
        }
        FastGraph<Long> graph = (FastGraph<Long>) Adapters.removeAutoloops(auxgraph);
        if(graph == null)
        {
            System.err.println("ERROR: Could not remove autoloops from the training graph");
            return;
        }
        auxgraph = greader.read(recTest, weighted, false);
        if(auxgraph == null)
        {
            System.err.println("ERROR: Could not read the test graph");
            return;
        }
        FastGraph<Long> testGraph = (FastGraph<Long>) Adapters.onlyTrainUsers(auxgraph, graph);
        if(testGraph == null)
        {
            System.err.println("ERROR: Could not restrict the test graph");
            return;
        }

        // Read the training and test data
        FastPreferenceData<Long, Long> trainData;
        trainData = GraphSimpleFastPreferenceData.load(graph);

        FastPreferenceData<Long, Long> testData;
        testData = GraphSimpleFastPreferenceData.load(testGraph);
        GraphIndex<Long> index = new FastGraphIndex<>(graph);

        bb = System.currentTimeMillis();
        System.out.println("Recommendation data read(" + (bb-aa)/1000.0 + " s.)");

        // Execute the recommender
        // Select the set of users to be recommended, the format, and the filters to apply to the recommendation
        Set<Long> targetUsers = testData.getUsersWithPreferences().collect(Collectors.toCollection(HashSet::new));
        System.out.println("Num. target users: " + targetUsers.size());
        RecommendationFormat<Long, Long> format = new SimpleRecommendationFormat<>(lp, lp);
        Function<Long,IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(trainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(graph, index));

        /*
         * Step 4: Now that all the elements have been read, and feature vectors have been generated, we can execute the experiment
         */

        IntStream.range(0, numPartitions).parallel().forEach(i ->
        {
            try
            {
                long a = System.currentTimeMillis();
                System.out.println("Starting experiment with the " + (i+1) + "-th feature selection");

                // a) We obtain the corresponding directory for the partition.
                String directoryName = expDirectory + i + File.separator;
                File dir = new File(directoryName);

                // b) We generate the .bin files for training and validation.
                String cmd = terrier + " --config-file " + jforestprop + " --cmd=generate-bin --ranking --folder " + directoryName + " --file train.letor --file valid.letor > " + directoryName + "bins-train.txt";
                System.out.println(cmd);
                Process process = Runtime.getRuntime().exec(cmd);
                process.waitFor();
                long b = System.currentTimeMillis();
                System.out.println("Generated bins for train and validation with the " + (i+1) +"-th set of features (" + (b-a)/1000.0 + " s.)");

                // c) We generate the .bin files for test.
                cmd = terrier + " --config-file " + jforestprop + " --cmd=generate-bin --ranking --folder " + directoryName + " --file test.letor > " + directoryName + "bins-test.txt";
                System.out.println(cmd);
                process = Runtime.getRuntime().exec(cmd);
                process.waitFor();
                b = System.currentTimeMillis();
                System.out.println("Generated bins for test with the " + (i+1) +"-th set of features (" + (b-a)/1000.0 + " s.)");

                // d) We train the model.
                cmd = terrier + " --config-file " + jforestprop + " --cmd=train --ranking --train-file " + directoryName + "train.bin --validation-file " + directoryName + "valid.bin --output-model " + directoryName + "model.txt";
                System.out.println(cmd);
                process = Runtime.getRuntime().exec(cmd);
                process.waitFor();
                b = System.currentTimeMillis();
                System.out.println("Model trained for experiment with the " + (i+1) + "-th set of features (" + (b-a)/1000.0 + " s.)");

                // e) We execute the predictions by the model.
                cmd = terrier + " --config-file " + jforestprop + " --cmd=predict --ranking --model " + directoryName + "model.txt --tree-type RegressionTree --test-file " + directoryName + "test.bin --output-file " + directoryName + "pred.txt";
                System.out.println(cmd);
                process = Runtime.getRuntime().exec(cmd);
                process.waitFor();
                b = System.currentTimeMillis();
                System.out.println("Model predictions done for experiment with the " + (i+1) + "-th set of features (" + (b-a)/1000.0 + " s.)");

                // f) We "execute" the recommendations, i.e. we obtain the recommendation rankings for each user from the output of
                // JForests.
                RecommenderRunner<Long,Long> runner = new FastFilterRecommenderRunner<>(index, index, targetUsers.stream(), filter, maxLength);
                Recommender<Long, Long> rec = new LambdaMARTRecommender<>(graph, directoryName + "test.letor", directoryName + "pred.txt", Parsers.lp);
                b = System.currentTimeMillis();
                System.out.println("Recommendation with the " + (i+1) + "-th set of features prepared (" + (b-a)/1000.0 + " s.)");

                RecommendationFormat.Writer<Long,Long> recWriter = format.getWriter(recOutput + i + ".txt");

                runner.run(rec, recWriter);
                recWriter.close();
                b = System.currentTimeMillis();
                System.out.println("Recommendation with the " + (i+1) + "-th set of features done (" + (b-a)/1000.0 + " s.)");
                 System.out.println("Experiment with " + i + " features done (" + (b-a)/1000.0 + " s.)");
             }
             catch (IOException ex)
             {
                 System.err.println("ERROR: Something failed while executing exp. for " + i + " features");
             } catch (InterruptedException ex) {
                 ex.printStackTrace();
             }
         });
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
     * @param file the route to the LETOR file.
     * @param expDirectory a directory in which we want to store the files.
     * @param partitions the different feature selections.
     * @param numPartitions the number of different feature selections.
     * @param filename the name of the new files.
     * @return the feature information for the whole LETOR file.
     * @throws IOException if something fails while reading / writing.
     */
    private static FeatureInformation readAndSample(String file, String expDirectory, Map<Integer, Set<Integer>> partitions, int numPartitions, String filename) throws IOException
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

            return featInfo;
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
