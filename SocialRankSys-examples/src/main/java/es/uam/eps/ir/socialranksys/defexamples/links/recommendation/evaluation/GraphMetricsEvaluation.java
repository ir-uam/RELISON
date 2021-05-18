/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.defexamples.links.recommendation.evaluation;

import es.uam.eps.ir.socialranksys.AuxiliarMethods;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.io.TextCommunitiesReader;
import es.uam.eps.ir.socialranksys.graph.Adapters;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.graph.generator.GraphCloneGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.GraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.socialranksys.grid.metrics.MetricTypeIdentifiers;
import es.uam.eps.ir.socialranksys.grid.metrics.YAMLMetricGridReader;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.GlobalCommunityMetricSelector;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.indiv.IndividualCommunityMetricSelector;
import es.uam.eps.ir.socialranksys.grid.metrics.edge.EdgeMetricFunction;
import es.uam.eps.ir.socialranksys.grid.metrics.edge.EdgeMetricSelector;
import es.uam.eps.ir.socialranksys.grid.metrics.graph.GraphMetricFunction;
import es.uam.eps.ir.socialranksys.grid.metrics.graph.GraphMetricSelector;
import es.uam.eps.ir.socialranksys.grid.metrics.pair.PairMetricFunction;
import es.uam.eps.ir.socialranksys.grid.metrics.pair.PairMetricSelector;
import es.uam.eps.ir.socialranksys.grid.metrics.vertex.VertexMetricFunction;
import es.uam.eps.ir.socialranksys.grid.metrics.vertex.VertexMetricSelector;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.metrics.*;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.FastDistanceCalculator;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
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
     * <ul>
     *  <li><b>Training graph file:</b> File containing the training graph to analyze</li>
     *  <li><b>Test graph file:</b> File containing the test graph</li>
     *  <li><b>Metric grid:</b> YAML file containing all the metrics we want to compute</li>
     *  <li><b>Recommenders route:</b> Directory which contains the recommendations to evaluate</li>
     *  <li><b>Directed:</b> true if the graph is directed, false if not</li>
     *  <li><b>Weighted:</b> true if the graph is weighted, false if not</li>
     *  <li><b>Comm. Route</b> The path which contains the community files</li>
     *  <li><b>Comm. files:</b> A comma separated list of community files for the graph</li>
     *  <li><b>Output folder:</b> Folder for storing the different outcomes</li>
     *  <li><b>Max. Length:</b> Maximum length of the recommendations (each element below that position will be discarded)</li>
     *  <li><b>Full Graph:</b> For pairs / edge metrics, indicates if the metric restricts to the recommended links, or it is computed for each pair of nodes / edge in the network</li>
     *  <li><b>Only relevant:</b> true if we want to add only relevant edges to the graph, false if we want to add all</li>
     * </ul>
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 10)
        {
            System.err.println("Usage: <training graph file> <test graph file> <metric grid> <recroute> <directed> <weighted> <commroute> <commfiles> <output folder> <length> <fullGraph> <onlyrel>");
            return;
        }

        // Argument reading
        String graphFile = args[0];
        String testGraphFile = args[1];
        String metricGrid = args[2];
        String recRoute = args[3];
        boolean directed = args[4].equalsIgnoreCase("true");
        boolean weighted = args[5].equalsIgnoreCase("true");
        String commpath = args[6];
        String[] comms = args[7].split(",");
        List<String> commFiles = Arrays.asList(comms);
        String outputFile = args[8];
        int length = Parsers.ip.parse(args[9]);
        boolean fullGraph = args[10].equalsIgnoreCase("true");
        boolean onlyrel = args[11].equalsIgnoreCase("true");

        // Read the graphs
        long a = System.currentTimeMillis();
        
        TextGraphReader<Long> greader = new TextGraphReader<>(directed, weighted, false, "\t", Parsers.lp);
        Graph<Long> auxGraph = greader.read(graphFile, weighted, false);
        Graph<Long> auxTestGraph = greader.read(testGraphFile, weighted, onlyrel);

        // Clean the graph
        FastGraph<Long> graph = (FastGraph<Long>) Adapters.removeAutoloops(auxGraph);
        FastGraph<Long> testGraph = (FastGraph<Long>) Adapters.removeAutoloops(auxTestGraph);

        if(graph == null || testGraph == null)
        {
            return;
        }
        long b = System.currentTimeMillis();
        System.out.println("Graphs read (" + (b-a) + " ms.");


        // Read the communities
        a = System.currentTimeMillis();

        Map<String, Communities<Long>> communities = new HashMap<>();
        TextCommunitiesReader<Long> creader = new TextCommunitiesReader<>("\t", Parsers.lp);
        commFiles.forEach((comm) -> communities.put(comm, creader.read(commpath + comm)));

        b = System.currentTimeMillis();
        System.out.println("Communities read (" + (b-a) + " ms.");


        // Read the metrics to compute:
        a = System.currentTimeMillis();
        YAMLMetricGridReader gridReader = new YAMLMetricGridReader();
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
        Map<String, EdgeMetricFunction<Long>> edgeMetrics = new HashMap<>();
        EdgeMetricSelector<Long> edgeSelector = new EdgeMetricSelector<>();
        gridReader.getMetrics(MetricTypeIdentifiers.EDGE_METRIC).forEach(metric ->
        {
            Map<String,EdgeMetricFunction<Long>> map = edgeSelector.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.EDGE_METRIC));
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
                        EdgeMetric<Long> em = value.apply(dc);
                        double average;
                        if(fullGraph)
                            average = em.averageValue(recGraph);
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
