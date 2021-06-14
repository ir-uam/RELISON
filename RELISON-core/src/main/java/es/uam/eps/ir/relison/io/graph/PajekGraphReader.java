/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
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
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.relison.index.Index;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.ranksys.formats.parsing.Parser;
import org.ranksys.formats.parsing.Parsers;

import java.io.*;

/**
 * Reads a graph using the Pajek format:
 * <p>
 * Format: <br>
 * <br>
 * *Vertices numVertices<br>
 * vertexId1 "vertexLabel1" <br>
 * vertexId2 "vertexLabel2" <br>
 * ... <br>
 * vertexIdN "vertexLabelN" <br>
 * *Edges or *Arcs numEdges <br>
 * vertexId1.1 vertexId1.2 weight1 <br>
 * vertexId2.1 vertexId2.2 weight2 <br>
 * ... <br>
 * vertexIdM.1 vertexIdM.2 weightM <br>
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class PajekGraphReader<U extends Serializable> implements GraphReader<U>
{
    /**
     * Indicates if the graph to read is a multigraph (true) or not (false)
     */
    private final boolean multigraph;
    /**
     * Indicates if the graph to read is directed (true) or not (false)
     */
    private final boolean directed;
    /**
     * Indicates if the graph to read is weighted (true) or not (false)
     */
    private final boolean weighted;
    /**
     * Indicates if the graph to read allows autoloops (true) or not (false)
     */
    private final boolean selfloops;
    /**
     * Parser for reading the users
     */
    private final Parser<U> uParser;

    /**
     * Constructor
     *
     * @param multigraph Indicates if the graph to read is a multigraph (true) or not (false)
     * @param directed   Indicates if the graph to read is directed (true) or not (false)
     * @param weighted   Indicates if the graph to read is weighted (true) or not (false)
     * @param selfloops  Indicates if the graph to read allows autoloops (true) or not (false)
     * @param uParser    Parser for reading the users
     */
    public PajekGraphReader(boolean multigraph, boolean directed, boolean weighted, boolean selfloops, Parser<U> uParser)
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
        try
        {
            InputStream stream = new FileInputStream(file);
            return this.read(stream);
        }
        catch (FileNotFoundException ex)
        {
            return null;
        }
    }

    @Override
    public Graph<U> read(String file, boolean readWeights, boolean readTypes)
    {
        if (readTypes)
        {
            throw new UnsupportedOperationException("ERROR: Pajek does not support types");
        }

        try
        {
            InputStream stream = new FileInputStream(file);
            return this.read(stream, readWeights, false);
        }
        catch (FileNotFoundException ex)
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
            throw new UnsupportedOperationException("ERROR: Pajek does not support types");
        }

        try
        {
            GraphGenerator<U> ggen = multigraph ? new EmptyMultiGraphGenerator<>() : new EmptyGraphGenerator<>();
            ggen.configure(directed, weighted);
            Graph<U> graph = ggen.generate();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream)))
            {
                String line = br.readLine();
                String[] split = line.split(" ");
                int numVertices = Parsers.ip.parse(split[1]);

                Int2ObjectMap<U> map = new Int2ObjectOpenHashMap<>();

                // Read the vertices of the graph
                for (int i = 0; i < numVertices; ++i)
                {
                    String substring = line.substring(line.indexOf(" "));
                    int id = Parsers.ip.parse(substring);
                    int index0 = line.indexOf("\"");
                    int index1 = line.indexOf("\"", index0 + 1);

                    substring = line.substring(index0 + 1, index1);
                    U u = uParser.parse(substring);
                    map.put(id, u);
                    graph.addNode(u);
                }

                // Read the edges
                line = br.readLine();
                split = line.split(" ");
                int numEdges = Parsers.ip.parse(split[1]);

                // Read the vertices of the graph
                for (int i = 0; i < numEdges; ++i)
                {
                    split = line.split(" ");
                    int uidx = Parsers.ip.parse(split[0]);
                    int vidx = Parsers.ip.parse(split[1]);

                    double weight = 1.0;
                    if (split.length > 2 && readWeights)
                    {
                        weight = Parsers.dp.parse(split[2]);
                    }

                    if (uidx != vidx || selfloops)
                    {
                        graph.addEdge(map.get(uidx), map.get(vidx), weight);
                    }
                }
            }
            return graph;
        }
        catch (IOException | GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }
    }

    @Override
    public Graph<U> read(String file, boolean readWeights, boolean readTypes, Index<U> users)
    {
        if (readTypes)
        {
            throw new UnsupportedOperationException("ERROR: Pajek does not support types");
        }

        try
        {
            InputStream stream = new FileInputStream(file);
            return this.read(stream, readWeights, false, users);
        }
        catch (FileNotFoundException ex)
        {
            return null;
        }
    }

    @Override
    public Graph<U> read(InputStream stream, boolean readWeights, boolean readTypes, Index<U> users)
    {
        throw new UnsupportedOperationException("ERROR: This method has not been implemented yet");
    }

}
