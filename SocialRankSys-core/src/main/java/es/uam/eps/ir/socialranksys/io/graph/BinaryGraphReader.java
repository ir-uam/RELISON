/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.io.graph;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.GraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.socialranksys.index.Index;

import java.io.*;

/**
 * Class for reading graph from binary files
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class BinaryGraphReader implements GraphReader<Long>
{
    @Override
    public Graph<Long> read(String file)
    {
        return this.read(file, true, false);
    }

    @Override
    public Graph<Long> read(String file, boolean readWeights, boolean readTypes)
    {
        try (InputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(file))))
        {
            return this.read(stream, readWeights, readTypes);
        }
        catch (IOException ioe)
        {
            return null;
        }
    }

    @Override
    public Graph<Long> read(InputStream stream)
    {
        return this.read(stream, true, false);
    }

    @Override
    public Graph<Long> read(InputStream stream, boolean readWeights, boolean readTypes)
    {
        try
        {
            DataInputStream input = (DataInputStream) stream;
            boolean multigraph = input.readBoolean();
            boolean directed = input.readBoolean();
            boolean weighted = input.readBoolean();

            if (multigraph)
            {
                return this.readMultiGraph(input, directed, weighted, readWeights, readTypes);
            }
            else
            {
                return this.readSimpleGraph(input, directed, weighted, readWeights, readTypes);
            }
        }
        catch (IOException ex)
        {
            return null;
        }
    }

    /**
     * Reads a multigraph from an input stream reading a binary file
     *
     * @param stream      the input stream
     * @param directed    true if the graph is directed, false otherwise.
     * @param weighted    true if the graph is weighted, false otherwise.
     * @param readWeights true if weights have to be read.
     * @param readTypes   true if types have to be read.
     *
     * @return the multigraph.
     */
    private Graph<Long> readMultiGraph(InputStream stream, boolean directed, boolean weighted, boolean readWeights, boolean readTypes)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Reads a graph from an input stream reading a binary file.
     *
     * @param stream      the input stream.
     * @param directed    true if the graph is directed, false otherwise.
     * @param weighted    true if the graph is weighted, false otherwise.
     * @param readWeights true if weights have to be read.
     * @param readTypes   true if types have to be read.
     *
     * @return the graph.
     *
     * @throws IOException if something fails during the reading.
     */
    private Graph<Long> readSimpleGraph(InputStream stream, boolean directed, boolean weighted, boolean readWeights, boolean readTypes) throws IOException
    {
        try
        {
            DataInputStream input = (DataInputStream) stream;
            GraphGenerator<Long> ggen = new EmptyGraphGenerator<>();
            ggen.configure(directed, weighted);

            Graph<Long> graph = ggen.generate();
            long numUsers = input.readLong();
            for (int i = 0; i < numUsers; ++i)
            {
                Long u = input.readLong();
                int numAdj = input.readInt();
                for (int j = 0; j < numAdj; ++j)
                {
                    Long v = input.readLong();
                    double weight = (readWeights ? input.readDouble() : 1.0);
                    int type = (readTypes ? input.readInt() : 0);

                    graph.addEdge(u, v, weight, type, true);
                }
            }

            return graph;
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }
    }

    @Override
    public Graph<Long> read(String file, boolean readWeights, boolean readTypes, Index<Long> users)
    {
        try (InputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(file))))
        {
            return this.read(stream, readWeights, readTypes, users);
        }
        catch (IOException ioe)
        {
            return null;
        }
    }

    @Override
    public Graph<Long> read(InputStream stream, boolean readWeights, boolean readTypes, Index<Long> users)
    {
        try
        {
            DataInputStream input = (DataInputStream) stream;
            boolean multigraph = input.readBoolean();
            boolean directed = input.readBoolean();
            boolean weighted = input.readBoolean();

            if (multigraph)
            {
                return this.readMultiGraph(input, directed, weighted, readWeights, readTypes, users);
            }
            else
            {
                return this.readSimpleGraph(input, directed, weighted, readWeights, readTypes, users);
            }
        }
        catch (IOException ex)
        {
            return null;
        }
    }

    /**
     * Reads a multigraph from an input stream reading a binary file.
     *
     * @param stream      the input stream
     * @param directed    true if the graph is directed, false otherwise.
     * @param weighted    true if the graph is weighted, false otherwise.
     * @param readWeights true if weights have to be read.
     * @param readTypes   true if types have to be read.
     * @param users       a user index.
     *
     * @return the multigraph.
     */
    private Graph<Long> readMultiGraph(InputStream stream, boolean directed, boolean weighted, boolean readWeights, boolean readTypes, Index<Long> users)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Reads a graph from an input stream reading a binary file.
     *
     * @param stream      the input stream.
     * @param directed    true if the graph is directed, false otherwise.
     * @param weighted    true if the graph is weighted, false otherwise.
     * @param readWeights true if weights have to be read.
     * @param readTypes   true if types have to be read.
     * @param users       a user index.
     *
     * @return the graph.
     *
     * @throws IOException if something fails during the reading.
     */
    private Graph<Long> readSimpleGraph(InputStream stream, boolean directed, boolean weighted, boolean readWeights, boolean readTypes, Index<Long> users) throws IOException
    {
        try
        {
            DataInputStream input = (DataInputStream) stream;
            GraphGenerator<Long> ggen = new EmptyGraphGenerator<>();
            ggen.configure(directed, weighted);

            Graph<Long> graph = ggen.generate();

            users.getAllObjectsIds().sorted().forEach(i -> graph.addNode(users.idx2object(i)));

            long numUsers = input.readLong();
            for (int i = 0; i < numUsers; ++i)
            {
                Long u = input.readLong();
                int numAdj = input.readInt();
                for (int j = 0; j < numAdj; ++j)
                {
                    Long v = input.readLong();
                    double weight = (readWeights ? input.readDouble() : 1.0);
                    int type = (readTypes ? input.readInt() : 0);

                    graph.addEdge(u, v, weight, type, false);
                }
            }

            return graph;
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }
    }
}
