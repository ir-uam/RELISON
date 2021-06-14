/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.io.graph;

import es.uam.eps.ir.relison.graph.Graph;

import java.io.OutputStream;

/**
 * Interface for graph writers.
 *
 * @param <V> type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface GraphWriter<V>
{
    /**
     * Writes a graph into a file. It writes the weights, but not the types
     *
     * @param graph The graph we want to write.
     * @param file  The file.
     *
     * @return true if everything went OK, false otherwise.
     */
    boolean write(Graph<V> graph, String file);

    /**
     * Writes a graph into an output stream. It writes the weights, but not the types.
     *
     * @param graph The graph we want to write.
     * @param file  The output stream.
     *
     * @return true if everything went OK, false otherwise.
     */
    boolean write(Graph<V> graph, OutputStream file);

    /**
     * Writes a graph into a file. Simple graphs types are written, while multigraph
     * types are not.
     *
     * @param graph        The graph we want to write.
     * @param file         The file.
     * @param writeWeights Indicates if weights have to be written.
     * @param writeTypes   Indicates if types have to be written.
     *
     * @return true if everything is ok, false otherwise.
     */
    boolean write(Graph<V> graph, String file, boolean writeWeights, boolean writeTypes);

    /**
     * Writes a graph into a output stream. Simple graphs types are written, while multigraph
     * types are not.
     *
     * @param graph        The graph we want to write.
     * @param file         The file.
     * @param writeWeights Indicates if weights have to be written.
     * @param writeTypes   Indicates if types have to be written.
     *
     * @return true if everything is ok, false otherwise.
     */
    boolean write(Graph<V> graph, OutputStream file, boolean writeWeights, boolean writeTypes);


}
