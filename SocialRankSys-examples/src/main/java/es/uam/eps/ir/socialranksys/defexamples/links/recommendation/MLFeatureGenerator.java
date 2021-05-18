/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.defexamples.links.recommendation;


import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.AuxiliarMethods;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridSelector;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.YAMLAlgorithmConfigurationsReader;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.sampling.IndividualSamplerFunction;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.sampling.IndividualSamplingAlgorithmGridSelector;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.sampling.YAMLIndividualSamplingAlgorithmGridReader;
import es.uam.eps.ir.socialranksys.grid.metrics.YAMLMetricConfigurationsReader;
import es.uam.eps.ir.socialranksys.grid.metrics.pair.PairMetricFunction;
import es.uam.eps.ir.socialranksys.grid.metrics.pair.PairMetricSelector;
import es.uam.eps.ir.socialranksys.grid.metrics.vertex.VertexMetricFunction;
import es.uam.eps.ir.socialranksys.grid.metrics.vertex.VertexMetricSelector;
import es.uam.eps.ir.socialranksys.io.graph.GraphReader;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.links.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.socialranksys.links.data.letor.FeatureInformation;
import es.uam.eps.ir.socialranksys.links.data.letor.FeatureType;
import es.uam.eps.ir.socialranksys.links.data.letor.Instance;
import es.uam.eps.ir.socialranksys.links.data.letor.InstanceSet;
import es.uam.eps.ir.socialranksys.links.data.letor.io.InstanceSetWriter;
import es.uam.eps.ir.socialranksys.links.data.letor.io.LETORInstanceWriter;
import es.uam.eps.ir.socialranksys.links.data.letor.sampling.IndividualSampler;
import es.uam.eps.ir.socialranksys.links.recommendation.filler.Filler;
import es.uam.eps.ir.socialranksys.links.recommendation.filler.RandomFiller;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.*;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.CompleteDistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import org.jooq.lambda.tuple.Tuple2;
import org.ranksys.core.util.tuples.Tuple2od;
import org.ranksys.formats.parsing.Parsers;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static es.uam.eps.ir.socialranksys.examples.AuxiliarVariables.TRUE;
import static es.uam.eps.ir.socialranksys.grid.metrics.MetricTypeIdentifiers.PAIR_METRIC;
import static es.uam.eps.ir.socialranksys.grid.metrics.MetricTypeIdentifiers.VERTEX_METRIC;
import static es.uam.eps.ir.socialranksys.links.data.letor.FeatureType.CONTINUOUS;

/**
 * Class for generating learning to rank / machine learning examples.
 * The examples are written in the LETOR format.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MLFeatureGenerator
{
    /**
     * Builds a set of learning to rank instances using similarities between
     * pairs of users.
     *
     * @param args Execution arguments:
     * <ul>
     *  <li><b>Train-instance graph:</b> Graph for obtaining the features of the training set</li>
     *  <li><b>Train-class graph:</b> Graph for obtaining the relevance of each training instance</li>
     *  <li><b>Test-instance graph:</b> Graph for obtaining the features of the test set</li>
     *  <li><b>Test-class graph:</b> Graph for obtaining the relevance of each test set example</li>
     *  <li><b>Directed:</b> true if the graph is directed, false otherwise</li>
     *  <li><b>Weighted sampling:</b> true if we want to use graph weights for the sampling procedures.</li>
     *  <li><b>Weighted classes:</b> true if we take weights as classes (otherwise, binary classes).</li>
     *  <li><b>Weighted features: </b> true if we have to use weights for computing the features.</li>
     *  <li><b>Train sampling:</b> configuration for the individual sampler used in training.</li>
     *  <li><b>Test sampling:</b> configuration for the individual sampler used in test.</li>
     *  <li><b>Configuration:</b> YAML file containing the configurations we want to use.</li>
     *  <li><b>Train output:</b> File to store the training examples</li>
     *  <li><b>Test output:</b> File to store the test examples</li>
     *  <li><b>Normalization:</b> Score normalization:
     *      <ul>
     *          <li><u>none:</u> No normalization</li>
     *          <li><u>ranksim:</u> Ranking normalization</li>
     *          <li><u>minmax:</u> Rescale the scores to interval [0,1]</li>
     *          <li><u>z-score:</u> Rescale the query to have 0 mean and 1 variance</li>
     *      </ul>
     * </ul>
     *
     * @throws IOException if something fails while reading/writing
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 15)
        {
            System.err.println("ERROR: Invalid arguments");
            System.err.println("\tTrain-instance graph: Graph for obtaining the features of the training set.");
            System.err.println("\tTrain-class graph: Graph for obtaining the relevance of each training instance.");
            System.err.println("\tTest-instance graph: Graph for obtaining the features of the test set.");
            System.err.println("\tTest-class graph: Graph for obtaining the relevance of each test instance.");
            System.err.println("\tDirected: true if the graph is directed, false otherwise.");
            System.err.println("\tWeighted sampling: true if we have to use graph weights for the sampling.");
            System.err.println("\tWeighted classes: true if we have to use weights as classes (otherwise, binary classes)");
            System.err.println("\tWeighted features: true if we have to use weights for computing the features.");
            System.err.println("\tTrain sampling: configuration for the individual sampler used in training.");
            System.err.println("\tTest sampling: configuration for the individual sampler used in test");
            System.err.println("\tConfiguration: YAML containing the recommender / SNA metric configurations we want to use");
            System.err.println("\tTrain output: file to store the training examples");
            System.err.println("\tTest output: file to store the test examples");
            System.err.println("\tDescription output: file to store the description of the dataset");
            System.err.println("\tNormalization: the score normalization for each query. Possible values:");
            System.err.println("\t\tnone: no normalization");
            System.err.println("\t\tranksim: ranking normalization");
            System.err.println("\t\tminmax: rescales the scores to interval [0,1]");
            System.err.println("\t\tz-score: rescales the scores to have mean 0 and variance 1");
        }

        // Files of the graphs for generating the training set.
        String patternsTrain = args[0];
        String patternsTest = args[1];

        // Files of the graphs for generating the test set.
        String train = args[2];
        String test = args[3];

        // Values to check whether the graphs are directed and weighted or not.
        boolean directed = args[4].equalsIgnoreCase(TRUE);
        // true if we want to consider the weights at the moment of using a sampling approach, false otherwise.
        boolean weightedSampling = args[5].equalsIgnoreCase(TRUE);
        // true if we want to consider the weights for storing the "class" information about the pair of users.
        boolean weightedClasses = args[6].equalsIgnoreCase(TRUE);
        // true if we want to consider weights for computing the features.
        boolean weightedFeatures = args[7].equalsIgnoreCase(TRUE);

        // Sampling: configuration for the sampling algorithms to use:
        String samplingTrain = args[8]; // configuration for training
        String samplingTest = args[9]; // configuration for test.

        // Algorithm grid: a set of contact recommendation algorithms / SNA metrics which can be used for computing features:
        String algorithmGrid = args[10];

        // Output files for training and test patterns.
        String outputTrain = args[11];
        String outputTest = args[12];
        String outputDescr = args[13];

        String normalization = args[14];
        boolean onlyTest = false;
        if(args.length > 15) // if we only want to compute the outcome.
            onlyTest = args[15].equalsIgnoreCase(TRUE);

        // First, we identify the algorithms we are going to use:
        YAMLAlgorithmConfigurationsReader algConfig = new YAMLAlgorithmConfigurationsReader();
        Map<String, Object> grid = AuxiliarMethods.readYAML(algorithmGrid);
        algConfig.read(grid);

        AlgorithmGridSelector<Long> selector = new AlgorithmGridSelector<>(Parsers.lp);
        List<String> descriptions = new ArrayList<>();
        List<FeatureType> types = new ArrayList<>();
        List<RecommendationAlgorithmFunction<Long>> algorithms = new ArrayList<>();
        Set<String> sims = algConfig.getAlgorithms();
        for(String sim : sims)
        {
            Map<String, RecommendationAlgorithmFunction<Long>> aux = selector.getRecommenders(sim, algConfig.getConfigurations(sim));
            if(aux == null || aux.isEmpty())
            {
                System.err.println(sim + " failed");
                return;
            }
            aux.forEach((key, value) ->
            {
                descriptions.add(key);
                types.add(CONTINUOUS);
                algorithms.add(value);
            });
        }

        YAMLMetricConfigurationsReader metricConfig = new YAMLMetricConfigurationsReader();
        metricConfig.read(grid);

        // Node metrics:
        VertexMetricSelector<Long> vmSelector = new VertexMetricSelector<>();
        List<VertexMetricFunction<Long>> vertexmetrics = new ArrayList<>();
        Set<String> vms = metricConfig.getMetrics(VERTEX_METRIC);
        for(String vm : vms)
        {
            Map<String, VertexMetricFunction<Long>> aux = vmSelector.getMetrics(vm, metricConfig.getConfigurations(vm, VERTEX_METRIC));
            if(aux == null || aux.isEmpty())
            {
                System.err.println(vm + " failed");
                return;
            }
            aux.forEach((key, value) ->
            {
                descriptions.add("orig_" + key);
                descriptions.add("dest_" + key);
                types.add(CONTINUOUS);
                types.add(CONTINUOUS);
                vertexmetrics.add(value);
            });
        }

        // Pair metrics:
        PairMetricSelector<Long> pmSelector = new PairMetricSelector<>();
        List<PairMetricFunction<Long>> pairmetrics = new ArrayList<>();
        Set<String> pms = metricConfig.getMetrics(PAIR_METRIC);
        for(String pm : pms)
        {
            Map<String, PairMetricFunction<Long>> aux = pmSelector.getMetrics(pm, metricConfig.getConfigurations(pm, PAIR_METRIC));
            if(aux == null || aux.isEmpty())
            {
                System.err.println(pm + " failed");
                return;
            }
            aux.forEach((key, value) ->
            {
                descriptions.add(key);
                types.add(CONTINUOUS);
                pairmetrics.add(value);
            });
        }

        long a = System.currentTimeMillis();
        long b;
        FeatureInformation featInfo;
        if(!onlyTest)
        {

            featInfo = MLFeatureGenerator.computeInstances(patternsTrain, patternsTest, directed, weightedSampling, weightedClasses, weightedFeatures, samplingTrain, outputTrain, descriptions,types, algorithms, vertexmetrics, pairmetrics, normalization);
            b = System.currentTimeMillis();
            if(featInfo == null)
            {
                System.out.println("ERROR: Training patterns were not generated");
                return;
            }
            else
            {
                InstanceSetWriter<Long> writer = new LETORInstanceWriter<>();
                String feats = writer.writeFeatureInfo(featInfo);
                try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputDescr))))
                {
                    bw.write(feats);
                }
            }
            System.out.println("Finished training patterns (" + (b-a)/1000.0 + " s.)");
        }

        featInfo = MLFeatureGenerator.computeInstances(train, test, directed, weightedSampling, weightedClasses, weightedFeatures, samplingTest, outputTest, descriptions, types, algorithms, vertexmetrics, pairmetrics, normalization);
        if(featInfo == null)
        {
            System.out.println("ERROR: Test patterns were not generated");
            return;
        }
        b = System.currentTimeMillis();
        System.out.println("Finished test patterns (" + (b-a)/1000.0 + " s.)");
    }

    /**
     * Computes the instances for a pair of graphs.
     * @param train             training graph.
     * @param test              validation/test graph.
     * @param directed          true if the graph is directed, false otherwise.
     * @param weightedSampling  true if the graph is weighted, false otherwise.
     * @param sampling          sampling algorithm grid.
     * @param output            file in which to output the examples.
     * @param descriptions      list of features.
     * @param types             list of types.
     * @param similarities      list of similarity functions.
     * @throws IOException if something failed while creating the instances.
     */
    private static FeatureInformation computeInstances(String train, String test, boolean directed, boolean weightedSampling, boolean weightedClasses, boolean weightedFeatures, String sampling, String output, List<String> descriptions, List<FeatureType> types, List<RecommendationAlgorithmFunction<Long>> similarities, List<VertexMetricFunction<Long>> vertexmetrics, List<PairMetricFunction<Long>> pairmetrics, String normalization) throws IOException
    {
        // First, we build the feature information for the instance set:
        FeatureInformation featInfo = new FeatureInformation(descriptions,types);
        InstanceSet<Long> patternSet = new InstanceSet<>(featInfo);

        // Then, we obtain the sampling algorithm, for prefiltering the set of candidate users for each target one:
        long a = System.currentTimeMillis();

        YAMLIndividualSamplingAlgorithmGridReader gridReader = new YAMLIndividualSamplingAlgorithmGridReader();
        Map<String, Object> samplerMap = AuxiliarMethods.readYAML(sampling);
        gridReader.read(samplerMap);
        Set<String> auxalgs = gridReader.getIndividualSamplingAlgorithms();
        if(auxalgs == null || auxalgs.isEmpty())
        {
            System.err.println("ERROR: Not sampling algorithm found");
            return null;
        }
        List<String> algorithms = new ArrayList<>(gridReader.getIndividualSamplingAlgorithms());
        String algorithm = algorithms.get(0);

        // Once it is identified, we obtain it:
        IndividualSamplingAlgorithmGridSelector<Long> gridSelector = new IndividualSamplingAlgorithmGridSelector<>();
        Tuple2<String, IndividualSamplerFunction<Long>> function = gridSelector.getIndividualSamplingAlgorithm(algorithm, gridReader.getParameters(algorithm), Parsers.lp);
        long b = System.currentTimeMillis();
        System.out.println("Individual sampler: " + function.v1() + " (" + (b-a)/1000.0 + " s.)");

        // Now, we read the training graph, for applying the sampling:
        GraphReader<Long> greader = new TextGraphReader<>(directed, weightedSampling, false, "\t", Parsers.lp);
        FastGraph<Long> trainGraph = (FastGraph<Long>) greader.read(train, weightedSampling, false);
        FastPreferenceData<Long, Long> prefData = GraphSimpleFastPreferenceData.load(trainGraph);

        greader = new TextGraphReader<>(directed, weightedClasses, false, "\t", Parsers.lp);
        FastGraph<Long> testGraph = (FastGraph<Long>) greader.read(test, weightedClasses, false);

        b = System.currentTimeMillis();
        System.out.println("Graphs for sampling read: " + (b-a)/1000.0 + " s.)");

        // Then, we do obtain the query users:
        List<Long> queryUsers = testGraph.getAllNodes().filter(u -> testGraph.getAdjacentEdgesCount(u) > 0 && trainGraph.containsVertex(u)).collect(Collectors.toCollection(ArrayList::new));
        // And, for each of them, we do obtain the candidate users:
        IndividualSampler<Long> sampler = function.v2().apply(trainGraph, testGraph, prefData);

        Set<Pair<Long>> samples = new HashSet<>();
        Map<Long, Set<Long>> categorizedSamples = new HashMap<>();
        int numSamples = 0;
        for(Long u : queryUsers)
        {
            Set<Long> sample = sampler.sampleUsers(u, v -> !trainGraph.containsEdge(u, v) && !trainGraph.containsEdge(v,u) && !u.equals(v));
            numSamples += sample.stream().mapToInt(v ->
            {
                samples.add(new Pair<>(u,v));
                return 1;
            }).sum();
            categorizedSamples.put(u, sample);
        }

        b = System.currentTimeMillis();
        System.out.println("Queries sampled: " + (b-a)/1000.0 + " s.)");
        System.out.println("Total samples: " + numSamples);

        // We continue by reading the training network, so we can compute the different features:
        FastGraph<Long> defTrainGraph;
        FastPreferenceData<Long, Long> defPrefData;
        if(weightedSampling != weightedFeatures)
        {
            greader = new TextGraphReader<>(directed, weightedFeatures, false, "\t", Parsers.lp);
            defTrainGraph = (FastGraph<Long>) greader.read(train, weightedFeatures, false);
            defPrefData = GraphSimpleFastPreferenceData.load(defTrainGraph);
        }
        else
        {
            defTrainGraph = trainGraph;
            defPrefData = prefData;
        }

        b = System.currentTimeMillis();

        System.out.println("Training graph for features read (" + (b-a)/1000.0 + " s.)");

        int numUsers = Long.valueOf(trainGraph.getVertexCount()).intValue();
        Map<String, Map<Pair<Long>, Double>> simRes = new ConcurrentHashMap<>();
        AtomicInteger atomCounter = new AtomicInteger(0);
        int total = similarities.size();

        // For each recommendation algorithm:
        IntStream.range(0, total).parallel().forEach(i ->
        {
            // We compute the value for each pair:
            Map<Pair<Long>,Double> map = new HashMap<>();
            RecommendationAlgorithmFunction<Long> f = similarities.get(i);
            String name = descriptions.get(i);
            long auxa = System.currentTimeMillis();
            System.out.println("Similarity " + name + " started: (" + (auxa - a) + " ms.");
            Recommender<Long,Long> rec = f.apply(defTrainGraph, defPrefData);
            queryUsers.forEach(u ->
            {
                Recommendation<Long, Long> recomm = rec.getRecommendation(u, x -> categorizedSamples.get(u).contains(x));
                Filler<Long, Long> filler = new RandomFiller<>(defPrefData, 0);

                recomm = filler.fill(recomm,numUsers , v -> x -> categorizedSamples.get(v).contains(x));

                Recommendation<Long, Long> normRecomm = MLFeatureGenerator.normalize(recomm, normalization);

                List<Tuple2od<Long>> items = normRecomm.getItems();
                for(Tuple2od<Long> item : items)
                {
                    Pair<Long> pair = new Pair<>(u, item.v1);
                    if(samples.contains(pair))
                    {
                        map.put(pair, item.v2);
                    }
                }
            });
            simRes.put(name, map);
            auxa = System.currentTimeMillis();
            int count  = atomCounter.incrementAndGet();
            System.out.println("Similarity " + name + " finished: (" + (auxa - a) + "ms.) " + count + "/" + similarities.size());
        });

        DistanceCalculator<Long> calc = new CompleteDistanceCalculator<>();
        if(vertexmetrics.size() > 0 && pairmetrics.size() > 0)
            calc.computeDistances(defTrainGraph);

        total = vertexmetrics.size();
        IntStream.range(0, total).parallel().forEach(i ->
        {
            Map<Pair<Long>, Double> origmap = new HashMap<>();
            Map<Pair<Long>, Double> destmap = new HashMap<>();
            VertexMetricFunction<Long> f = vertexmetrics.get(i);
            VertexMetric<Long> vm = f.apply(calc);

            String nameorig = descriptions.get(similarities.size() + 2*i);
            String namedest = descriptions.get(similarities.size() + 2*i + 1);

            long auxa = System.currentTimeMillis();
            System.out.println("Vertex metric " + nameorig + " started: (" + (auxa - a) + " ms.");

            Map<Long, Double> values = vm.compute(defTrainGraph);
            Map<Long, Double> defValues = MLFeatureGenerator.normalize(values, normalization);

            samples.forEach(pair ->
            {
                origmap.put(pair, defValues.get(pair.v1()));
                destmap.put(pair, defValues.get(pair.v2()));
            });

            simRes.put(nameorig, origmap);
            simRes.put(namedest, destmap);
            int count = atomCounter.incrementAndGet();
            System.out.println("Vertex metric " + namedest + " finished: (" + (auxa - a) + "ms.) " + count + "/" + vertexmetrics.size());
        });

        total = vertexmetrics.size();
        IntStream.range(0, total).parallel().forEach(i ->
        {
            Map<Long, List<Tuple2od<Long>>> recs = new HashMap<>();

            PairMetricFunction<Long> f = pairmetrics.get(i);
            PairMetric<Long> pm = f.apply(calc);

            String name = descriptions.get(similarities.size() + 2*vertexmetrics.size() + i);

            long auxa = System.currentTimeMillis();
            System.out.println("Pair metric " + name + " started: (" + (auxa - a) + " ms.");

            Map<Pair<Long>, Double> values = pm.compute(defTrainGraph, samples.stream());
            values.forEach((key, value) ->
            {
                if(!recs.containsKey(key.v1())) recs.put(key.v1(), new ArrayList<>());
                recs.get(key.v1()).add(new Tuple2od<>(key.v2(), value));
            });
            Map<Pair<Long>, Double> defValues = new HashMap<>();

            recs.forEach((key, value) ->
            {
                value.sort((o1, o2) -> Double.compare(o2.v2, o1.v2));
                Recommendation<Long, Long> rec = new Recommendation<>(key, value);
                Recommendation<Long, Long> recVal = MLFeatureGenerator.normalize(rec, normalization);
                recVal.getItems().forEach(t -> defValues.put(new Pair<>(key, t.v1), t.v2));
            });

            simRes.put(name, defValues);
            int count = atomCounter.incrementAndGet();
            System.out.println("Pair metric " + name + " finished: (" + (auxa - a) + "ms.) " + count + "/" + vertexmetrics.size());
        });

        AtomicInteger atom = new AtomicInteger(0);
        samples.forEach(pair ->
        {
            long u = pair.v1();
            long v = pair.v2();

            boolean introduce = true;
            List<Double> values = new ArrayList<>();
            for(String descr : descriptions)
            {
                Double val = simRes.get(descr).getOrDefault(pair, Double.NaN);
                if(Double.isNaN(val))
                {
                    introduce = false;
                    break;
                }
                values.add(val);
            }

            int category = testGraph.containsEdge(u,v) ? 1 : 0;

            if(introduce)
            {
                Instance<Long> pattern = new Instance<>(u, v, values, category);
                patternSet.addInstance(pattern);

            }

            int counter = atom.incrementAndGet();
            if(counter % 1000 == 0)
            {
                long auxb = System.currentTimeMillis();
                System.out.println(counter + " patterns computed: " + (auxb-a)/1000.0 + " s.)");
            }
        });

        InstanceSetWriter<Long> writer = new LETORInstanceWriter<>();
        writer.write(output, patternSet);

        return featInfo;
    }

    /**
     * Normalizes a recommendation.
     * @param recomm the recommendation.
     * @param normalization the identifier of the normalization algorithm.
     * @return the normalized recommendation.
     */
    private static Recommendation<Long, Long> normalize(Recommendation<Long, Long> recomm, String normalization)
    {
        List<Tuple2od<Long>> normalized = new ArrayList<>();
        Normalizer<Long> norm = switch (normalization)
        {
            case "ranksim" -> new RanksimNormalizer<>();
            case "zscore" -> new ZScoreNormalizer<>();
            case "minmax" -> new MinMaxNormalizer<>();
            default -> new NoNormalizer<>();
        };

        recomm.getItems().forEach(i -> norm.add(i.v1, i.v2));
        recomm.getItems().forEach(i -> normalized.add(new Tuple2od<>(i.v1,norm.norm(i.v1, i.v2))));
        return new Recommendation<>(recomm.getUser(), normalized);
    }

    /**
     * Normalizes a recommendation.
     * @param recomm the recommendation.
     * @param normalization the identifier of the normalization algorithm.
     * @return the normalized recommendation.
     */
    private static <L> Map<L, Double> normalize(Map<L, Double> recomm, String normalization)
    {
        Map<L, Double> normalized = new HashMap<>();
        Normalizer<L> norm = switch (normalization)
        {
            case "ranksim" -> new RanksimNormalizer<>();
            case "zscore" -> new ZScoreNormalizer<>();
            case "minmax" -> new MinMaxNormalizer<>();
            default -> new NoNormalizer<>();
        };

        recomm.forEach(norm::add);
        recomm.forEach((key, value) -> normalized.put(key, norm.norm(key, value)));
        return normalized;
    }
}