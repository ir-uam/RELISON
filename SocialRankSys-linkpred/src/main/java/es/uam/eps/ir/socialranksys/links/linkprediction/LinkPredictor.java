/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
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
 * Predicts a collection of links which are likely to appear in the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public interface LinkPredictor<U>
{
    /**
     * Obtains a list with the scores for the link prediction algorithms.
     * @return A list containing the score for each pair.
     */
    List<Tuple2od<Pair<U>>> getPrediction();
    
    /**
     * Free link prediction. Predicts scores for any pair of users in the network,
     * but limits the list size.
     * 
     * @param maxLength The maximum length of the list.
     * @return The prediction.
     */
    List<Tuple2od<Pair<U>>> getPrediction(int maxLength);
    
    /**
     * Filter link prediction. Predicts scores only for those items that pass the filter.
     * 
     * @param filter Filter to decide which pairs will obtain an score.
     * @return The prediction.
     */
    List<Tuple2od<Pair<U>>> getPrediction(Predicate<Pair<U>> filter);
    
    /**
     * Filter link prediction. Predicts scores only for those items that pass the filter,
     * and limits the list size.
     * 
     * @param maxLength The maximum length of the list.
     * @param filter Filter to decide which pairs will obtain an score
     * @return The prediction
     */
    List<Tuple2od<Pair<U>>> getPrediction(int maxLength, Predicate<Pair<U>> filter);
    
    /**
     * Filter link prediction. Predicts scores only for the candidates in a given list.
     * @param candidates The list of pairs.
     * @return The prediction
     */
    List<Tuple2od<Pair<U>>> getPrediction(Stream<Pair<U>> candidates);
    
    /**
     * Obtains the prediction score for a pair of candidates.
     * @param u First user in the pair.
     * @param v Second user in the pair.
     * @return The prediction score.
     */
    double getPredictionScore(U u, U v);
}
