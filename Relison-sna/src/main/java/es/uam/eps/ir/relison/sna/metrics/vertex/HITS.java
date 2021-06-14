/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.vertex;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.metrics.VertexMetric;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Computes the HITS values of the different nodes in a graph.
 *
 * <p>
 * <b>Reference:</b> J.M. Kleinberg. Authoritative sources in a hyperlink environment. Journal of the ACM 46(5), PP. 604-632 (1999)
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class HITS<U> implements VertexMetric<U>
{

    /**
     * Maximum number of iterations.
     */
    private final int MAXITER = 50;
    /**
     * Indicates if we want to return authorities or hubs.
     */
    private final boolean authorities;
    /**
     * Last graph which computed the resolution.
     */
    private Graph<U> oldGraph;
    /**
     * True if already computed, false if not.
     */
    private boolean computed;
    /**
     * Authorities values.
     */
    private Map<U, Double> auths;
    /**
     * Hubs values.
     */
    private Map<U, Double> hubs;

    /**
     * Constructor for not personalized PageRank.
     *
     * @param authorities true if we want to compute the authorities, false if not.
     */
    public HITS(boolean authorities)
    {
        this.authorities = authorities;
        this.oldGraph = null;
        this.computed = false;
    }

    @Override
    public double compute(Graph<U> graph, U user)
    {
        return this.compute(graph).get(user);
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph)
    {
        if (graph != null && !graph.equals(oldGraph) && computed)
        {
            if (this.authorities)
            {
                return auths;
            }
            else
            {
                return hubs;
            }
        }
        else if (graph == null)
        {
            computed = false;
            return new HashMap<>();
        }

        computed = false;
        oldGraph = graph;
        Object2DoubleMap<U> hubScore = new Object2DoubleOpenHashMap<>();
        Object2DoubleMap<U> authScore = new Object2DoubleOpenHashMap<>();

        /*
         * Initialize
         */
        graph.getAllNodes().forEach(u ->
        {
            if (graph.getAdjacentNodesCount(u) > 0)
            {
                hubScore.put(u, 1.0);
            }
            else
            {
                hubScore.put(u, 0.0);
            }

            if (graph.getIncidentNodesCount(u) > 0)
            {
                authScore.put(u, 1.0);
            }
            else
            {
                authScore.put(u, 0.0);
            }
        });

        /*
         * Compute the metric
         */
        for (int i = 0; i < MAXITER; ++i)
        {
            // Compute the hubs scores
            double hubsSum = graph.getAllNodes().mapToDouble(hub ->
            {
                double score = graph.getAdjacentNodes(hub).mapToDouble(authScore::getDouble).sum();
                hubScore.put(hub, score);
                return score * score;
            }).sum();

            // Normalize the hubs scores
            graph.getAllNodes().forEach(hub -> hubScore.put(hub, hubScore.getDouble(hub) / Math.sqrt(hubsSum + 0.0)));

            // Compute the authorities scores
            double authSum = graph.getAllNodes().mapToDouble(auth ->
            {
                double score = graph.getIncidentNodes(auth).mapToDouble(hubScore::getDouble).sum();
                authScore.put(auth, score);
                return score * score;
            }).sum();

            // Normalize the authorities scores
            graph.getAllNodes().forEach(auth -> authScore.put(auth, authScore.getDouble(auth) / Math.sqrt(authSum + 0.0)));
        }


        auths = authScore;
        hubs = hubScore;
        computed = true;
        // If we want to compute the authorities, compute them
        if (this.authorities)
        {
            return authScore;
        }
        else
        {
            return hubScore;
        }


    }
}
