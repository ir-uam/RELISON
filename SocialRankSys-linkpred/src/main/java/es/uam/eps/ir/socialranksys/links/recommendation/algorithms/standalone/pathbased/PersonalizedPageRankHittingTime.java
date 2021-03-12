/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.pathbased;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import org.jblas.DoubleMatrix;

/**
 * Implementation of the Hitting time (using the personalized
 * PageRank transition matrix)
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class PersonalizedPageRankHittingTime<U> extends AbstractHittingTime<U>
{
    /**
     * The hitting time matrix (it is the same for all users).
     */
    private DoubleMatrix matrix;
    /**
     * Teleport vector.
     */
    private final double r;

    /**
     * Constructor.
     *
     * @param graph A fast graph representing the social network.
     */
    public PersonalizedPageRankHittingTime(FastGraph<U> graph, double r)
    {
        super(graph);
        this.r = r;
        matrix = this.getMatrix(0);

    }

    @Override
    protected DoubleMatrix getTransitionMatrix(int uidx)
    {
        DoubleMatrix adjacency = graph.getJBLASAdjacencyMatrix(EdgeOrientation.OUT);
        DoubleMatrix matrix = DoubleMatrix.zeros(adjacency.rows);
        DoubleMatrix vector = matrix.rowSums();
        for(int i = 0; i < adjacency.rows; ++i)
        {
            if(vector.get(i) >= 0)
            {
                for(int j = 0; j < adjacency.columns; ++j)
                {
                    adjacency.put(i,j,adjacency.get(i,j)/vector.get(i));
                }
            }

            matrix.put(i, uidx, r);
        }
        matrix.addi(adjacency.mul(1.0-r));
        return matrix;
    }

    @Override
    protected DoubleMatrix getMatrix(int uidx)
    {
        if(matrix != null) return matrix;
        else return super.getMatrix(uidx);
    }
}
