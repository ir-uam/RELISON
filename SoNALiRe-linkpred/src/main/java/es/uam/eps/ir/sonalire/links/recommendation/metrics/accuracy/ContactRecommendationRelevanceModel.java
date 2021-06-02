/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.metrics.accuracy;

import es.uam.eps.ir.ranksys.metrics.rel.IdealRelevanceModel;
import es.uam.eps.ir.sonalire.graph.Graph;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Relevance model for the specific contact recommendation task. Only those links
 * appearing in a given test network are judged as relevant.
 *
 * @param <U> type of the users.
 */
public class ContactRecommendationRelevanceModel<U> extends IdealRelevanceModel<U,U>
{
    /**
     * The test graph containing the relevant links.
     */
    private final Graph<U> testGraph;

    /**
     * Constructor.
     * @param caching   true if we want to cache the user relevance models.
     * @param testGraph the test graph containing the relevance edges.
     */
    public ContactRecommendationRelevanceModel(boolean caching, Graph<U> testGraph)
    {
        super(caching, testGraph.getNodesWithAdjacentEdges());
        this.testGraph = testGraph;
    }

    @Override
    protected UserIdealRelevanceModel<U, U> get(U user)
    {
        return new UserContactRecommendationRelevanceModel(user);
    }

    /**
     * The relevance model for a single user in the network.
     */
    private class UserContactRecommendationRelevanceModel implements UserIdealRelevanceModel<U,U>
    {
        /**
         * The set of adjacent neighbors of the given user in the network.
         */
        private final Set<U> relevantContacts;

        /**
         * Constructor.
         * @param user the user.
         */
        public UserContactRecommendationRelevanceModel(U user)
        {
            this.relevantContacts = testGraph.getAdjacentNodes(user).collect(Collectors.toSet());
        }

        @Override
        public Set<U> getRelevantItems()
        {
            return relevantContacts;
        }

        @Override
        public boolean isRelevant(U u)
        {
            return relevantContacts.contains(u);
        }

        @Override
        public double gain(U u)
        {
            return isRelevant(u) ? 1.0 : 0.0;
        }
    }
}