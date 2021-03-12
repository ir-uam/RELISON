/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.pathbased;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;
import org.jblas.ranges.IntervalRange;
import org.jblas.ranges.Range;

/**
 * Abstract version of the hitting time algorithm.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class AbstractHittingTime<U> extends UserFastRankingRecommender<U>
{
    /**
     * Constructor.
     *
     * @param graph A fast graph representing the social network.
     */
    public AbstractHittingTime(FastGraph<U> graph)
    {
        super(graph);
    }

    protected DoubleMatrix getMatrix(int uidx)
    {
        int nUsers = Long.valueOf(graph.getVertexCount()).intValue();
        // First, we find the transition matrix:

        DoubleMatrix T = this.getTransitionMatrix(uidx);
        DoubleMatrix A = DoubleMatrix.eye(nUsers);
        A.addi(T.mul(-1.0));

        Range range = new IntervalRange(0, nUsers-1);

        DoubleMatrix U = A.get(range, range);
        DoubleMatrix D = A.get(nUsers-1, range);

        DoubleMatrix invU = Solve.solve(U, DoubleMatrix.eye(nUsers-1));
        DoubleMatrix h = Solve.solve(U, D);
        double beta = h.sum();

        DoubleMatrix W = DoubleMatrix.zeros(nUsers, nUsers);
        for(int i = 0; i < nUsers; ++i)
        {
            W.put(new IntervalRange(i,i+1), range, h.mul(-1.0/beta));
            W.put(i, nUsers-1, 1.0/beta);
        }

        DoubleMatrix aux = DoubleMatrix.zeros(nUsers, nUsers);
        aux.put(range, range, invU);
        DoubleMatrix AA = DoubleMatrix.eye(nUsers);
        AA.addi(W.mul(-1.0));

        DoubleMatrix AHash = AA.mmul(aux).mmul(AA);
        DoubleMatrix AHashDiag = AHash.diag().diag();

        DoubleMatrix J = DoubleMatrix.ones(nUsers, nUsers);
        aux = DoubleMatrix.eye(nUsers).add(AHash.mul(-1.0)).add(J.mmul(AHashDiag));
        DoubleMatrix Pi = DoubleMatrix.zeros(nUsers, nUsers);
        for(int i = 0; i < nUsers; ++i)
        {
            Pi.put(i,i, W.get(i,i));
        }

        return aux.mmuli(Pi);
    }

    protected abstract DoubleMatrix getTransitionMatrix(int uidx);

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        DoubleMatrix matrix = this.getMatrix(uidx);

        Int2DoubleMap scoresMap = new Int2DoubleOpenHashMap();
        for(int vidx = 0; vidx < matrix.columns; ++vidx)
        {
            scoresMap.put(vidx, matrix.get(uidx, vidx));
        }

        return scoresMap;
    }
}
