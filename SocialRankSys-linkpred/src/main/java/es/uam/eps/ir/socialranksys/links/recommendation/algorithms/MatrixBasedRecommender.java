/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Abstract implementation of a contact recommendation algorithm that depends on matrix operations.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class MatrixBasedRecommender<U> extends UserFastRankingRecommender<U>
{
    /**
     * The matrix library to consider.
     */
    protected final MatrixLibrary library;

    /**
     * Constructor.
     *
     * @param graph   A fast graph representing the social network.
     * @param library The matrix library to use.
     */
    public MatrixBasedRecommender(FastGraph<U> graph, MatrixLibrary library)
    {
        super(graph);
        this.library = library;
    }

    /**
     * Default constructor. If possible, uses the JBLAS library. Otherwise,
     * it takes the COLT library.
     *
     * @param graph A fast graph representing the social network.
     */
    public MatrixBasedRecommender(FastGraph<U> graph)
    {
        super(graph);
        MatrixChecker.init();
        if (MatrixChecker.fast) this.library = MatrixLibrary.JBLAS;
        else this.library = MatrixLibrary.COLT;
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleMap scoresMap = new Int2DoubleOpenHashMap();

        double[][] vector = this.getMatrix(uidx);

        for (int vidx = 0; vidx < numItems(); ++vidx)
        {
            scoresMap.put(vidx, vector[uidx][vidx]);
        }
        return scoresMap;
    }

    /**
     * Obtains the matrix that provides the ratings given
     * the target user.
     *
     * @param uidx the target user.
     *
     * @return the matrix containing the item scores in the uidx row.
     */
    public double[][] getMatrix(int uidx)
    {
        return switch(library)
        {
            case JBLAS -> this.getJBLASMatrix(uidx);
            case MTJ -> this.getMTJMatrix(uidx);
            case COLT -> this.getCOLTMatrix(uidx);
        };
    }

    /**
     * Obtains the matrix that provides the ratings given
     * the target user, using the JBLAS library.
     *
     * @param uidx the target user.
     *
     * @return the matrix containing the item scores in the uidx row.
     */
    protected abstract double[][] getJBLASMatrix(int uidx);
    /**
     * Obtains the matrix that provides the ratings given
     * the target user, using the COLT library.
     *
     * @param uidx the target user.
     *
     * @return the matrix containing the item scores in the uidx row.
     */
    protected abstract double[][] getCOLTMatrix(int uidx);
    /**
     * Obtains the matrix that provides the ratings given
     * the target user, using the MTJ library.
     *
     * @param uidx the target user.
     *
     * @return the matrix containing the item scores in the uidx row.
     */
    protected abstract double[][] getMTJMatrix(int uidx);




}