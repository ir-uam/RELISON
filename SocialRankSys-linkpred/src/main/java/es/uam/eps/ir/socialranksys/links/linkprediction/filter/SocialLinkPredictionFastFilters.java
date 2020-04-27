/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction.filter;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.function.Predicate;

/**
 * Filters for link prediction algorithms.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users in the networks.
 */
public class SocialLinkPredictionFastFilters<U>
{
    /**
     * Filter that only allows links that already exist in the graph.
     * @param <U> Type of the users
     * @param trainGraph The graph.
     * @return The filter.
     */
    public static <U> Predicate<Pair<U>> onlyNeighbours(Graph<U> trainGraph)
    {
        return pair -> trainGraph.containsEdge(pair.v1(), pair.v2());
    }
    
    /**
     * Filter that only allows links that do not exist in the graph.
     * @param <U> Type of the users
     * @param trainGraph The graph.
     * @return The filter.
     */
    public static <U> Predicate<Pair<U>> onlyNewLinks(Graph<U> trainGraph)
    {
        return pair -> !trainGraph.containsEdge(pair.v1(), pair.v2());
    }
    
    /**
     * Filter that only allows pairs of nodes whose reciprocal does not exist.
     * @param <U> Type of the users
     * @param trainGraph The graph.
     * @return The filter.
     */
    public static <U> Predicate<Pair<U>> notReciprocal(Graph<U> trainGraph)
    {
        return pair -> !trainGraph.containsEdge(pair.v2(), pair.v1());
    }
    
    /**
     * Filter that only allows pairs of nodes whose reciprocal exists.
     * @param <U> Type of the users
     * @param trainGraph The graph.
     * @return The filter.
     */
    public static <U> Predicate<Pair<U>> onlyReciprocal(Graph<U> trainGraph)
    {
        return pair -> trainGraph.containsEdge(pair.v2(), pair.v1());
    }
    
    /**
     * Filter that does not allow predicting autoloops.
     * @param <U> Type of the users.
     * @return The filter.
     */
    public static <U> Predicate<Pair<U>> notSelf()
    {
        return pair -> !(pair.v1().equals(pair.v2()));
    }
}
