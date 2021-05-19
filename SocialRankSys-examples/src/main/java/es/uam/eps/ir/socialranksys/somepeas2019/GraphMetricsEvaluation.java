/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.somepeas2019;


import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.io.TextCommunitiesReader;
import es.uam.eps.ir.socialranksys.graph.Adapters;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.sna.MetricGridReader;
import es.uam.eps.ir.socialranksys.grid.sna.MetricTypeIdentifiers;
import es.uam.eps.ir.socialranksys.grid.sna.comm.global.GlobalCommunityMetricSelector;
import es.uam.eps.ir.socialranksys.grid.sna.comm.indiv.IndividualCommunityMetricSelector;
import es.uam.eps.ir.socialranksys.grid.sna.edge.EdgeMetricFunction;
import es.uam.eps.ir.socialranksys.grid.sna.edge.EdgeMetricSelector;
import es.uam.eps.ir.socialranksys.grid.sna.graph.GraphMetricFunction;
import es.uam.eps.ir.socialranksys.grid.sna.graph.GraphMetricSelector;
import es.uam.eps.ir.socialranksys.grid.sna.pair.PairMetricFunction;
import es.uam.eps.ir.socialranksys.grid.sna.pair.PairMetricSelector;
import es.uam.eps.ir.socialranksys.grid.sna.vertex.VertexMetricFunction;
import es.uam.eps.ir.socialranksys.grid.sna.vertex.VertexMetricSelector;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.metrics.*;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.FastDistanceCalculator;
import org.ranksys.formats.parsing.Parsers;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Class which analizes the different properties of a graph.
 * @author Javier Sanz-Cruzado Puig
 */
public class GraphMetricsEvaluation
{
    /**
     * Program which analyzes the different properties of a graph.
     * @param args Execution arguments
     * <ul>
     *  <li><b>Training graph file:</b> File containing the training graph to analyze</li>
     *  <li><b>Test graph file:</b> File containing the test graph</li>
     *  <li><b>Metric grid:</b> Grid file containing all the metrics we want to compute</li>
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
    public static void main(String[] args)
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
        boolean directed = args[3].equalsIgnoreCase("true");
        boolean weighted = args[4].equalsIgnoreCase("true");
        String commpath = args[5];
        String[] comms = args[6].split(",");
        List<String> commFiles = Arrays.asList(comms);
        String outputFile = args[7];

        // Read the graphs
        long a = System.currentTimeMillis();
        
        TextGraphReader<Long> greader = new TextGraphReader<>(directed, weighted, false, "\t", Parsers.lp);
        Graph<Long> auxGraph = greader.read(graphFile, weighted, false);
        FastGraph<Long> graph = (FastGraph<Long>) Adapters.removeAutoloops(auxGraph);

        // Clean the graph

        if(graph == null)
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
        MetricGridReader gridReader = new MetricGridReader(metricGrid);
        gridReader.readDocument();
        b = System.currentTimeMillis();
        System.out.println("Grid read (" + (b-a) + " ms.)");

        // Common distance calculator
        Map<String, Supplier<GraphMetric<Long>>> metrics = new HashMap<>();

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

        // Compute and write the values of the metrics
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile))))
        {
            // Map for storing the metric values.
            Map<String, Double> values = new ConcurrentHashMap<>();
            // Counter.
            AtomicInteger counter = new AtomicInteger(0);
            // Metric list.

            long aa = System.currentTimeMillis();
            DistanceCalculator<Long> dc = new FastDistanceCalculator<>();

            // Compute vertex metrics
            vertexMetrics.forEach((name, value) ->
            {
                VertexMetric<Long> vm = value.apply(dc);
                double average = vm.averageValue(graph);
                values.put("Average vertex " + name, average);
            });

            long bb = System.currentTimeMillis();
            System.out.println("Vertex metrics done (" + (bb-aa) + " ms.)" );

            // Compute edge metrics.
            edgeMetrics.forEach((name, value) ->
            {
                EdgeMetric<Long> em = value.apply(dc);
                double average = em.averageValue(graph);
                values.put("Average edge " + name, average);
            });
            bb = System.currentTimeMillis();
            System.out.println("Edge metrics done (" + (bb-aa) + " ms.)" );

            // Compute pair metrics.
            pairMetrics.forEach((name, value) ->
            {
                PairMetric<Long> pm = value.apply(dc);
                double average = pm.averageValue(graph);
                values.put("Average pair " + name, average);
            });
            bb = System.currentTimeMillis();
            System.out.println("Pair metrics done (" + (bb-aa) + " ms.)" );

                    // Compute indiv comm. metrics.
            indivCommMetrics.forEach((name, value) ->
            {
                IndividualCommunityMetric<Long> icm = value.get();

                communities.forEach((commName, comm) ->
                {
                    double average = icm.averageValue(graph, comm);
                    values.put("Average comm " + commName + " " + name, average);
                });
            });
            bb = System.currentTimeMillis();
            System.out.println("Indiv community metrics done (" + (bb-aa) + " ms.)" );

            // Compute global comm. metrics.
            globalCommMetrics.forEach((name, value) ->
            {
                CommunityMetric<Long> gcm = value.get();
                communities.forEach((commName, comm) ->
                {
                    double average = gcm.compute(graph, comm);
                    values.put("Comm " + commName + " " + name, average);
                });
             });
            bb = System.currentTimeMillis();
            System.out.println("Global community metrics done (" + (bb-aa) + " ms.)" );

            // Compute graph metrics.
            graphMetrics.forEach((name, value) ->
            {
                 GraphMetric<Long> gm = value.apply(dc);
                 double average = gm.compute(graph);
                 values.put("Graph " + name, average);
            });
            bb = System.currentTimeMillis();
            System.out.println("Graph metrics done (" + (bb-aa) + " ms.)" );

            bw.write("metric\tvalue");
            for(Map.Entry<String, Double> entry : values.entrySet())
            {
                bw.write("\n"+entry.getKey() + "\t" + entry.getValue());
            }
        }
        catch(IOException ioe)
        {
            System.err.println("ERROR: Something failed while computing the metrics");
        }
    }
}
