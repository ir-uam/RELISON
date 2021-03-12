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

/**
 * Commute time algorithm. It is computed as the hitting time between the target user
 * and the candidate user plus the time needed to do the backwards path.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CommuteTime<U> extends UserFastRankingRecommender<U>
{
    /**
     * A hitting time implementation.
     */
    private final AbstractHittingTime<U> hittingTime;

    /**
     * Constructor.
     *
     * @param graph A fast graph representing the social network.
     */
    public CommuteTime(FastGraph<U> graph, AbstractHittingTime<U> hittingTime)
    {
        super(graph);
        this.hittingTime = hittingTime;
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        DoubleMatrix matrix = hittingTime.getMatrix(uidx);

        Int2DoubleMap scoresMap = new Int2DoubleOpenHashMap();
        for(int vidx = 0; vidx < matrix.columns; ++vidx)
        {
            scoresMap.put(vidx, -matrix.get(uidx, vidx)-matrix.get(vidx,uidx));
        }

        return scoresMap;
    }
}
