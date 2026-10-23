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
import es.uam.eps.ir.relison.graph.attributes.AttributeType;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Reads a network from a GEXF file (the Gephi exchange format), the counterpart of the GEXF the GUI exports.
 *
 * <p>Reads static node and edge attributes, including defaults, using titles as names (or IDs when titles are absent).
 * Built-in node and edge labels are stored as string attributes named {@code label}, taking precedence over
 * custom attributes with that name. Missing labels remain unset unless a custom label value or default exists.
 * Float values become doubles and unsupported types are preserved as strings. Dynamic attribute declarations
 * are ignored. Node visualization positions and sizes are stored as double attributes named {@code viz:x},
 * {@code viz:y}, {@code viz:z} and {@code viz:size}; invalid visualization values are ignored.
 * Node colors are stored as CSS strings in {@code viz:color}, preserving RGBA alpha values.
 * Edge colors and thickness are stored in {@code viz:color} and {@code viz:thickness}, respectively.
 * Malformed custom attribute values cause the read to return {@code null}. The
 * Directedness is detected from {@code defaultedgetype} and per-edge {@code type} overrides. Mixed graphs
 * are represented as directed graphs, with undirected and mutual edges expanded into reciprocal arcs.
 * If the file omits direction, the constructor flag is used as a fallback. Weightedness remains caller-controlled.</p>
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
    private final boolean labelsAsIds;

    /**
     * Constructor.
     * @param multigraph true if the network allows multiple edges between a pair of nodes.
     * @param directed   fallback directedness when the file omits direction.
     * @param weighted   true if the network is weighted.
     * @param selfloops  true if the network allows self-loops.
     * @param uParser    parser for the node identifiers.
     */
    public GexfGraphReader(boolean multigraph, boolean directed, boolean weighted, boolean selfloops, Parser<U> uParser)
    {
        this(multigraph, directed, weighted, selfloops, uParser, false);
    }

    /** Optional label-based identifiers; missing labels use IDs and collisions fail the read. */
    public GexfGraphReader(boolean multigraph, boolean directed, boolean weighted, boolean selfloops,
                           Parser<U> uParser, boolean labelsAsIds)
    {
        this.multigraph = multigraph;
        this.directed = directed;
        this.weighted = weighted;
        this.selfloops = selfloops;
        this.uParser = uParser;
        this.labelsAsIds = labelsAsIds;
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
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // The format needs no external entities; disabling them keeps an untrusted file from reaching the
            // filesystem or the network while it is being parsed.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(stream);
            Element graphElement = (Element) doc.getElementsByTagNameNS("*", "graph").item(0);
            String defaultType = graphElement.getAttribute("defaultedgetype");
            if (defaultType.isEmpty()) defaultType = directed ? "directed" : "undirected";
            validateDirection(defaultType);
            NodeList edges = doc.getElementsByTagNameNS("*", "edge");
            boolean fileDirected = edges.getLength() == 0 && !"undirected".equals(defaultType);
            for (int i = 0; i < edges.getLength(); i++)
                fileDirected |= !"undirected".equals(edgeDirection((Element) edges.item(i), defaultType));
            GraphGenerator<U> ggen = multigraph ? new EmptyMultiGraphGenerator<>() : new EmptyGraphGenerator<>();
            ggen.configure(fileDirected, weighted);
            Graph<U> graph = ggen.generate();
            Map<String, Definition> nodeAttributes = definitions(doc, graph, true);
            Map<String, Definition> edgeAttributes = definitions(doc, graph, false);

            Map<String, U> identifiers = new LinkedHashMap<>();
            Set<U> usedIdentifiers = new HashSet<>();
            NodeList nodes = doc.getElementsByTagNameNS("*", "node");
            for (int i = 0; i < nodes.getLength(); ++i)
            {
                Element element = element(nodes.item(i));
                if (element == null) continue;
                String id = element.getAttribute("id");
                if (id == null || id.isEmpty()) continue;
                String label = element.getAttribute("label");
                U user = uParser.parse(labelsAsIds && !label.isBlank() ? label : id);
                if (identifiers.containsKey(id) || !usedIdentifiers.add(user))
                    throw new IllegalArgumentException("Duplicate GEXF node identifier: " + user);
                identifiers.put(id, user);
                graph.addNode(user);
                values(element, nodeAttributes, (name, value) -> graph.setNodeAttribute(user, name, value));
                readNodeVisualization(element, graph, user);
            }

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

                U u = resolveIdentifier(source, identifiers, usedIdentifiers);
                U v = resolveIdentifier(target, identifiers, usedIdentifiers);
                // Endpoints declared only on an edge (a file with no <nodes> section) are still added.
                graph.addNode(u);
                graph.addNode(v);
                if (!u.equals(v) || selfloops)
                {
                    addEdge(graph, u, v, weight, element, edgeAttributes);
                    if (fileDirected && !"directed".equals(edgeDirection(element, defaultType)) && !u.equals(v))
                        addEdge(graph, v, u, weight, element, edgeAttributes);
                }
            }

            graph.getAllNodes().forEach(user -> nodeAttributes.values().forEach(definition -> {
                if (definition.defaultValue != null && graph.getNodeAttribute(user, definition.name) == null)
                    graph.setNodeAttribute(user, definition.name, definition.defaultValue);
            }));
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

    private U resolveIdentifier(String id, Map<String, U> identifiers, Set<U> used)
    {
        if (identifiers.containsKey(id)) return identifiers.get(id);
        U user = uParser.parse(id);
        if (!used.add(user)) throw new IllegalArgumentException("Conflicting GEXF node identifier: " + user);
        identifiers.put(id, user);
        return user;
    }

    private void validateDirection(String type)
    {
        if (!"directed".equals(type) && !"undirected".equals(type) && !"mutual".equals(type))
            throw new IllegalArgumentException("Unknown GEXF edge direction: " + type);
    }

    private String edgeDirection(Element edge, String defaultType)
    {
        String type = edge.getAttribute("type");
        if (type.isEmpty()) type = defaultType;
        validateDirection(type);
        return type;
    }

    private void addEdge(Graph<U> graph, U source, U target, double weight, Element element,
                         Map<String, Definition> attributes)
    {
        if (!graph.addEdge(source, target, weight)) return;
        BiConsumer<String, Object> setter;
        if (graph instanceof MultiGraph)
        {
            MultiGraph<U> multi = (MultiGraph<U>) graph;
            int index = multi.getNumEdges(source, target) - 1;
            setter = (name, value) -> multi.setEdgeAttribute(source, target, index, name, value);
        }
        else setter = (name, value) -> graph.setEdgeAttribute(source, target, name, value);
        values(element, attributes, setter);
        readEdgeVisualization(element, graph, setter);
    }

    /** Reads static schemas, indexed by GEXF attribute IDs. */
    private Map<String, Definition> definitions(Document doc, Graph<U> graph, boolean nodes)
    {
        Map<String, Definition> result = new LinkedHashMap<>();
        NodeList schemas = doc.getElementsByTagNameNS("*", "attributes");
        for (int i = 0; i < schemas.getLength(); i++)
        {
            Element schema = (Element) schemas.item(i);
            if (!(nodes ? "node" : "edge").equals(schema.getAttribute("class"))
                    || "dynamic".equals(schema.getAttribute("mode"))) continue;
            for (Node child = schema.getFirstChild(); child != null; child = child.getNextSibling())
            {
                if (!(child instanceof Element) || !"attribute".equals(child.getLocalName())) continue;
                Element attribute = (Element) child;
                String id = attribute.getAttribute("id");
                String name = attribute.getAttribute("title");
                if (name.isBlank()) name = id;
                AttributeType type;
                try { type = AttributeType.fromToken(attribute.getAttribute("type")); }
                catch (IllegalArgumentException ex) { type = AttributeType.STRING; }
                if ("label".equals(name)) type = AttributeType.STRING;
                Object defaultValue = null;
                for (Node value = attribute.getFirstChild(); value != null; value = value.getNextSibling())
                    if (value instanceof Element && "default".equals(value.getLocalName()))
                        defaultValue = parse(type, value.getTextContent());
                result.put(id, new Definition(name, type, defaultValue));
                if (nodes) graph.defineNodeAttribute(name, type);
                else graph.defineEdgeAttribute(name, type);
            }
        }
        NodeList elements = doc.getElementsByTagNameNS("*", nodes ? "node" : "edge");
        for (int i = 0; i < elements.getLength(); i++)
        {
            if (((Element) elements.item(i)).hasAttribute("label"))
            {
                if (nodes) graph.defineNodeAttribute("label", AttributeType.STRING);
                else graph.defineEdgeAttribute("label", AttributeType.STRING);
                break;
            }
        }
        return result;
    }

    /** Applies defaults before explicit values, without descending into nested nodes. */
    private void values(Element element, Map<String, Definition> definitions, BiConsumer<String, Object> setter)
    {
        definitions.values().forEach(definition -> {
            if (definition.defaultValue != null) setter.accept(definition.name, definition.defaultValue);
        });
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling())
        {
            if (!(child instanceof Element) || !"attvalues".equals(child.getLocalName())) continue;
            for (Node value = child.getFirstChild(); value != null; value = value.getNextSibling())
            {
                if (!(value instanceof Element) || !"attvalue".equals(value.getLocalName())) continue;
                Element entry = (Element) value;
                Definition definition = definitions.get(entry.getAttribute("for"));
                if (definition != null && entry.hasAttribute("value"))
                    setter.accept(definition.name, parse(definition.type, entry.getAttribute("value")));
            }
        }
        if (element.hasAttribute("label")) setter.accept("label", element.getAttribute("label"));
    }

    /** Reads visualization fields independently of the XML namespace prefix. */
    private void readNodeVisualization(Element node, Graph<U> graph, U user)
    {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
        {
            if (!(child instanceof Element)) continue;
            Element visual = (Element) child;
            String namespace = visual.getNamespaceURI();
            if (namespace == null || !namespace.matches("https?://(www\\.)?gexf\\.net/[^/]+/viz")) continue;
            if ("position".equals(visual.getLocalName()))
                for (String axis : new String[]{"x", "y", "z"})
                    readVisualNumber(visual, axis, "viz:" + axis, graph, user, false);
            else if ("size".equals(visual.getLocalName()))
                readVisualNumber(visual, "value", "viz:size", graph, user, true);
            else if ("color".equals(visual.getLocalName()))
            {
                String color = readVisualColor(visual);
                if (color != null)
                {
                    graph.defineNodeAttribute("viz:color", AttributeType.STRING);
                    graph.setNodeAttribute(user, "viz:color", color);
                }
            }
        }
    }

    /** Preserves visual styling per edge, independently of edge weight. */
    private void readEdgeVisualization(Element edge, Graph<U> graph, BiConsumer<String, Object> setter)
    {
        for (Node child = edge.getFirstChild(); child != null; child = child.getNextSibling())
        {
            if (!(child instanceof Element)) continue;
            Element visual = (Element) child;
            String namespace = visual.getNamespaceURI();
            if (namespace == null || !namespace.matches("https?://(www\\.)?gexf\\.net/[^/]+/viz")) continue;
            if ("color".equals(visual.getLocalName()))
            {
                String color = readVisualColor(visual);
                if (color != null)
                {
                    graph.defineEdgeAttribute("viz:color", AttributeType.STRING);
                    setter.accept("viz:color", color);
                }
            }
            else if ("thickness".equals(visual.getLocalName()))
            {
                try
                {
                    double thickness = Double.parseDouble(visual.getAttribute("value"));
                    if (!Double.isFinite(thickness) || thickness < 0) continue;
                    graph.defineEdgeAttribute("viz:thickness", AttributeType.DOUBLE);
                    setter.accept("viz:thickness", thickness);
                }
                catch (NumberFormatException ignored) { /* Keep default rendering for invalid values. */ }
            }
        }
    }

    /** Converts GEXF hex or RGBA colors to CSS, ignoring malformed color data. */
    private String readVisualColor(Element element)
    {
        String hex = element.getAttribute("hex");
        if (hex.matches("#[0-9a-fA-F]{6}")) return hex;
        try
        {
            int r = Integer.parseInt(element.getAttribute("r"));
            int g = Integer.parseInt(element.getAttribute("g"));
            int b = Integer.parseInt(element.getAttribute("b"));
            double a = element.hasAttribute("a") ? Double.parseDouble(element.getAttribute("a")) : 1.0;
            if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255
                    || !Double.isFinite(a) || a < 0 || a > 1) return null;
            return "rgba(" + r + "," + g + "," + b + "," + a + ")";
        }
        catch (NumberFormatException ignored) { return null; }
    }

    private void readVisualNumber(Element element, String field, String name, Graph<U> graph, U user,
                                  boolean nonnegative)
    {
        if (!element.hasAttribute(field)) return;
        try
        {
            double value = Double.parseDouble(element.getAttribute(field));
            if (!Double.isFinite(value) || (nonnegative && value < 0)) return;
            graph.defineNodeAttribute(name, AttributeType.DOUBLE);
            graph.setNodeAttribute(user, name, value);
        }
        catch (NumberFormatException ignored) { /* Use the display fallback for invalid visualization data. */ }
    }

    private Object parse(AttributeType type, String value)
    {
        return type == AttributeType.STRING ? value : type.parse(value);
    }

    private static class Definition
    {
        final String name;
        final AttributeType type;
        final Object defaultValue;

        Definition(String name, AttributeType type, Object defaultValue)
        {
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
        }
    }

    /** The DOM node as an element, or {@code null} when it is not one. */
    private Element element(Node node)
    {
        return (node instanceof Element) ? (Element) node : null;
    }
}
