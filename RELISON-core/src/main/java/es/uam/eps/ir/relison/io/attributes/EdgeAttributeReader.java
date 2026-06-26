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

    /**
     * Constructor.
     * @param delimiter the field delimiter.
     * @param uParser   the parser for the node identifiers.
     */
    public EdgeAttributeReader(String delimiter, Parser<V> uParser)
    {
        this.delimiter = delimiter;
        this.uParser = uParser;
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
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream)))
        {
            String header = br.readLine();
            if (header == null) return false;

            String[] cols = header.split(delimiter, -1);
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

            String line;
            while ((line = br.readLine()) != null)
            {
                if (line.isEmpty()) continue;
                String[] splits = line.split(delimiter, -1);
                if (splits.length < 2) continue;
                V source = uParser.parse(splits[0]);
                V target = uParser.parse(splits[1]);
                if (!graph.containsEdge(source, target)) continue;

                int edgeIdx = multi ? occurrences.merge(splits[0] + "\t" + splits[1], 1, Integer::sum) - 1 : 0;
                for (int j = 0; j < numAttr; ++j)
                {
                    int col = j + 2;
                    if (col >= splits.length) break;
                    Object value = types[j].parse(splits[col]);
                    if (value == null) continue;
                    if (multi) ((MultiGraph<V>) graph).setEdgeAttribute(source, target, edgeIdx, names[j], value);
                    else graph.setEdgeAttribute(source, target, names[j], value);
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
