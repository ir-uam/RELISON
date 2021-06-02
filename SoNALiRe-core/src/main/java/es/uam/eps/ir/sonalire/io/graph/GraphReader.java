/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.io.graph;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.index.Index;

import java.io.InputStream;

/**
 * Interface for graph readers.
 *
 * @param <V> Type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface GraphReader<V>
{
    /**
     * Given a file, reads a graph.
     *
     * @param file The file containing the nodes.
     *
     * @return the graph if everything goes ok, null otherwise.
     */
    Graph<V> read(String file);

    /**
     * Given a file, reads a graph.
     *
     * @param file        The file containing the graph.
     * @param readWeights True if the file contains weights, false otherwise.
     * @param readTypes   True if the file contains types, false otherwise.
     *
     * @return the graph if everything goes ok, null otherwise.
     */
    Graph<V> read(String file, boolean readWeights, boolean readTypes);

    /**
     * Given an file, reads a graph.
     *
     * @param file        The file containing the graph.
     * @param readWeights True if the file contains weights, false otherwise.
     * @param readTypes   True if the file contains graph types.
     * @param nodes       An index containing the nodes in the network.
     *
     * @return the graph if everything goes ok, null otherwise.
     */
    Graph<V> read(String file, boolean readWeights, boolean readTypes, Index<V> nodes);

    /**
     * Given an input stream, reads a file from it (for reading embedded graphs in greater files).
     * By default, assumes the graph contains information about weights, but not about types.
     *
     * @param stream The input stream we read the graph from.
     *
     * @return the graph if everything goes ok, null otherwise.
     */
    Graph<V> read(InputStream stream);

    /**
     * Given an input stream, reads a file from it (for reading embedded graphs in greater files).
     *
     * @param stream      The input stream we read the graph from.
     * @param readWeights True if the file contains weights, false otherwise.
     * @param readTypes   True if the file contains graph types.
     *
     * @return the graph if everything goes ok, null otherwise.
     */
    Graph<V> read(InputStream stream, boolean readWeights, boolean readTypes);

    /**
     * Given an input stream, reads a file from it (for reading embedded graphs in greater files).
     *
     * @param stream      The input stream we read the graph from.
     * @param readWeights True if the file contains weights, false otherwise.
     * @param readTypes   True if the file contains graph types.
     * @param nodes       An index containing the nodes in the network.
     *
     * @return the graph if everything goes ok, null otherwise.
     */
    Graph<V> read(InputStream stream, boolean readWeights, boolean readTypes, Index<V> nodes);
}
