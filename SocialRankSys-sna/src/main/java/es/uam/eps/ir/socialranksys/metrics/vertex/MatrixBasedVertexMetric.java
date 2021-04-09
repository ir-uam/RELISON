/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.vertex;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.utils.matrix.MatrixChecker;
import es.uam.eps.ir.socialranksys.utils.matrix.MatrixLibrary;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Vertex metric based on matrices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public abstract class MatrixBasedVertexMetric<U> implements VertexMetric<U>
{
    /**
     * The matrix library to use.
     */
    private final MatrixLibrary library;

    /**
     * Constructor.
     *
     * @param library the matrix library to use.
     */
    public MatrixBasedVertexMetric(MatrixLibrary library)
    {
        this.library = library;
    }

    /**
     * Default constructor. If possible, uses the JBLAS library. Otherwise,
     * it takes the COLT library.
     *
     */
    public MatrixBasedVertexMetric()
    {
        MatrixChecker.init();
        if (MatrixChecker.fast) this.library = MatrixLibrary.JBLAS;
        else this.library = MatrixLibrary.COLT;
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph)
    {
        Index<U> index = graph.getAdjacencyMatrixMap();
        double[] scores = this.getScores(graph);

        Map<U, Double> res = new HashMap<>();
        for(int i = 0; i < scores.length; ++i)
        {
            res.put(index.idx2object(i), scores[i]);
        }
        return res;
    }

    @Override
    public double compute(Graph<U> graph, U user)
    {
        return this.compute(graph).get(user);
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph, Stream<U> users)
    {
        Map<U, Double> full = this.compute(graph);

        Map<U, Double> res = new ConcurrentHashMap<>();
        users.filter(graph::containsVertex).forEach(x -> res.put(x, full.get(x)));
        return res;
    }

    /**
     * Obtains the value of the metric for each user.
     * @param graph the network.
     * @return an array containing the value of the metric for each user.
     */
    public double[] getScores(Graph<U> graph)
    {
        return switch(library)
        {
            case JBLAS -> getJBLASScores(graph);
            case MTJ -> getMTJScores(graph);
            case COLT -> getCOLTScores(graph);
        };
    }

    /**
     * Obtains the values of the metric for the different users using the JBLAS library.
     * @param graph the network.
     * @return an array containing the value of the metric for each user.
     */
    protected abstract double[] getJBLASScores(Graph<U> graph);

    /**
     * Obtains the values of the metric for the different users using the MTJ library.
     * @param graph the network.
     * @return an array containing the value of the metric for each user.
     */
    protected abstract double[] getMTJScores(Graph<U> graph);

    /**
     * Obtains the values of the metric for the different users using the COLT library.
     * @param graph the network.
     * @return an array containing the value of the metric for each user.
     */
    protected abstract double[] getCOLTScores(Graph<U> graph);
}