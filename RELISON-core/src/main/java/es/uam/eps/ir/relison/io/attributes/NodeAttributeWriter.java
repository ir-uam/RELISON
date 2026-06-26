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

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes the node attributes of a graph to a delimited text file, in the format read back by
 * {@link NodeAttributeReader}: a {@code id} column followed by one {@code name:type} column per declared attribute,
 * and then one row per node.
 *
 * @param <V> the type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class NodeAttributeWriter<V>
{
    /** Field delimiter. */
    private final String delimiter;

    /**
     * Constructor.
     * @param delimiter the field delimiter.
     */
    public NodeAttributeWriter(String delimiter)
    {
        this.delimiter = delimiter;
    }

    /**
     * Writes the node attributes to a file.
     * @param graph the graph whose node attributes are written.
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
     * Writes the node attributes to an output stream.
     * @param graph  the graph whose node attributes are written.
     * @param stream the destination output stream.
     * @return true if everything went OK, false otherwise.
     */
    public boolean write(Graph<V> graph, OutputStream stream)
    {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(stream)))
        {
            List<String> names = graph.getNodeAttributeNames().collect(Collectors.toList());

            StringBuilder header = new StringBuilder("id");
            for (String name : names)
            {
                header.append(delimiter).append(name).append(':').append(graph.getNodeAttributeType(name).token());
            }
            bw.write(header.toString());
            bw.newLine();

            List<V> nodes = graph.getAllNodes().collect(Collectors.toList());
            for (V node : nodes)
            {
                StringBuilder row = new StringBuilder(String.valueOf(node));
                for (String name : names)
                {
                    AttributeType type = graph.getNodeAttributeType(name);
                    row.append(delimiter).append(type.format(graph.getNodeAttribute(node, name)));
                }
                bw.write(row.toString());
                bw.newLine();
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
