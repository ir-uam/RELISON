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
import es.uam.eps.ir.relison.io.DelimitedRow;
import org.ranksys.formats.parsing.Parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads edge attributes from a delimited text file into an existing graph.
 *
 * <p>The first line is a header whose first two columns are (ignored) labels for the source and target identifiers
 * and whose remaining columns declare the attributes as {@code name:type}, e.g.</p>
 * <pre>
 * source    target    since:long    kind:string
 * 1         2         2020          friend
 * </pre>
 * <p>Each subsequent line holds the endpoints of an edge followed by one value per declared attribute (empty cells
 * leave the corresponding attribute unset). Rows whose edge is not present in the graph are ignored.</p>
 *
 * @param <V> the type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class EdgeAttributeReader<V>
{
    /** Field delimiter. */
    private final String delimiter;
    /** Parser for the node identifiers. */
    private final Parser<V> uParser;
    /** Whether to automatically add edges that are not present in the graph. */
    private final boolean addEdges;
    /** Whether the first row declares attribute names and types. */
    private final boolean header;

    /**
     * Constructor.
     * @param delimiter the field delimiter.
     * @param uParser   the parser for the node identifiers.
     */
    /**
     * Constructor that does not add missing edges (default behaviour).
     * @param delimiter field delimiter.
     * @param uParser parser for the node identifiers.
     */
    public EdgeAttributeReader(String delimiter, Parser<V> uParser)
    {
        this(delimiter, uParser, false);
    }

    /**
     * Constructor allowing the caller to decide whether missing edges should be added to the graph.
     * @param delimiter field delimiter.
     * @param uParser parser for the node identifiers.
     * @param addEdges if {@code true}, edges that are not already present will be added before setting their attributes.
     */
    public EdgeAttributeReader(String delimiter, Parser<V> uParser, boolean addEdges)
    {
        this(delimiter, uParser, addEdges, true);
    }

    /** Constructor allowing the caller to select whether the first row declares the schema. */
    public EdgeAttributeReader(String delimiter, Parser<V> uParser, boolean addEdges, boolean header)
    {
        this.delimiter = delimiter;
        this.uParser = uParser;
        this.addEdges = addEdges;
        this.header = header;
    }

    /**
     * Reads edge attributes from a file.
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
     * Reads edge attributes from an input stream.
     * @param graph  the graph to populate.
     * @param stream the input stream to read from.
     * @return true if everything went OK, false otherwise.
     */
    public boolean read(Graph<V> graph, InputStream stream)
    {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)))
        {
            String first = br.readLine();
            if (first == null) return false;

            String[] cols = DelimitedRow.parse(first, delimiter);
            String firstData = null;
            if (!header)
            {
                firstData = first;
                if (cols.length < 2) return false;
                cols[0] = "source";
                cols[1] = "target";
                for (int i = 2; i < cols.length; i++) cols[i] = "attribute" + (i - 1) + ":string";
            }
            int numAttr = cols.length - 2;
            if (numAttr < 0) return false;
            String[] names = new String[numAttr];
            AttributeType[] types = new AttributeType[numAttr];
            for (int j = 0; j < numAttr; ++j)
            {
                String spec = cols[j + 2];
                int colon = spec.indexOf(':');
                names[j] = colon < 0 ? spec.trim() : spec.substring(0, colon).trim();
                types[j] = colon < 0 ? AttributeType.STRING : AttributeType.fromToken(spec.substring(colon + 1));
                graph.defineEdgeAttribute(names[j], types[j]);
            }

            // For multigraphs, consecutive rows for the same (source, target) address its parallel edges in order.
            boolean multi = graph instanceof MultiGraph;
            Map<String, Integer> occurrences = multi ? new HashMap<>() : null;

            String line = firstData == null ? br.readLine() : firstData;
            while (line != null)
            {
                if (line.isEmpty()) { line = br.readLine(); continue; }
                String[] splits = DelimitedRow.parse(line, delimiter);
                if (splits.length < 2) { line = br.readLine(); continue; }
                V source = uParser.parse(splits[0]);
                V target = uParser.parse(splits[1]);

                boolean addedNow = false;
                if (!graph.containsEdge(source, target)) {
                    if (addEdges) {
                        // Try to add the edge (nodes will be added if needed by the graph implementation).
                        boolean added = graph.addEdge(source, target);
                        if (!added) { line = br.readLine(); continue; } // could not add edge
                        addedNow = true;
                        // Initialise occurrence count for multigraphs so that the current line is treated as the first edge.
                        if (multi) occurrences.put(splits[0] + "\t" + splits[1], 1);
                    } else {
                        line = br.readLine();
                        continue; // skip rows for non-existing edges when addEdges is false
                    }
                }

                int edgeIdx = 0;
                if (multi) {
                    if (addedNow) {
                        edgeIdx = 0; // the edge was just added, this is the first occurrence
                    } else {
                        edgeIdx = occurrences.merge(splits[0] + "\t" + splits[1], 1, Integer::sum) - 1;
                    }
                }
                for (int j = 0; j < numAttr; ++j)
                {
                    int col = j + 2;
                    if (col >= splits.length) break;
                    Object value = types[j].parse(splits[col]);
                    if (value == null) continue;
                    if (multi) ((MultiGraph<V>) graph).setEdgeAttribute(source, target, edgeIdx, names[j], value);
                    else graph.setEdgeAttribute(source, target, names[j], value);
                }
                line = br.readLine();
            }
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }
}
