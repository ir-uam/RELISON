/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction;

import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Definition of a method that predicts a collection of links which are likely
 * to appear in a social network in the future.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public interface LinkPredictor<U>
{
    /**
     * Given a social network, ranks the possible user-user pairs, according
     * to the scores given by the algorithm.
     * @return a sorted list containing the user-user pairs and the link prediction score (in descending
     * score order).
     */
    List<Tuple2od<Pair<U>>> getPrediction();
    
    /**
     * Given a social network, ranks the possible user-user pairs, according
     * to the scores given by the algorithm and returns the a limited set of them.
     * 
     * @param maxLength the maximum size of the link prediction list.
     *
     * @return a sorted list containing the user-user pairs and the link prediction score (in descending score
     * order).
     */
    List<Tuple2od<Pair<U>>> getPrediction(int maxLength);
    
    /**
     * Given a social network, ranks the possible user-user pairs, according to the scores
     * given by the algorithm. It only ranks those user pairs which pass a given filter.
     * 
     * @param filter a filter to indicate which user pairs shall be ranked.
     * @return a sorted list containing the user-user pairs and the link prediction score (in descending score
     * order).
     */
    List<Tuple2od<Pair<U>>> getPrediction(Predicate<Pair<U>> filter);

    /**
     * Given a social network, ranks the possible user-user pairs, according to the scores
     * given by the algorithm. It only ranks those user pairs which pass a given filter.
     * It returns a limited number of such pairs.
     *
     * @param filter a filter to indicate which user pairs shall be ranked.
     * @param maxLength the maximum size of the link prediction list.
     *
     * @return a sorted list containing the user-user pairs and the link prediction score (in descending score
     * order).
     */
    List<Tuple2od<Pair<U>>> getPrediction(int maxLength, Predicate<Pair<U>> filter);

    /**
     * Given a social network, ranks the possible user-user pairs, according to the scores
     * given by the algorithm. It only ranks the received user pairs.
     *
     * @param candidates a stream containing the user pairs to rank.
     *
     * @return a sorted list containing the user-user pairs and the link prediction score (in descending score
     * order).
     */
    List<Tuple2od<Pair<U>>> getPrediction(Stream<Pair<U>> candidates);
    
    /**
     * Obtains the prediction score for a pair of candidates.
     * @param u first user in the pair.
     * @param v second user in the pair.
     * @return the prediction score.
     */
    double getPredictionScore(U u, U v);
}
