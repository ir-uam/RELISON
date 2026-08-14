/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.io.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.EmptyMultiGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.GraphGenerator;
import es.uam.eps.ir.relison.index.Index;
import org.ranksys.formats.parsing.Parser;
import org.ranksys.formats.parsing.Parsers;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Reads a network from a GEXF file (the Gephi exchange format), the counterpart of the GEXF the GUI exports.
 *
 * <p>Only the structure is read — the nodes and the (optionally weighted) edges. Visual hints ({@code viz:position},
 * {@code viz:size}, {@code viz:color}) and {@code attvalues} are ignored, matching the other readers in this package,
 * which likewise take the topology from the network file and leave attributes to their own sidecar files. The
 * directedness / weightedness of the resulting graph come from this reader's flags rather than from the file's
 * {@code defaultedgetype}, so the caller stays in control of the network type.</p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class GexfGraphReader<U extends Serializable> implements GraphReader<U>
{
    private final boolean multigraph;
    private final boolean directed;
    private final boolean weighted;
    private final boolean selfloops;
    private final Parser<U> uParser;

    /**
     * Constructor.
     * @param multigraph true if the network allows multiple edges between a pair of nodes.
     * @param directed   true if the network is directed.
     * @param weighted   true if the network is weighted.
     * @param selfloops  true if the network allows self-loops.
     * @param uParser    parser for the node identifiers.
     */
    public GexfGraphReader(boolean multigraph, boolean directed, boolean weighted, boolean selfloops, Parser<U> uParser)
    {
        this.multigraph = multigraph;
        this.directed = directed;
        this.weighted = weighted;
        this.selfloops = selfloops;
        this.uParser = uParser;
    }

    @Override
    public Graph<U> read(String file)
    {
        return this.read(file, true, false);
    }

    @Override
    public Graph<U> read(String file, boolean readWeights, boolean readTypes)
    {
        try (InputStream stream = new FileInputStream(file))
        {
            return this.read(stream, readWeights, readTypes);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    @Override
    public Graph<U> read(InputStream stream)
    {
        return this.read(stream, true, false);
    }

    @Override
    public Graph<U> read(InputStream stream, boolean readWeights, boolean readTypes)
    {
        if (readTypes)
        {
            throw new UnsupportedOperationException("ERROR: GEXF does not support types");
        }

        try
        {
            GraphGenerator<U> ggen = multigraph ? new EmptyMultiGraphGenerator<>() : new EmptyGraphGenerator<>();
            ggen.configure(directed, weighted);
            Graph<U> graph = ggen.generate();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // The format needs no external entities; disabling them keeps an untrusted file from reaching the
            // filesystem or the network while it is being parsed.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(stream);

            NodeList nodes = doc.getElementsByTagNameNS("*", "node");
            for (int i = 0; i < nodes.getLength(); ++i)
            {
                Element element = element(nodes.item(i));
                if (element == null) continue;
                String id = element.getAttribute("id");
                if (id == null || id.isEmpty()) continue;
                graph.addNode(uParser.parse(id));
            }

            NodeList edges = doc.getElementsByTagNameNS("*", "edge");
            for (int i = 0; i < edges.getLength(); ++i)
            {
                Element element = element(edges.item(i));
                if (element == null) continue;
                String source = element.getAttribute("source");
                String target = element.getAttribute("target");
                if (source == null || source.isEmpty() || target == null || target.isEmpty()) continue;

                double weight = 1.0;
                String w = element.getAttribute("weight");
                if (readWeights && w != null && !w.isEmpty())
                {
                    try { weight = Parsers.dp.parse(w); }
                    catch (Exception ex) { weight = 1.0; }
                }

                U u = uParser.parse(source);
                U v = uParser.parse(target);
                // Endpoints declared only on an edge (a file with no <nodes> section) are still added.
                graph.addNode(u);
                graph.addNode(v);
                if (!u.equals(v) || selfloops)
                {
                    graph.addEdge(u, v, weight);
                }
            }

            return graph;
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    @Override
    public Graph<U> read(String file, boolean readWeights, boolean readTypes, Index<U> users)
    {
        throw new UnsupportedOperationException("ERROR: reading a GEXF file over a fixed user index is not supported");
    }

    @Override
    public Graph<U> read(InputStream stream, boolean readWeights, boolean readTypes, Index<U> users)
    {
        throw new UnsupportedOperationException("ERROR: reading a GEXF file over a fixed user index is not supported");
    }

    /** The DOM node as an element, or {@code null} when it is not one. */
    private Element element(Node node)
    {
        return (node instanceof Element) ? (Element) node : null;
    }
}
