/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of applying a link prediction algorithm.
 * @param <U> type of the users.
 */
public class Prediction<U>
{
    /**
     * The list of links, sorted by descending score.
     */
    private final List<Tuple2od<Pair<U>>> ranking;

    /**
     * Constructs the prediction.
     * @param ranking a list of links sorted by descending score.
     */
    public Prediction(List<Tuple2od<Pair<U>>> ranking)
    {
        this.ranking = ranking;
    }

    /**
     * Obtains the complete prediction list.
     * @return the list of predicted links.
     */
    public List<Tuple2od<Pair<U>>> getPrediction()
    {
        return this.ranking;
    }

    /**
     * Obtains the prediction for a single user.
     * @param u the user.
     * @return the prediction for a single user.
     */
    public Recommendation<U,U> getPrediction(U u)
    {
        List<Tuple2od<U>> rec = new ArrayList<>();
        for(Tuple2od<Pair<U>> predictedLink : ranking)
        {
            if(predictedLink.v1.v1().equals(u))
            {
                rec.add(new Tuple2od<>(predictedLink.v1.v2(), predictedLink.v2));
            }
        }

        return new Recommendation<>(u, rec);
    }
}