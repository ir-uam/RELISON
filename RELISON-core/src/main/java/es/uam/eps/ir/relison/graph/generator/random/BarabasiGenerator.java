/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.generator.random;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.GraphGenerator;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.relison.utils.generator.Generator;

import java.util.*;

/**
 * Generator for graphs following the Barabasi-Albert Preferential Attachment model for
 * graphs.
 *
 * <p>
 * <b>Reference:</b> A-L. Barabási, R. Albert. Emergence of Scaling in Random Networks. Science 286(5439), pp. 509-512 (1999)
 * </p>
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class BarabasiGenerator<U> implements GraphGenerator<U>
{
    /**
     * Indicates if the generator is configured or not
     */
    private boolean configured = false;
    /**
     * Indicates if the graph is directed or not
     */
    private boolean directed;
    /**
     * Number of initial nodes
     */
    private int initialNodes;
    /**
     * Number of iterations
     */
    private int numIter;
    /**
     * Number of new edges created each iteration.
     */
    private int numEdgesIter;
    /**
     * User generator.
     */
    private Generator<U> generator;

    @Override
    public void configure(Object... configuration)
    {
        if (!(configuration == null) && configuration.length == 5)
        {
            boolean auxDirected = (boolean) configuration[0];
            int auxInitialNodes = (int) configuration[1];
            int auxNumIter = (int) configuration[2];
            int auxNumEdgesIter = (int) configuration[3];
            Generator<U> auxGenerator = (Generator<U>) configuration[4];

            this.configure(auxDirected, auxInitialNodes, auxNumIter, auxNumEdgesIter, auxGenerator);
        }
        else
        {
            this.configured = false;
        }

    }

    /**
     * Configures the graph.
     *
     * @param directed     Indicates if the node is directed or not.
     * @param initialNodes Number of initial nodes of the graph.
     * @param numIter      Number of iterations of the algorithm.
     * @param numEdgesIter Number of new edges to add each iteration
     * @param generator    Object that automatically creates the indicated number of nodes.
     */
    public void configure(boolean directed, int initialNodes, int numIter, int numEdgesIter, Generator<U> generator)
    {
        this.initialNodes = initialNodes;
        this.numIter = numIter;
        this.numEdgesIter = numEdgesIter;
        this.generator = generator;
        this.directed = directed;
        this.configured = true;
    }

    /**
     * Generates the graph.
     *
     * @return A Barabasi-Albert model graph if everything OK.
     *
     * @throws GeneratorNotConfiguredException if the generator is not configured
     * @throws GeneratorBadConfiguredException if the generator is badly configured. In this case,
     *                                         if the number of edges per iteration is greater than the number of initial nodes.
     */
    @Override
    public Graph<U> generate() throws GeneratorNotConfiguredException, GeneratorBadConfiguredException
    {
        if (!configured)
        {
            throw new GeneratorNotConfiguredException("Barabási-Albert: The model was not configured");
        }
        if (numEdgesIter > initialNodes)
        {
            throw new GeneratorBadConfiguredException("Barabási-Albert: numEdgesIter must be smaller than initialNodes");
        }

        int degreeIncrease = directed ? 2 : 1;

        EmptyGraphGenerator<U> gen = new EmptyGraphGenerator<>();
        gen.configure(directed, false);
        Graph<U> graph = gen.generate();

        Random rand = new Random();
        Map<U, Integer> inDegrees = new HashMap<>();
        //Generate the initial nodes for the random graph.
        for (int i = 0; i < initialNodes; ++i)
        {
            U node = generator.generate();
            graph.addNode(node);
            inDegrees.put(node, degreeIncrease * (initialNodes - 1));
        }

        // Create the connected component of the graph
        if (directed)
        {
            graph.getAllNodes().forEach((orig) -> graph.getAllNodes().forEach((dest) ->
                                                                              {
                                                                                  if (!orig.equals(dest))
                                                                                  {
                                                                                      graph.addEdge(orig, dest);
                                                                                  }
                                                                              }));
        }
        else
        {
            List<U> visited = new ArrayList<>();
            graph.getAllNodes().forEach((orig) -> {
                graph.getAllNodes().forEach((dest) -> {
                    if (!orig.equals(dest) && !visited.contains(dest))
                    {
                        graph.addEdge(orig, dest);
                    }
                });
                visited.add(orig);
            });
        }


        for (int i = 0; i < numIter; i++)
        {
            Set<U> newLinks = new HashSet<>();
            long numEdges = graph.getEdgeCount();
            // Select the new edges
            while (newLinks.size() < numEdgesIter)
            {
                double aux = 0.0;
                double rnd = rand.nextDouble();
                for (Map.Entry<U, Integer> entry : inDegrees.entrySet())
                {
                    aux += (entry.getValue() + 0.0) / (degreeIncrease * numEdges + 0.0);
                    if (aux > rnd)
                    {
                        newLinks.add(entry.getKey());
                        break;
                    }
                }
            }


            U newNode = generator.generate();
            graph.addNode(newNode);
            inDegrees.put(newNode, numEdgesIter);

            newLinks.forEach((node) ->
                             {
                                 graph.addEdge(newNode, node);
                                 inDegrees.put(node, inDegrees.get(node) + 1);

                             });
        }

        return graph;
    }


}
