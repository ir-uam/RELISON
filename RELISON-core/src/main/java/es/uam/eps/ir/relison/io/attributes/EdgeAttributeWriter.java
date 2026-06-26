/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.io.attributes;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.attributes.AttributeType;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes the edge attributes of a graph to a delimited text file, in the format read back by
 * {@link EdgeAttributeReader}: {@code source} and {@code target} columns followed by one {@code name:type} column per
 * declared attribute. Only edges that have at least one attribute value set are written; for undirected graphs each
 * edge is written once.
 *
 * @param <V> the type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class EdgeAttributeWriter<V>
{
    /** Field delimiter. */
    private final String delimiter;

    /**
     * Constructor.
     * @param delimiter the field delimiter.
     */
    public EdgeAttributeWriter(String delimiter)
    {
        this.delimiter = delimiter;
    }

    /**
     * Writes the edge attributes to a file.
     * @param graph the graph whose edge attributes are written.
     * @param file  the path of the destination file.
     * @return true if everything went OK, false otherwise.
     */
    public boolean write(Graph<V> graph, String file)
    {
        try (OutputStream stream = new FileOutputStream(file))
        {
            return this.write(graph, stream);
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /**
     * Writes the edge attributes to an output stream.
     * @param graph  the graph whose edge attributes are written.
     * @param stream the destination output stream.
     * @return true if everything went OK, false otherwise.
     */
    public boolean write(Graph<V> graph, OutputStream stream)
    {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(stream)))
        {
            List<String> names = graph.getEdgeAttributeNames().collect(Collectors.toList());

            StringBuilder header = new StringBuilder("source").append(delimiter).append("target");
            for (String name : names)
            {
                header.append(delimiter).append(name).append(':').append(graph.getEdgeAttributeType(name).token());
            }
            bw.write(header.toString());
            bw.newLine();

            // Ordinal of each node, used to write every undirected edge exactly once.
            boolean directed = graph.isDirected();
            boolean multi = graph instanceof MultiGraph;
            List<V> nodes = graph.getAllNodes().collect(Collectors.toList());
            Map<V, Integer> ordinal = new HashMap<>();
            for (int i = 0; i < nodes.size(); ++i) ordinal.put(nodes.get(i), i);

            for (V source : nodes)
            {
                List<V> targets = graph.getAdjacentNodes(source).distinct().collect(Collectors.toList());
                for (V target : targets)
                {
                    if (!directed && ordinal.get(source) > ordinal.get(target)) continue;

                    // For multigraphs, write one row per parallel edge (in index order); one row otherwise.
                    int count = multi ? ((MultiGraph<V>) graph).getNumEdges(source, target) : 1;
                    for (int idx = 0; idx < count; ++idx)
                    {
                        StringBuilder cells = new StringBuilder();
                        boolean any = false;
                        for (String name : names)
                        {
                            AttributeType type = graph.getEdgeAttributeType(name);
                            Object value = multi
                                    ? ((MultiGraph<V>) graph).getEdgeAttribute(source, target, idx, name)
                                    : graph.getEdgeAttribute(source, target, name);
                            if (value != null) any = true;
                            cells.append(delimiter).append(type.format(value));
                        }
                        if (!any) continue;

                        bw.write(String.valueOf(source) + delimiter + target + cells);
                        bw.newLine();
                    }
                }
            }
            bw.flush();
            return true;
        }
        catch (UnsupportedOperationException | IOException e)
        {
            return false;
        }
    }
}
