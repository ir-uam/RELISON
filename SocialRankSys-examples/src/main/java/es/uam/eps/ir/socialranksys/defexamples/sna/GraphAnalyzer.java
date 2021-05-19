/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.defexamples.sna;

import es.uam.eps.ir.socialranksys.AuxiliarMethods;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.community.detection.connectedness.StronglyConnectedComponents;
import es.uam.eps.ir.socialranksys.community.detection.connectedness.WeaklyConnectedComponents;
import es.uam.eps.ir.socialranksys.community.io.CommunitiesReader;
import es.uam.eps.ir.socialranksys.community.io.TextCommunitiesReader;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.grid.sna.MetricTypeIdentifiers;
import es.uam.eps.ir.socialranksys.grid.sna.YAMLMetricGridReader;
import es.uam.eps.ir.socialranksys.grid.sna.comm.global.GlobalCommunityMetricSelector;
import es.uam.eps.ir.socialranksys.grid.sna.comm.indiv.IndividualCommunityMetricSelector;
import es.uam.eps.ir.socialranksys.grid.sna.edge.EdgeMetricSelector;
import es.uam.eps.ir.socialranksys.grid.sna.graph.GraphMetricSelector;
import es.uam.eps.ir.socialranksys.grid.sna.pair.PairMetricSelector;
import es.uam.eps.ir.socialranksys.grid.sna.vertex.VertexMetricSelector;
import es.uam.eps.ir.socialranksys.io.graph.GraphReader;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.io.graph.TextMultiGraphReader;
import es.uam.eps.ir.socialranksys.metrics.*;
import es.uam.eps.ir.socialranksys.metrics.distance.CompleteDistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import org.ranksys.formats.parsing.Parsers;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.examples.AuxiliarVariables.TRUE;

/**
 * Program that given a network (and, if available, some community partitions), computes several properties of the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class GraphAnalyzer 
{
    /**
     * Program which analyzes the different properties of a graph.
     * @param args Execution arguments
     *             <ol>
     *                  <li><b>Graph file:</b> File containing the graph to analyze</li>
     *                  <li><b>Metric grid:</b> Grid file containing all the metrics we want to compute</li>
     *                  <li><b>Multigraph:</b> true if the graph is a multigraph, false if not</li>
     *                  <li><b>Directed:</b> true if the graph is directed, false if not</li>
     *                  <li><b>Weighted:</b> true if the graph is directed, false if not</li>
     *                  <li><b>Comm. Route</b> The path which contains the community files</li>
     *                  <li><b>Comm. files:</b> A comma separated list of community files for the graph</li>
     *                  <li><b>Output folder:</b> Folder for storing the different outcomes</li>
     *                  <li><b>Optional:</b>
     *                      <ul>
     *                          <li>-communities commFile1,commFile2... : a comma separated list of the files containing communities.</li>
     *                          <li>--distances: include if we want to precompute distance metrics (recommended if any is used)</li>
     *                      </ul>
     *                  </li>
     *             </ol>
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 7) // parameter checking
        {
            System.err.println("ERROR: Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tGraph file: a file containing the graph to analyze.");
            System.err.println("\tMultigraph: true if the network allows multiple edges, false otherwise.");
            System.err.println("\tDirected: true if the network is directed, false otherwise.");
            System.err.println("\tWeighted: true if the network is weighted, false otherwise.");
            System.err.println("\tSelfloops: true if the network allows self-loops, false otherwise.");
            System.err.println("\tMetrics: a YAML file containing the metrics to compute.");
            System.err.println("\tOutput: the directory in which to store the metrics.");
            System.err.println();
            System.err.println("\tOptional parameters:");
            System.err.println("\t\t-communities commFile1,...,commFileN: a comma separated list of files containing communities.");
            System.err.println("\t\t--distances: include if we want to precompute distance metrics (recommended if any is used)");
            return;
        }

        // Argument reading:
        String graphFile = args[0];

        boolean multigraph = args[1].equalsIgnoreCase(TRUE);
        boolean directed = args[2].equalsIgnoreCase(TRUE);
        boolean weighted = args[3].equalsIgnoreCase(TRUE);
        boolean selfloops = args[4].equalsIgnoreCase(TRUE);

        String metricGrid = args[5];
        String output = args[6];

        List<String> comms = new ArrayList<>();
        boolean precomputeDistances = false;

        // Optional arguments:
        for(int i = 7; i < args.length; ++i)
        {
            if(args[i].equalsIgnoreCase("-communities"))
            {
                String commFiles = args[++i];
                String[] commNames = commFiles.split(",");
                comms.addAll(Arrays.asList(commNames));
            }
            else if(args[i].equalsIgnoreCase("--distances"))
            {
                precomputeDistances = true;
            }
        }

        // We first read the network to analyze.
        long a = System.currentTimeMillis();
        GraphReader<Long> greader;
        if(multigraph)
        {
            greader = new TextMultiGraphReader<>(directed, weighted, selfloops, "\t", Parsers.lp);
        }
        else
        {
            greader = new TextGraphReader<>(directed, weighted, selfloops, "\t", Parsers.lp);
        }
        Graph<Long> graph = greader.read(graphFile, weighted, false);
        long b = System.currentTimeMillis();
        System.out.println("Network read (" + (b-a) + " ms.)");

        // Then, we read the set of communities to analyze:
        // As a first step, we compute the strongly and weakly connected components of the network.
        a = System.currentTimeMillis();
        Map<String, Communities<Long>> communities = new HashMap<>();

        CommunityDetectionAlgorithm<Long> wccAlg = new WeaklyConnectedComponents<>();
        Communities<Long> wcc = wccAlg.detectCommunities(graph);
        CommunityDetectionAlgorithm<Long> sccAlg = new StronglyConnectedComponents<>();
        Communities<Long> scc = sccAlg.detectCommunities(graph);

        communities.put("wcc", wcc);
        communities.put("scc", scc);

        // Then, we read the rest of community partitions (if any):
        for(String comm : comms)
        {
            File f = new File(comm);
            String name = f.getName();
            String route = f.getAbsolutePath();

            CommunitiesReader<Long> creader = new TextCommunitiesReader<>("\t", Parsers.lp);
            Communities<Long> partition = creader.read(route);
            communities.put(name, partition);
        }

        b = System.currentTimeMillis();
        System.out.println("Communities read (" + (b-a) + " ms.");

        // Now, we read the set of metrics we do have to execute:
        a = System.currentTimeMillis();
        YAMLMetricGridReader gridReader = new YAMLMetricGridReader();
        Map<String, Object> map = AuxiliarMethods.readYAML(metricGrid);
        gridReader.read(map);
        b = System.currentTimeMillis();
        System.out.println("Grid read (" + (b-a) + " ms.)");

        // Executing the metrics:

        // We do create a map for storing the average values of the different metrics.
        Map<String, Double> metricvalues = new HashMap<>();

        // We do initialize a common distance calculator for the metrics.
        DistanceCalculator<Long> dc = new CompleteDistanceCalculator<>();
        if(precomputeDistances) // If we do indicate to precompute the distances...
        {
            dc.computeDistances(graph);
        }

        // Then, we execute the different families of metrics.

        // Step 1: vertex (or node) metrics
        String type = MetricTypeIdentifiers.VERTEX_METRIC;
        System.out.println("Starting vertex metrics...");
        a = System.currentTimeMillis();

        VertexMetricSelector<Long> vertexSel = new VertexMetricSelector<>();

        Set<String> metricsSet = gridReader.getMetrics(type);
        Map<String, Supplier<VertexMetric<Long>>> vertexMetrics = new HashMap<>();
        metricsSet.forEach(metric -> vertexMetrics.putAll(vertexSel.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.VERTEX_METRIC), dc)));
        System.out.println("Identified " + vertexMetrics.size() + " metrics");

        // Create the individual folder for the vertex metrics.
        if(vertexMetrics.size() > 0)
        {
            File file = new File(output + type);
            if(!file.exists())
            {
                boolean mkdirs = file.mkdirs();
                if(!mkdirs)
                {
                    System.err.println("ERROR: Something failed while creating a folder for the vertex metrics");
                }
            }
        }
        
        // Compute each individual metric.
        vertexMetrics.forEach((metric, value) ->
        {
            System.out.println("Running " + metric);
            Long a2 = System.currentTimeMillis();

            VertexMetric<Long> vm = value.get();

            // Compute the individual values
            Map<Long, Double> values = vm.compute(graph);
            Long b2 = System.currentTimeMillis();
            System.out.println("Computed " + metric + " (" + (b2 - a2) + " ms.)");

            // Compute the average values for the metric
            OptionalDouble opt = values.values().stream().mapToDouble(val -> val).average();
            double average = opt.orElse(0.0);
            metricvalues.put("Average " + metric, average);
            b2 = System.currentTimeMillis();
            System.out.println("Computed average " + metric + " (" + (b2 - a2) + " ms.");

            // Write the metric to a file
            GraphAnalyzer.printIndividualMetric(output + MetricTypeIdentifiers.VERTEX_METRIC + "/" + metric + ".txt", values);
            b2 = System.currentTimeMillis();
            System.out.println("Metric " + metric + " done (" + (b2 - a2) + " ms.)");
        });
        b = System.currentTimeMillis();
        System.out.println("Vertex metrics done (" + (b-a) + " ms.)");

        // Step 2: edge (or link) metrics.
        type = MetricTypeIdentifiers.EDGE_METRIC;
        System.out.println("Starting edge metrics...");
        a = System.currentTimeMillis();

        EdgeMetricSelector<Long> edgeSel = new EdgeMetricSelector<>();

        Map<String, Supplier<EdgeMetric<Long>>> edgeMetrics = new HashMap<>();
        metricsSet = gridReader.getMetrics(type);
        metricsSet.forEach(metric -> edgeMetrics.putAll(edgeSel.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.EDGE_METRIC), dc)));
        System.out.println("Identified " + edgeMetrics.size() + " metrics");
        
        // Create the individual folder for the edge metrics.
        if(edgeMetrics.size() > 0)
        {
            File file = new File(output + type);
            if(!file.exists())
            {
                boolean mkdirs = file.mkdirs();
                if(!mkdirs)
                {
                    System.err.println("ERROR: Something failed while creating a folder for the edge metrics");
                }
            }
        }
        
        // Compute each individual metric.
        edgeMetrics.forEach((metric, value) ->
        {
            System.out.println("Running " + metric);
            Long a2 = System.currentTimeMillis();

            EdgeMetric<Long> em = value.get();

            // Compute the individual values
            Map<Pair<Long>, Double> values = em.compute(graph);
            Long b2 = System.currentTimeMillis();
            System.out.println("Computed " + metric + " (" + (b2 - a2) + " ms.)");

            // Compute the average values for the metric
            double average = values.values().stream().mapToDouble(val -> val).average().orElse(0.0);
            metricvalues.put("Average " + metric, average);
            b2 = System.currentTimeMillis();
            System.out.println("Computed average " + metric + " (" + (b2 - a2) + " ms.");

            // Write the metric to a file
            GraphAnalyzer.printPairMetric(output + MetricTypeIdentifiers.EDGE_METRIC + "/" + metric + ".txt", values);
            b2 = System.currentTimeMillis();
            System.out.println("Metric " + metric + " done (" + (b2 - a2) + " ms.)");
        });
        b = System.currentTimeMillis();
        System.out.println("Edge metrics done (" + (b-a) + " ms.)");

        // Step 3: pair metrics.
        type = MetricTypeIdentifiers.PAIR_METRIC;
        System.out.println("Starting pair metrics...");
        a = System.currentTimeMillis();

        PairMetricSelector<Long> pairSel = new PairMetricSelector<>();

        Map<String, Supplier<PairMetric<Long>>> pairMetrics = new HashMap<>();
        metricsSet = gridReader.getMetrics(type);
        metricsSet.forEach(metric -> pairMetrics.putAll(pairSel.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.PAIR_METRIC), dc)));
        System.out.println("Identified " + pairMetrics.size() + " metrics");
        
        // Create the individual folder for the pair metrics.
        if(pairMetrics.size() > 0)
        {
            File file = new File(output + type);
            if(!file.exists())
            {
                boolean mkdirs = file.mkdirs();
                if(!mkdirs)
                {
                    System.err.println("ERROR: Something failed while creating a folder for the pair metrics");
                }
            }
        }
        
        // Compute each individual metric.
        pairMetrics.forEach((metric, value) ->
        {
            System.out.println("Running " + metric);
            Long a2 = System.currentTimeMillis();
            PairMetric<Long> pm = value.get();

            // Compute the individual values
            Map<Pair<Long>, Double> values = pm.compute(graph);
            Long b2 = System.currentTimeMillis();
            System.out.println("Computed " + metric + " (" + (b2 - a2) + " ms.)");

            // Compute the average values for the metric
            double average = values.values().stream().mapToDouble(val -> val).average().orElse(0.0);
            metricvalues.put("Average " + metric, average);
            b2 = System.currentTimeMillis();
            System.out.println("Computed average " + metric + " (" + (b2 - a2) + " ms.");

            // Write the metric to a file
            GraphAnalyzer.printPairMetric(output + MetricTypeIdentifiers.PAIR_METRIC + "/" + metric + ".txt", values);
            b2 = System.currentTimeMillis();
            System.out.println("Metric " + metric + " done (" + (b2 - a2) + " ms.)");
        });

        b = System.currentTimeMillis();
        System.out.println("Pair metrics done (" + (b-a) + " ms.)");

        // Step 4: individual community metrics.
        type = MetricTypeIdentifiers.INDIV_COMM_METRIC;
        System.out.println("Starting individual community metrics...");
        a = System.currentTimeMillis();

        IndividualCommunityMetricSelector<Long> indivSel = new IndividualCommunityMetricSelector<>();

        Map<String, Supplier<IndividualCommunityMetric<Long>>> indivcommMetrics = new HashMap<>();
        metricsSet = gridReader.getMetrics(type);
        metricsSet.forEach(metric -> indivcommMetrics.putAll(indivSel.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.INDIV_COMM_METRIC))));
        System.out.println("Identified " + indivcommMetrics.size() + " metrics");
        
        // Create the individual folder for the individual community metrics.
        if(indivcommMetrics.size() > 0)
        {
            File file = new File(output + type);
            if(!file.exists())
            {
                boolean mkdirs = file.mkdirs();
                if(!mkdirs)
                {
                    System.err.println("ERROR: Something failed while creating a folder for the individual community metrics");
                }
            }
        }
        
        // Compute each individual metric.
        indivcommMetrics.forEach((metric, value) ->
        {
            System.out.println("Running " + metric);
            Long a2 = System.currentTimeMillis();
            IndividualCommunityMetric<Long> icm = value.get();

            // Compute the metric values for each community detection algorithm
            communities.forEach((key, value1) ->
            {
                // Compute the metric
                Map<Integer, Double> values = icm.compute(graph, value1);
                Long b2 = System.currentTimeMillis();
                System.out.println("Computed " + metric + " for communities " + key + " (" + (b2 - a2) + " ms.)");

                // Find the average values.
                double average = values.values().stream().mapToDouble(val -> val).average().orElse(0.0);
                metricvalues.put("Average " + metric + " " + key, average);
                b2 = System.currentTimeMillis();
                System.out.println("Computed average " + metric + " for community " + key + " (" + (b2 - a2) + " ms.)");

                // Print the values for the metric.
                GraphAnalyzer.printIndividualCommMetric(output + MetricTypeIdentifiers.INDIV_COMM_METRIC + "/" + metric + "_" + key, values);
                b2 = System.currentTimeMillis();
                System.out.println("Metric " + metric + " for community " + key + " done (" + (b2 - a2) + " ms.)");
            });

            Long b3 = System.currentTimeMillis();
            System.out.println("Metric  " + metric + " done (" + (b3 - a2) + " ms.)");
        });
        b = System.currentTimeMillis();
        System.out.println("Individual community metrics done (" + (b-a) + " ms.)");


        // Step 5: global community metrics.
        type = MetricTypeIdentifiers.GLOBAL_COMM_METRIC;
        System.out.println("Starting global community metrics...");
        a = System.currentTimeMillis();

        GlobalCommunityMetricSelector<Long> globalSel = new GlobalCommunityMetricSelector<>();

        Map<String, Supplier<CommunityMetric<Long>>> globalCommMetrics = new HashMap<>();
        metricsSet = gridReader.getMetrics(type);
        metricsSet.forEach(metric -> globalCommMetrics.putAll(globalSel.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.GLOBAL_COMM_METRIC))));
        System.out.println("Identified " + globalCommMetrics.size() + " metrics");

        // Compute each individual metric
        globalCommMetrics.forEach((metric, value1) ->
        {
            System.out.println("Running " + metric);
            Long a2 = System.currentTimeMillis();

            CommunityMetric<Long> gcm = value1.get();

            // Compute the metric for each community detection algorithm
            communities.forEach((key, value2) ->
            {
                double value = gcm.compute(graph, value2);
                metricvalues.put(metric + "_" + key, value);
                Long b2 = System.currentTimeMillis();

                System.out.println("Metric " + metric + " for community " + key + " done (" + (b2 - a2) + " ms.)");
            });
            Long b3 = System.currentTimeMillis();
            System.out.println("Metric " + metric + " done (" + (b3 - a2) + " ms.)");
        });
        b = System.currentTimeMillis();
        System.out.println("Global community metrics done (" + (b-a) + " ms.)");

        // Step 6 (and final): global graph metrics.
        type = MetricTypeIdentifiers.GRAPH_METRIC;
        System.out.println("Starting graph metrics...");
        a = System.currentTimeMillis();

        GraphMetricSelector<Long> graphSel = new GraphMetricSelector<>();

        Map<String, Supplier<GraphMetric<Long>>> graphMetrics = new HashMap<>();
        metricsSet = gridReader.getMetrics(type);
        metricsSet.forEach(metric -> graphMetrics.putAll(graphSel.getMetrics(metric, gridReader.getGrid(metric, MetricTypeIdentifiers.GRAPH_METRIC), dc)));
        System.out.println("Identified " + graphMetrics.size() + " metrics");

        graphMetrics.forEach((metric, value1) ->
        {
            System.out.println("Running " + metric);

            Long a2 = System.currentTimeMillis();
            GraphMetric<Long> gm = value1.get();
            double value = gm.compute(graph);
            metricvalues.put(metric, value);
            Long b2 = System.currentTimeMillis();

            System.out.println("Metric " + metric + " done (" + (b2 - a2) + " ms.)");
        });
        b = System.currentTimeMillis();
        System.out.println("Graph metrics done (" + (b-a) + " ms.)");
        
        GraphAnalyzer.printGlobalMetrics(output + "global.txt", metricvalues);
    }

    /**
     * Prints the different values for an individual metric in a file. Values are ordered
     * by node identifier.
     * @param file      the route of the file.
     * @param values    the values of the vertex metric.
     */
    static void printIndividualMetric(String file, Map<Long, Double> values) 
    {
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
        {
            bw.write("node\tmetric\n");
            List<Long> nodes = new ArrayList<>(values.keySet());
            nodes.sort(Comparator.naturalOrder());
            
            for(long node : nodes)
            {    
                bw.write(node + "\t" + values.get(node) + "\n");
            }
        }
        catch(IOException ioe)
        {
            System.err.println("ERROR: The file " + file + " could not be written");
        }
    }
    
    /**
     * Prints the different values for an individual community metric in a file. Values are ordered
     * by node identifier.
     * @param file      the route of the file.
     * @param values    the values of the individual community metric.
     */
    static void printIndividualCommMetric(String file, Map<Integer, Double> values) 
    {
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
        {
            bw.write("comm\tmetric\n");
            List<Integer> nodes = new ArrayList<>(values.keySet());
            nodes.sort(Comparator.naturalOrder());
            
            for(int node : nodes)
            {    
                bw.write(node + "\t" + values.get(node) + "\n");
            }
        }
        catch(IOException ioe)
        {
            System.err.println("ERROR: The file " + file + " could not be written");
        }
    }

    /**
     * Prints the different values for a pair/edge metric in a file. Values are ordered
     * by node identifier.
     * @param file      the route of the file.
     * @param values    the values of the vertex metric.
     */
    static void printPairMetric(String file, Map<Pair<Long>, Double> values) 
    {
        // Comparator for ordering the pair of nodes.
        Comparator<Pair<Long>> comparator = (Pair<Long> p1, Pair<Long> p2) -> 
        {
            if(Objects.equals(p1.v1(), p2.v1()))
            {
                return p1.v2().compareTo(p2.v2());
            }
            else
            {
                if(p1.v1() > p2.v1())
                    return 1;
                else
                    return -1;
            }
        };
        
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
        {
            bw.write("nodeA\tnodeB\tmetric\n");
            List<Pair<Long>> nodes = new ArrayList<>(values.keySet());
            nodes.sort(comparator);
            
            for(Pair<Long> node : nodes)
            {    
                bw.write(node.v1() + "\t" + node.v2() + "\t" + values.get(node) + "\n");
            }
        }
        catch(IOException ioe)
        {
            System.err.println("ERROR: The file " + file + " could not be written");
        }
    }

    /**
     * Prints the global metric values into a file.
     * @param file      the route to the file.
     * @param values    the metric values.
     */
    static void printGlobalMetrics(String file, Map<String, Double> values) 
    {
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
        {
            bw.write("metric\tvalue\n");
            for(String metric : values.keySet())
            {
                bw.write(metric + "\t" + values.get(metric) + "\n");
            }
        } 
        catch (IOException ex) 
        {
            System.err.println("ERROR: The file " + file + " could not be written");
        }
    }
}
