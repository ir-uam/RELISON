/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation;

import es.uam.eps.ir.relison.graph.Adapters;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.data.GraphIndex;
import es.uam.eps.ir.relison.metrics.distance.FastDistanceCalculator;

import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * Filters for contact recommendation.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SocialFastFilters
{
    /**
     * Prevents recommenders from recommending links which are not in the training set.
     *
     * @param <U>        type of the users.
     * @param trainGraph the original graph.
     *
     * @return the filter.
     */
    public static <U> Function<U, IntPredicate> notInTrain(FastGraph<U> trainGraph)
    {
        return uidx -> iidx -> !trainGraph.containsEdge(uidx, trainGraph.getIndex().idx2object(iidx));
    }

    /**
     * Prevents recommenders from recommending users to themselves.
     *
     * @param <U>        type of the users.
     * @param trainGraph the original graph.
     *
     * @return the filter.
     */
    public static <U> Function<U, IntPredicate> notSelf(FastGraph<U> trainGraph)
    {
        return uidx -> iidx -> !(iidx == trainGraph.getIndex().object2idx(uidx));
    }


    /**
     * Prevents recommenders from recommending reciprocal links.
     *
     * @param <U>        type of the users.
     * @param trainGraph the original graph.
     * @param gindex     the graph index
     *
     * @return The filter.
     */
    public static <U> Function<U, IntPredicate> notReciprocal(FastGraph<U> trainGraph, GraphIndex<U> gindex)
    {
        return uidx -> iidx -> !trainGraph.containsEdge(gindex.uidx2user(iidx), uidx);
    }

    /**
     * Prevents recommenders from recommending links outside of the network (mainly used for detecting
     * if the links will remain in the network or not.
     *
     * @param <U>        type of the users.
     * @param trainGraph the original graph.
     *
     * @return The filter
     */
    public static <U> Function<U, IntPredicate> onlyNeighbours(FastGraph<U> trainGraph)
    {
        return uidx -> iidx -> trainGraph.containsEdge(uidx, trainGraph.getIndex().idx2object(iidx));
    }

    /**
     * Prevents recommenders from recommending links to nodes with indegree equal to zero.
     *
     * @param <U>        type of the users.
     * @param trainGraph the original graph.
     *
     * @return The filter
     */
    public static <U> Function<U, IntPredicate> onlyFollowedUsers(FastGraph<U> trainGraph)
    {
        return uidx -> iidx -> trainGraph.getIncidentEdgesCount(trainGraph.getIndex().idx2object(iidx)) > 0;
    }

    /**
     * Prevents recommenders from recommending links to nodes farther than a given distance from the target
     * user.
     * @param trainGraph    the training network.
     * @param directed      true if we want to limit distance based on directed (true) or undirected (false) links.
     * @param maxDistance   the maximum possible distance.
     * @param <U>           the type of the users.
     * @return the filter.
     */
    public static <U> Function<U, IntPredicate> limitedDistance(FastGraph<U> trainGraph, boolean directed, int maxDistance)
    {
        if(maxDistance < 0) return uidx -> iidx -> true;

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

        return uidx -> iidx -> distanceCalculator.getDistances(uidx, trainGraph.getIndex().idx2object(iidx)) <= maxDistance;
    }
}
