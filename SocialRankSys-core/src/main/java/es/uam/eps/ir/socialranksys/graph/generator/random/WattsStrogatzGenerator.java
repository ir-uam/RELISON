/* 
 *  Copyright (C) 2015 Information Retrieval Group at Universidad AutÃ³noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.generator.random;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.socialranksys.graph.generator.GraphGenerator;
import es.uam.eps.ir.socialranksys.utils.generator.Generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Generator for random graphs using the Watts-Strogatz model.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the vertices.
 */
public class WattsStrogatzGenerator<U> implements GraphGenerator<U>
{
    /**
     * Indicates if the graph is directed
     */
    private boolean directed;
    /**
     * Indicates the number of nodes of the generated graph.
     */
    private int numNodes;
    /**
     * Average degree of the graph.
     */
    private int meanDegree;
    /**
     * Rewiring probability
     */
    private double beta;
    /**
     * Node generator
     */
    private Generator<U> generator;
    /**
     * Indicates if the generator is configured
     */
    private boolean configured;
    /**
     * Original ring graph.
     */
    private Graph<U> ring;
    
    @Override
    @SuppressWarnings("unchecked")
    public void configure(Object... configuration) 
    {
        if(!(configuration == null) && configuration.length == 5)
        {
            boolean auxDirected = (boolean) configuration[0];
            int auxNumNodes = (int) configuration[1];
            int auxMeanDegree = (int) configuration[2];
            double auxBeta = (double) configuration[3];
            Generator<U> auxGenerator = (Generator<U>) configuration[4];
            
            this.configure(auxDirected, auxNumNodes, auxMeanDegree, auxBeta, auxGenerator);
        }
        else
        {
            configured = false;
        }
        
    }
    
    /**
     * Configures the graph generator.
     * @param directed Indicates if the graph is directed.
     * @param numNodes Number of nodes of the graph.
     * @param meanDegree Average degree of the nodes.
     * @param beta Rewiring probability (between 0 and 1).
     * @param generator Node generator.
     */
    public void configure(boolean directed, int numNodes, int meanDegree, double beta, Generator<U> generator)
    {
        try {
            this.directed = directed;
            this.numNodes = numNodes;
            this.meanDegree = meanDegree;
            this.beta = beta;
            this.generator = generator;
            this.configured = true;
            
            EmptyGraphGenerator<U> gen = new EmptyGraphGenerator<>();
            gen.configure(directed, false);
            this.ring = gen.generate();
            
            List<U> list = new ArrayList<>();
            
            IntStream.range(0, numNodes).forEach((i)->{
                U node = this.generator.generate();
                list.add(node);
                ring.addNode(node);
            });
                
            if(directed)
            {
                IntStream.range(0, numNodes).forEach((i)->{
                    U node = list.get(i);
                    IntStream.range(1, meanDegree/2).forEach((j)->{
                        int leftIdx = (i-j) % numNodes;
                        if(leftIdx < 0)
                            leftIdx += numNodes;
                        int rightIdx = (i+j) % numNodes;
                        U left = list.get(leftIdx);
                        U right = list.get(rightIdx);
                        ring.addEdge(node, right);
                        ring.addEdge(node, left); 
                    });
                });
            }
            else
            {
                IntStream.range(0, numNodes).forEach((i)->{
                U node = list.get(i);
                IntStream.range(1, meanDegree/2).forEach((j)->{
                    U right = list.get((i+j)%numNodes);
                    ring.addEdge(node, right);
                    });
                });
            }            
        } catch (GeneratorNotConfiguredException ex) {
            this.configured = false;
        }
    }
    
    @Override
    public Graph<U> generate() throws GeneratorNotConfiguredException, GeneratorBadConfiguredException
    {
        if(!configured)
            throw new GeneratorNotConfiguredException("Watts-Strogatz model: Generator was not configured");
        if(meanDegree > numNodes || meanDegree % 2 != 0)
            throw new GeneratorBadConfiguredException("Watts-Strogatz model: The mean degree cannot be greater than the number of nodes, and must be even");
        if(beta < 0 || beta > 1)
            throw new GeneratorBadConfiguredException("Watts-Strogatz model: beta must be between 0 and 1 (both limits included)");
        
        EmptyGraphGenerator<U> gen = new EmptyGraphGenerator<>();
        gen.configure(directed, false);
        Graph<U> graph = gen.generate();

        // Generating the ring ring.
        List<U> list = new ArrayList<>();
        this.ring.getAllNodes().forEach(node -> 
        {
            list.add(node);
            graph.addNode(node);
        });
        
        Random rng = new Random();
        
        // Rewire
        if(directed)
        {
            this.ring.getAllNodes().forEach((node)-> this.ring.getAdjacentNodes(node).forEach(adj ->
            {
                if(rng.nextDouble() < this.beta)
                {
                    U nextUser;
                    do
                    {
                        nextUser = list.get(rng.nextInt(numNodes));
                    }
                    while(node.equals(nextUser) || graph.containsEdge(node, nextUser));
                    graph.addEdge(node, nextUser);
                }
                else
                {
                    graph.addEdge(node, adj);
                }
            }));
        }
        else
        {
            List<U> visited = new ArrayList<>();
            ring.getAllNodes().forEach((node)->{
                ring.getAdjacentNodes(node).forEach(adj -> 
                {
                    if(!visited.equals(adj))
                    {
                        if(rng.nextDouble() < this.beta)
                        {
                            U nextUser;
                            do
                            {
                                nextUser = list.get(rng.nextInt(numNodes));
                            }
                            while(node.equals(nextUser) || graph.containsEdge(node, nextUser));
                            graph.addEdge(node, nextUser);
                        }
                        else
                        {
                            graph.addEdge(node, adj);
                        }
                    }
                });
                visited.add(node);
            });
        }
        
        return graph;
    }
}
