/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.linkprediction.filter;

import es.uam.eps.ir.relison.graph.Adapters;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.metrics.distance.FastDistanceCalculator;
import es.uam.eps.ir.relison.utils.datatypes.Pair;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Filters for link prediction algorithms.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class LinkPredFastFilters<U>
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

    /**
     * Filter that limits how far we can recommend users.
     * @param trainGraph    the training network.
     * @param directed      true if we want to limit on the directed distance, false if we ant to limit on the undirected one.
     * @param maxDistance   the maximum distance to consider.
     * @param <U>           type of the users.
     * @return a filter that only selects pairs of users at distance smaller than a fixed one.
     */
    public static <U> Predicate<Pair<U>> limitDistance(Graph<U> trainGraph, boolean directed, int maxDistance)
    {
        FastDistanceCalculator<U> distanceCalculator = new FastDistanceCalculator<>();

        Graph<U> aux;
        if(!directed && trainGraph.isDirected())
        {
            aux = Adapters.undirected(trainGraph);
        }
        else
        {
            aux = trainGraph;
        }

        distanceCalculator.computeDistances(aux);
        return pair -> distanceCalculator.getDistances(pair.v1(), pair.v2()) <= maxDistance;
    }

    /**
     * Limits the set of users to consider to a set of them.
     * @param targetUsers the set of available users.
     * @param <U> type of the users.
     * @return the filter.
     */
    public static <U> Predicate<Pair<U>> onlyUsers(Set<U> targetUsers)
    {
        return (pair) -> targetUsers.contains(pair.v1()) && targetUsers.contains(pair.v2());
    }

    /**
     * Filter that combines several filters. A pair is only valid if all filters are true.
     * @param filters the list of filters.
     * @param <U>     type of the users.
     * @return a combined filter.
     */
    @SafeVarargs
    public static <U> Predicate<Pair<U>> and(Predicate<Pair<U>>... filters)
    {
        return (pair) ->
        {
            boolean res;
            for(Predicate<Pair<U>> filter : filters)
            {
                if(!filter.test(pair)) return false;
            }
            return true;
        };
    }

    /**
     * Filter that combines several filters. A pair is valid if, at least, one filter is true.
     * @param filters the list of filters.
     * @param <U>     type of the users.
     * @return a combined filter.
     */
    @SafeVarargs
    public static <U> Predicate<Pair<U>> or(Predicate<Pair<U>>... filters)
    {
        return (pair) ->
        {
            boolean res;
            for(Predicate<Pair<U>> filter : filters)
            {
                if(!filter.test(pair)) return false;
            }
            return true;
        };
    }


}
