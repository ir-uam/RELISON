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
import org.ranksys.formats.parsing.Parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Reads node attributes from a delimited text file into an existing graph.
 *
 * <p>The first line is a header whose first column is an (ignored) label for the node identifier and whose remaining
 * columns declare the attributes as {@code name:type}, e.g.</p>
 * <pre>
 * id    age:int    country:categorical
 * 1     23         UK
 * 2     31         ES
 * </pre>
 * <p>Each subsequent line holds a node identifier followed by one value per declared attribute (empty cells leave
 * the corresponding attribute unset). Rows whose node is not present in the graph are ignored.</p>
 *
 * @param <V> the type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class NodeAttributeReader<V>
{
    /** Field delimiter. */
    private final String delimiter;
    /** Parser for the node identifiers. */
    private final Parser<V> uParser;
    /** Whether to automatically add nodes that are not present in the graph. */
    private final boolean addNodes;

    /**
     * Constructor.
     * @param delimiter the field delimiter.
     * @param uParser   the parser for the node identifiers.
     */
    /**
     * Constructor that does not add missing nodes (default behaviour).
     * @param delimiter field delimiter.
     * @param uParser parser for node identifiers.
     */
    public NodeAttributeReader(String delimiter, Parser<V> uParser)
    {
        this(delimiter, uParser, false);
    }

    /**
     * Constructor allowing the caller to decide whether missing nodes should be added to the graph.
     * @param delimiter field delimiter.
     * @param uParser parser for node identifiers.
     * @param addNodes if {@code true}, nodes that are not already present in the graph will be added before
     *                 setting their attributes.
     */
    public NodeAttributeReader(String delimiter, Parser<V> uParser, boolean addNodes)
    {
        this.delimiter = delimiter;
        this.uParser = uParser;
        this.addNodes = addNodes;
    }

    /**
     * Reads node attributes from a file.
     * @param graph the graph to populate.
     * @param file  the path of the file to read.
     * @return true if everything went OK, false otherwise.
     */
    public boolean read(Graph<V> graph, String file)
    {
        try (InputStream stream = new FileInputStream(file))
        {
            return this.read(graph, stream);
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /**
     * Reads node attributes from an input stream.
     * @param graph  the graph to populate.
     * @param stream the input stream to read from.
     * @return true if everything went OK, false otherwise.
     */
    public boolean read(Graph<V> graph, InputStream stream)
    {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream)))
        {
            String header = br.readLine();
            if (header == null) return false;

            String[] cols = header.split(delimiter, -1);
            int numAttr = cols.length - 1;
            String[] names = new String[numAttr];
            AttributeType[] types = new AttributeType[numAttr];
            for (int j = 0; j < numAttr; ++j)
            {
                String spec = cols[j + 1];
                int colon = spec.indexOf(':');
                names[j] = colon < 0 ? spec.trim() : spec.substring(0, colon).trim();
                types[j] = colon < 0 ? AttributeType.STRING : AttributeType.fromToken(spec.substring(colon + 1));
                graph.defineNodeAttribute(names[j], types[j]);
            }

            String line;
            while ((line = br.readLine()) != null)
            {
                if (line.isEmpty()) continue;
                String[] splits = line.split(delimiter, -1);
                V node = uParser.parse(splits[0]);
                if (!graph.containsVertex(node)) 
                {
                    if (addNodes) 
                    {
                        // Attempt to add the node; if addition fails, skip this line.
                        boolean added = graph.addNode(node);
                        if (!added) continue; // could not add (e.g., null or duplicate), skip attributes.
                    } else 
                    {
                        continue; // skip rows for non‑existing nodes when addNodes is false
                    }
                }
                for (int j = 0; j < numAttr; ++j)
                {
                    int col = j + 1;
                    if (col >= splits.length) break;
                    Object value = types[j].parse(splits[col]);
                    if (value != null) graph.setNodeAttribute(node, names[j], value);
                }
            }
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }
}
