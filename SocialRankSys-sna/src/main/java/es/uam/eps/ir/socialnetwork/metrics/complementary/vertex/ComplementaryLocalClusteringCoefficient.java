/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.complementary.vertex;

import es.uam.eps.ir.socialnetwork.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes the local clustering coefficient of a node in the complementary graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class ComplementaryLocalClusteringCoefficient<U> implements VertexMetric<U>
{   
    /**
     * Selection of the direction of the edge from the studied node to the first node in the triads
     */
    private final EdgeOrientation vSel;
    /**
     * Selection of the direction of the edge from the studied node to the last node in the triads
     */    private final EdgeOrientation wSel;
    
    /**
     * Number of autoloops in the graph.
     */
    private long autoloops = 0L;
    /**
     * The last graph.
     */
    private Graph<U> lastGraph;
    
    /**
     * The degrees of each node.
     */
    private Map<EdgeOrientation, Map<U, Double>> degrees = new HashMap<>();
    /**
     * The degrees of each node without autoloops
     */
    private Map<EdgeOrientation, Map<U, Double>> degreesNoAutoloops = new HashMap<>();
    
    /**
     * Default constructor. Relates the incoming neighbourhood and the outgoing one.
     */
    public ComplementaryLocalClusteringCoefficient()
    {
        this(EdgeOrientation.IN, EdgeOrientation.OUT);
    }
    
    /**
     * Constructor.
     * @param vSel Selection of the direction of the first edges.
     * @param wSel Selection of the direction of the second edges.
     */
    public ComplementaryLocalClusteringCoefficient(EdgeOrientation vSel, EdgeOrientation wSel)
    {
        this.lastGraph = null;
        this.vSel = vSel;
        this.wSel = wSel;
    }
    
    
    
    @Override
    public double compute(Graph<U> graph, U user) 
    {
        if(this.lastGraph == null || !this.lastGraph.equals(graph))
        {
            this.initialize(graph);
        }
        
        double value;
        double denom;


        Set<U> vSelSet = graph.getNeighbourhood(user, this.vSel).collect(Collectors.toCollection(HashSet::new));
        Set<U> wSelSet = graph.getNeighbourhood(user, this.wSel).collect(Collectors.toCollection(HashSet::new));
 
        // Count the number of triangles in the graph
        long triangles = vSelSet.stream().mapToLong(v -> wSelSet.stream().filter(w -> !v.equals(w)).filter(w -> graph.containsEdge(v, w)).count()).sum();
        
        long numUsers = graph.getVertexCount();
        long numEdges = ((graph.isDirected()) ? 1 : 2)*(graph.getEdgeCount() - this.autoloops);
        
        
        double vSum = vSelSet.stream().mapToDouble(v -> this.degreesNoAutoloops.get(vSel.invertSelection()).get(v)).sum();
        double wSum = wSelSet.stream().mapToDouble(w -> this.degreesNoAutoloops.get(wSel.invertSelection()).get(w)).sum();
        double uDegreeV = this.degrees.get(vSel).get(user);
        double uDegreeW = this.degrees.get(wSel).get(user);
        
        vSelSet.retainAll(wSelSet);
        int intersize = vSelSet.size();
        
        double origDenom = uDegreeV*uDegreeW - intersize + 0.0;

        /* Numerator */
        value = numUsers*(numUsers - 1);
        value -= (numEdges + (numUsers-1)*(uDegreeV + uDegreeW));
        value += vSum + wSum + origDenom;
        value -= triangles;
        
        denom = (numUsers - uDegreeV)*(numUsers - uDegreeW) + uDegreeV + uDegreeW - intersize - numUsers;
        
        if(denom == 0.0)
            return 0.0;
        return value/denom;
                    
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph) 
    {
        if(this.lastGraph == null || !this.lastGraph.equals(graph))
        {
            this.initialize(graph);
        }
        
        Map<U, Double> result = new HashMap<>();
        graph.getAllNodes().forEach(u -> result.put(u,this.compute(graph, u)));
        return result;
    }
    
    /**
     * Initializes all the necessary data to compute the local clustering
     * coefficient in the complementary graph.
     * @param graph the graph.
     */
    private void initialize(Graph<U> graph)
    {
        this.lastGraph = graph;
        this.autoloops = 0;

        // Compute the degrees and degrees without autoloops.
        this.degrees = new HashMap<>();
        this.degrees.put(EdgeOrientation.IN, new HashMap<>());
        this.degrees.put(EdgeOrientation.OUT, new HashMap<>());
        this.degrees.put(EdgeOrientation.UND, new HashMap<>());

        this.degreesNoAutoloops = new HashMap<>();
        this.degreesNoAutoloops.put(EdgeOrientation.IN, new HashMap<>());
        this.degreesNoAutoloops.put(EdgeOrientation.OUT, new HashMap<>());
        this.degreesNoAutoloops.put(EdgeOrientation.UND, new HashMap<>());

        // Compute all the degrees for each user in the network (in order to simplify)
        graph.getAllNodes().forEach(u -> 
        {
            boolean auto = graph.containsEdge(u, u);
            double inDegree = graph.getIncidentNodesCount(u);
            double inDegreeNoAutoloop = (auto ? inDegree-1 : inDegree);
            double outDegree = graph.getAdjacentNodesCount(u);
            double outDegreeNoAutoloop = (auto ? outDegree-1 : outDegree);

            if(auto) autoloops++;
            // Degrees
            this.degrees.get(EdgeOrientation.IN).put(u, inDegree);
            this.degrees.get(EdgeOrientation.OUT).put(u, outDegree);
            this.degrees.get(EdgeOrientation.UND).put(u, inDegree+outDegree);
            // Degrees without autoloops
            this.degreesNoAutoloops.get(EdgeOrientation.IN).put(u, inDegreeNoAutoloop);
            this.degreesNoAutoloops.get(EdgeOrientation.OUT).put(u, outDegreeNoAutoloop);
            this.degreesNoAutoloops.get(EdgeOrientation.UND).put(u, inDegreeNoAutoloop + outDegreeNoAutoloop);
        });
    }

    @Override
    public double averageValue(Graph<U> graph) 
    {
        double sum = graph.getAllNodes().map(u -> this.compute(graph, u)).reduce(0.0, Double::sum);
        if(graph.getVertexCount() > 0)
            sum = sum / (graph.getVertexCount()+0.0);
        return sum;
        
    }
}
