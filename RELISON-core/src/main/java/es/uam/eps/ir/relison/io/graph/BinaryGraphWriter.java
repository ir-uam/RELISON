/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad AutÃ³noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.io.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Writes a graph into a binary file.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class BinaryGraphWriter implements GraphWriter<Long>
{

    @Override
    public boolean write(Graph<Long> graph, String file)
    {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file))))
        {
            return this.write(graph, dos);
        }
        catch (IOException ioe)
        {
            return false;
        }
    }

    @Override
    public boolean write(Graph<Long> graph, OutputStream file)
    {
        if (graph == null || file == null)
        {
            return false;
        }

        return this.write(graph, file, true, false);
    }

    @Override
    public boolean write(Graph<Long> graph, String file, boolean writeWeights, boolean writeTypes)
    {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file))))
        {
            return this.write(graph, dos, writeWeights, writeTypes);
        }
        catch (IOException ioe)
        {
            return false;
        }
    }

    @Override
    public boolean write(Graph<Long> graph, OutputStream file, boolean writeWeights, boolean writeTypes)
    {
        if (graph == null || file == null)
        {
            return false;
        }

        try
        {
            if (graph.isMultigraph())
            {
                return writeMulti((MultiGraph<Long>) graph, file, writeWeights, writeTypes);
            }
            else
            {
                return writeSimple(graph, file, writeWeights, writeTypes);
            }
        }
        catch (IOException ioe)
        {
            return false;
        }
    }

    /**
     * Writes a multigraph.
     *
     * @param graph        the multigraph to write
     * @param file         the output stream where to write the graph.
     * @param writeWeights true if weights have to be written.
     * @param writeTypes   true if types have to be written.
     *
     * @return true if everything went ok, false if not.
     *
     * @throws IOException if something fails while writing
     */
    private boolean writeMulti(MultiGraph<Long> graph, OutputStream file, boolean writeWeights, boolean writeTypes) throws IOException
    {
        DataOutputStream dos = (DataOutputStream) file;

        dos.writeBoolean(true);
        dos.writeBoolean(graph.isDirected());
        dos.writeBoolean(graph.isWeighted());
        dos.writeLong(graph.getVertexCount());

        if (graph.isDirected())
        {
            List<Long> nodes = graph.getAllNodes().collect(Collectors.toCollection(ArrayList::new));
            for (Long u : nodes)
            {
                List<Long> adjacents = graph.getAdjacentNodes(u).collect(Collectors.toCollection(ArrayList::new));
                dos.writeLong(u);
                dos.writeInt(adjacents.size());
                for (Long v : adjacents)
                {
                    dos.writeLong(v);
                    int numEdges = graph.getNumEdges(u, v);
                    dos.writeInt(numEdges);
                    List<Double> weights = graph.getEdgeWeights(u, v);
                    List<Integer> types = graph.getEdgeTypes(u, v);

                    for (int i = 0; i < numEdges; ++i)
                    {
                        if (writeWeights)
                        {
                            dos.writeDouble(weights.get(i));
                        }
                        if (writeTypes)
                        {
                            dos.writeInt(types.get(i));
                        }
                    }
                }
            }

        }
        else
        {
            Set<Long> visited = new HashSet<>();
            List<Long> nodes = graph.getAllNodes().collect(Collectors.toCollection(ArrayList::new));
            for (Long u : nodes)
            {
                List<Long> adjacents = graph.getAdjacentNodes(u).filter(v -> !visited.contains(v)).collect(Collectors.toCollection(ArrayList::new));
                dos.writeLong(u);
                dos.writeInt(adjacents.size());
                for (Long v : adjacents)
                {
                    dos.writeLong(v);
                    int numEdges = graph.getNumEdges(u, v);
                    dos.writeInt(numEdges);
                    List<Double> weights = graph.getEdgeWeights(u, v);
                    List<Integer> types = graph.getEdgeTypes(u, v);

                    for (int i = 0; i < numEdges; ++i)
                    {
                        if (writeWeights)
                        {
                            dos.writeDouble(weights.get(i));
                        }
                        if (writeTypes)
                        {
                            dos.writeInt(types.get(i));
                        }
                    }
                }
                visited.add(u);
            }
        }

        return true;
    }

    /**
     * Writes a simple graph (without multiedges)
     *
     * @param graph        the graph
     * @param file         the output stream where to write the graph
     * @param writeWeights true if weights have to be written
     * @param writeTypes   true if types have to be written
     *
     * @return true if everything went ok, false if not
     *
     * @throws IOException if something fails while writing.
     */
    private boolean writeSimple(Graph<Long> graph, OutputStream file, boolean writeWeights, boolean writeTypes) throws IOException
    {
        DataOutputStream dos = (DataOutputStream) file;

        dos.writeBoolean(false);
        dos.writeBoolean(graph.isDirected());
        dos.writeBoolean(graph.isWeighted());
        dos.writeLong(graph.getVertexCount());

        if (graph.isDirected())
        {
            List<Long> nodes = graph.getAllNodes().collect(Collectors.toCollection(ArrayList::new));
            for (Long u : nodes)
            {
                List<Long> adjacents = graph.getAdjacentNodes(u).collect(Collectors.toCollection(ArrayList::new));
                dos.writeLong(u);
                dos.writeInt(adjacents.size());
                for (Long v : adjacents)
                {
                    dos.writeLong(v);
                    if (writeWeights)
                    {
                        dos.writeDouble(graph.getEdgeWeight(u, v));
                    }
                    if (writeTypes)
                    {
                        dos.writeInt(graph.getEdgeType(u, v));
                    }
                }
            }
        }
        else
        {
            Set<Long> visited = new HashSet<>();
            List<Long> nodes = graph.getAllNodes().collect(Collectors.toCollection(ArrayList::new));
            for (Long u : nodes)
            {
                List<Long> adjacents = graph.getAdjacentNodes(u).filter(v -> !visited.contains(v)).collect(Collectors.toCollection(ArrayList::new));
                dos.writeLong(u);
                dos.writeInt(adjacents.size());
                for (Long v : adjacents)
                {
                    dos.writeLong(v);
                    if (writeWeights)
                    {
                        dos.writeDouble(graph.getEdgeWeight(u, v));
                    }
                    if (writeTypes)
                    {
                        dos.writeInt(graph.getEdgeType(u, v));
                    }
                }

                visited.add(u);
            }
        }
        return true;
    }

}
