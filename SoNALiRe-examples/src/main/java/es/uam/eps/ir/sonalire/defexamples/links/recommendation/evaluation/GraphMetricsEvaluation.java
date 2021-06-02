/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.defexamples.links.recommendation.evaluation;

import es.uam.eps.ir.sonalire.AuxiliarMethods;
import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.community.io.TextCommunitiesReader;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.generator.GraphCloneGenerator;
import es.uam.eps.ir.sonalire.graph.generator.GraphGenerator;
import es.uam.eps.ir.sonalire.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.sonalire.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.sonalire.grid.sna.MetricGridReader;
import es.uam.eps.ir.sonalire.grid.sna.MetricTypeIdentifiers;
import es.uam.eps.ir.sonalire.grid.sna.comm.global.GlobalCommunityMetricSelector;
import es.uam.eps.ir.sonalire.grid.sna.comm.indiv.IndividualCommunityMetricSelector;
import es.uam.eps.ir.sonalire.grid.sna.graph.GraphMetricFunction;
import es.uam.eps.ir.sonalire.grid.sna.graph.GraphMetricSelector;
import es.uam.eps.ir.sonalire.grid.sna.pair.PairMetricFunction;
import es.uam.eps.ir.sonalire.grid.sna.pair.PairMetricSelector;
import es.uam.eps.ir.sonalire.grid.sna.vertex.VertexMetricFunction;
import es.uam.eps.ir.sonalire.grid.sna.vertex.VertexMetricSelector;
import es.uam.eps.ir.sonalire.io.graph.TextGraphReader;
import es.uam.eps.ir.sonalire.io.graph.TextMultiGraphReader;
import es.uam.eps.ir.sonalire.metrics.*;
import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.distance.FastDistanceCalculator;
import es.uam.eps.ir.sonalire.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;
import org.ranksys.formats.parsing.Parsers;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Class for analyzing the properties of a graph after adding the outcome of a contact recommendation / link prediction
 * algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class GraphMetricsEvaluation
{
    /**
     * Program which analyzes the different properties of a graph.
     * @param args Execution arguments
     *             <ol>
     *                  <li><b>Train:</b> Route to the file containing the training graph.</li>
     *                  <li><b>Test:</b> Route to the file containing the test links.</li>
     *                  <li><b>Multigraph:</b> true if the network allows multiple edges, false otherwise.</li>
     *                  <li><b>Directed:</b> true if the network is directed, false otherwise.</li>
     *                  <li><b>Weighted:</b> true if the network is weighted, false otherwise.</li>
     *                  <li><b>Selfloops:</b> true if the network allows self-loops, false otherwise.</li>
     *                  <li><b>Rec. directory:</b> directory containing the recommendations.</li>
     *                  <li><b>Grid:</b> Route to a YAML file containing the structural metric configurations.</li>
     *                  <li><b>Output file:</b> File in which to store the structural metrics.</li>
     *                  <li><b>Rec. Length:</b> Maximum number of recommendations per user to consider.</li>
     *                  <li><b>Full graph:</b> if true, it uses all the edges/pairs of users in the networks. If false, only the recommended ones.</li>
     *                  <li><b>Only relevant:</b> true if we only add to the original network only those correctly recommended links, false otherwise.</li>
     *             </ol>
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 13)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tTrain: Route to the file containing the training graph.");
            System.err.println("\tTest: Route to the file containing the test links.");
            System.err.println("\tMultigraph: true if the network allows multiple edges, false otherwise.");
            System.err.println("\tDirected: true if the network is directed, false otherwise.");
            System.err.println("\tWeighted: true if the network is weighted, false otherwise.");
            System.err.println("\tSelfloops: true if the network allows self-loops, false otherwise.");
            System.err.println("\tRec. directory: directory containing the recommendations.");
            System.err.println("\tGrid: Route to a YAML file containing the structural metric configurations.");
            System.err.println("\tOutput file: File in which to store the structural metrics.");
            System.err.println("\tRec. Length: Maximum number of recommendations per user to consider.");
            System.err.println("\tFull graph: if true, it uses all the edges/pairs of users in the networks. If false, only the recommended ones.");
            System.err.println("\tOnly relevant: true if we only add to the original network only those correctly recommended links, false otherwise.");
            return;
        }

        // Argument reading
        String graphFile = args[0];
        String testGraphFile = args[1];

        boolean multigraph = args[2].equalsIgnoreCase("true");
        boolean directed = args[3].equalsIgnoreCase("true");
        boolean weighted = args[4].equalsIgnoreCase("true");
        boolean selfloops = args[5].equalsIgnoreCase("true");

        String recRoute = args[6];

        String[] comms = args[7].split(",");
        List<String> commFiles = Arrays.asList(comms);

        String metricGrid = args[8];

        String outputFile = args[9];
        int length = Parsers.ip.parse(args[10]);
        boolean fullGraph = args[11].equalsIgnoreCase("true");
        boolean onlyrel = args[12].equalsIgnoreCase("true");

        // Read the graphs
        long a = System.currentTimeMillis();
        
        TextGraphReader<Long> greader = (multigraph) ? new TextMultiGraphReader<>(directed, weighted, selfloops, "\t", Parsers.lp) :
        new TextGraphReader<>(directed, weighted, false, "\t", Parsers.lp);
        Graph<Long> graph = greader.read(graphFile, weighted, false);
        Graph<Long> testGraph = greader.read(testGraphFile, false, false);


        if(graph == null || testGraph == null)
        {
            System.err.println("ERROR: graphs could not be read.");
            return;
        }
        long b = System.currentTimeMillis();
        System.out.println("Graphs read (" + (b-a) + " ms.");


        // Read the communities
        a = System.currentTimeMillis();

        Map<String, Communities<Long>> communities = new HashMap<>();
        TextCommunitiesReader<Long> creader = new TextCommunitiesReader<>("\t", Parsers.lp);
        commFiles.forEach((comm) ->
        {
            File f = new File(comm);
            String c = f.getName();
            communities.put(c, creader.read(comm));
        });

        b = System.currentTimeMillis();
        System.out.println("Communities read (" + (b-a) + " ms.");


        // Read the metrics to compute:
        a = System.currentTimeMillis();
        MetricGridReader gridReader = new MetricGridReader();
        Map<String, Object> yamlMap = AuxiliarMethods.readYAML(metricGrid);
        gridReader.read(yamlMap);
        b = System.currentTimeMillis();
        System.out.println("Grid read (" + (b-a) + " ms.)");

        // Vertex metrics
        System.out.println("Starting vertex metrics...");
        Map<String, VertexMetricFunction<Long>> vertexMetrics = new HashMap<>();
        VertexMetricSelector<Long> vertexSelector = new VertexMetricSelector<>();
        gridReader.getMetrics(MetricTypeIdentifiers.VERTEX_METRIC).forEach(metric ->
        {
            Map<String, VertexMetricFunction<Long>> map = vertexSelector.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.VERTEX_METRIC));
            vertexMetrics.putAll(map);
        });
        System.out.println("Identified " + vertexMetrics.size() + " vertex metrics");

        // Edge metrics
        System.out.println("Starting edge metrics...");
        Map<String, PairMetricFunction<Long>> edgeMetrics = new HashMap<>();
        PairMetricSelector<Long> edgeSelector = new PairMetricSelector<>();
        gridReader.getMetrics(MetricTypeIdentifiers.EDGE_METRIC).forEach(metric ->
        {
            Map<String,PairMetricFunction<Long>> map = edgeSelector.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.EDGE_METRIC));
            edgeMetrics.putAll(map);
        });
        System.out.println("Identified " + edgeMetrics.size() + " edge metrics");

        // Pair metrics
        System.out.println("Starting pair metrics...");
        Map<String, PairMetricFunction<Long>> pairMetrics = new HashMap<>();
        PairMetricSelector<Long> pairSelector = new PairMetricSelector<>();
        gridReader.getMetrics(MetricTypeIdentifiers.PAIR_METRIC).forEach(metric ->
        {
            Map<String,PairMetricFunction<Long>> map = pairSelector.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.PAIR_METRIC));
            pairMetrics.putAll(map);
        });
        System.out.println("Identified " + pairMetrics.size() + " pair metrics");

        // Individual community metrics
        System.out.println("Starting individual community metrics...");
        Map<String, Supplier<IndividualCommunityMetric<Long>>> indivCommMetrics = new HashMap<>();
        IndividualCommunityMetricSelector<Long> indivCommSelector = new IndividualCommunityMetricSelector<>();
        gridReader.getMetrics(MetricTypeIdentifiers.INDIV_COMM_METRIC).forEach(metric ->
        {
            Map<String,Supplier<IndividualCommunityMetric<Long>>> map = indivCommSelector.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.INDIV_COMM_METRIC));
            indivCommMetrics.putAll(map);
        });
        System.out.println("Identified " + indivCommMetrics.size() + " individual community metrics");

        // Global community metrics
        System.out.println("Starting individual community metrics...");
        Map<String, Supplier<CommunityMetric<Long>>> globalCommMetrics = new HashMap<>();
        GlobalCommunityMetricSelector<Long> globalCommSelector = new GlobalCommunityMetricSelector<>();
        gridReader.getMetrics(MetricTypeIdentifiers.GLOBAL_COMM_METRIC).forEach(metric ->
        {
            Map<String,Supplier<CommunityMetric<Long>>> map = globalCommSelector.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.GLOBAL_COMM_METRIC));
            globalCommMetrics.putAll(map);
        });
        System.out.println("Identified " + globalCommMetrics.size() + " global community metrics");

        // Graph metrics
        System.out.println("Starting individual community metrics...");
        Map<String, GraphMetricFunction<Long>> graphMetrics = new HashMap<>();
        GraphMetricSelector<Long> graphMetricSelector = new GraphMetricSelector<>();
        gridReader.getMetrics(MetricTypeIdentifiers.GRAPH_METRIC).forEach(metric ->
        {
            Map<String,GraphMetricFunction<Long>> map = graphMetricSelector.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.GRAPH_METRIC));
            graphMetrics.putAll(map);
        });
        System.out.println("Identified " + graphMetrics.size() + " global graph metrics");

        File recFolder = new File(recRoute);
        if(!recFolder.exists() || !recFolder.isDirectory())
        {
            System.err.println("Nothing to evaluate!");
            return;
        }
        String[] recommenders = recFolder.list();
        
        // Configure the graph cloner
        GraphGenerator<Long> generator = new GraphCloneGenerator<>();
        RecommendationFormat<Long, Long> format = new SimpleRecommendationFormat<>(Parsers.lp, Parsers.lp);
        generator.configure(graph);

        System.out.println("\n");
        // Compute and write the values of the metrics
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile))))
        {
            // Map for storing the metric values.
            Map<String, Map<String, Double>> values = new ConcurrentHashMap<>();
            // Counter.
            AtomicInteger counter = new AtomicInteger(0);
            // Metric list.
            List<String> metricList = new ArrayList<>();

            assert recommenders != null;
            for(String recFile : recommenders)
            {
                try
                {
                    System.out.println("\nStarting " + recFile);

                    long aa = System.currentTimeMillis();

                    Map<String, Double> recMetrics = new HashMap<>();
                    Graph<Long> recGraph = generator.generate();
                    List<Pair<Long>> extraEdges = new ArrayList<>();

                    // Read the recommendation and add the edges
                    format.getReader(recRoute + recFile).readAll().forEach(rec ->
                    {
                        long u = rec.getUser();
                        List<Tuple2od<Long>> items = rec.getItems();
                        long maxLength = Math.min(items.size(), length);
                        for(int i = 0; i < maxLength; ++i)
                        {
                            long v = items.get(i).v1;
                            if(!onlyrel || testGraph.containsEdge(u, v))
                            {
                                recGraph.addEdge(u, v);
                                extraEdges.add(new Pair<>(u,v));
                            }
                        }
                    });

                    long bb = System.currentTimeMillis();
                    System.out.println("Algorithm " + recFile + " : finished reading (" + (bb-aa) + " ms.)" );

                    DistanceCalculator<Long> dc = new FastDistanceCalculator<>();
                    // Compute vertex metrics.
                    vertexMetrics.forEach((name, value) ->
                    {
                        VertexMetric<Long> vm = value.apply(dc);
                        double average = vm.averageValue(recGraph);
                        recMetrics.put("Average vertex " + name, average);
                    });
                    bb = System.currentTimeMillis();
                    System.out.println("Algorithm " + recFile + " : vertex metrics done (" + (bb-aa) + " ms.)" );

                    // Compute edge metrics.
                    edgeMetrics.forEach((name, value) ->
                    {
                        PairMetric<Long> em = value.apply(dc);
                        double average;
                        if(fullGraph)
                            average = em.averageValueOnlyLinks(recGraph);
                        else
                            average = em.averageValue(recGraph, extraEdges.stream(), extraEdges.size());
                        recMetrics.put("Average edge " + name, average);
                    });
                    bb = System.currentTimeMillis();
                    System.out.println("Algorithm " + recFile + " : edge metrics done (" + (bb-aa) + " ms.)" );

                    // Compute edge metrics.
                    pairMetrics.forEach((name, value) ->
                    {
                        PairMetric<Long> pm = value.apply(dc);
                        double average;
                        if(fullGraph)
                            average = pm.averageValue(recGraph);
                        else
                            average = pm.averageValue(recGraph, extraEdges.stream(), extraEdges.size());
                        recMetrics.put("Average pair " + name, average);
                    });
                    bb = System.currentTimeMillis();
                    System.out.println("Algorithm " + recFile + " : pair metrics done (" + (bb-aa) + " ms.)" );

                    // Compute indiv comm. metrics.
                    indivCommMetrics.forEach((name, value) ->
                    {
                        IndividualCommunityMetric<Long> icm = value.get();

                        communities.forEach((commName, comm) ->
                        {
                            double average = icm.averageValue(recGraph, comm);
                            recMetrics.put("Average comm " + commName + " " + name, average);
                        });
                    });
                    bb = System.currentTimeMillis();
                    System.out.println("Algorithm " + recFile + " : indiv community metrics done (" + (bb-aa) + " ms.)" );

                    // Compute global comm. metrics.
                    globalCommMetrics.forEach((name, value) ->
                    {
                        CommunityMetric<Long> gcm = value.get();
                        communities.forEach((commName, comm) ->
                        {
                            double average = gcm.compute(recGraph, comm);
                            recMetrics.put("Comm " + commName + " " + name, average);
                        });
                     });
                    bb = System.currentTimeMillis();
                    System.out.println("Algorithm " + recFile + " : global community metrics done (" + (bb-aa) + " ms.)" );

                    // Compute graph metrics.
                    graphMetrics.forEach((name, value) ->
                    {
                         GraphMetric<Long> gm = value.apply(dc);
                         double average = gm.compute(recGraph);
                         recMetrics.put("Graph " + name, average);
                    });
                    bb = System.currentTimeMillis();
                    System.out.println("Algorithm " + recFile + " : graph metrics done (" + (bb-aa) + " ms.)" );
                    values.put(recFile, recMetrics);
                    System.out.println("Algorithm " + recFile + " finished (" + counter.incrementAndGet() + " / " + recommenders.length);

                    if(metricList.isEmpty())
                    {
                        metricList.addAll(recMetrics.keySet());
                    }
                }
                catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException | IOException e)
                {
                    e.printStackTrace();
                }
            }


            bw.write("algorithm");
            for(String metric : metricList)
            {
                bw.write("\t" + metric);
            }
            bw.write("\n");

            
            // Write each metric
            for(String recFile : recommenders)
            {
                bw.write(recFile);
                Map<String, Double> metricvalues = values.get(recFile);
                for(String metric : metricList)
                {
                    bw.write("\t" + metricvalues.get(metric));
                }
                bw.write("\n");
            }
        }
        catch(IOException ioe)
        {
            System.err.println("ERROR: Something failed while computing the metrics");
        }
    }
}
