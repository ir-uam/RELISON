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

import es.uam.eps.ir.ranksys.rec.fast.FastRankingRecommender;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.data.FastGraphIndex;
import es.uam.eps.ir.relison.links.data.GraphIndex;

/**
 * Abstract class for user recommendation in social networks.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class UserFastRankingRecommender<U> extends FastRankingRecommender<U, U>
{
    /**
     * The graph which represents the social network relations.
     */
    protected final FastGraph<U> graph;

    /**
     * Constructor.
     *
     * @param graph a fast graph representing the social network.
     */
    public UserFastRankingRecommender(FastGraph<U> graph)
    {
        this(graph, new FastGraphIndex<>(graph));
    }

    /**
     * Private constructor. Receives an index, in addition to a graph
     *
     * @param graph a fast graph representing the social network.
     * @param index the user index of the graph.
     */
    private UserFastRankingRecommender(FastGraph<U> graph, GraphIndex<U> index)
    {
        super(index, index);
        this.graph = graph;
    }

    /**
     * Obtains the graph.
     *
     * @return the graph.
     */
    public FastGraph<U> getGraph()
    {
        return this.graph;
    }

    /**
     * Obtains the weight of an edge.
     *
     * @param u      the first node
     * @param v      the second node
     * @param orient the orientation.
     *
     * @return the weight. In case of UND edge orientation, we take the sum of both possible weights (if the graph is weighted),
     * or 1.0 if any of them exists (if the graph is unweighted).
     */
    protected double getFreq(U u, U v, EdgeOrientation orient)
    {
        double val = 0.0;


        if (graph.isDirected())
        {
            if (!orient.equals(EdgeOrientation.OUT))
            {
                double aux = graph.getEdgeWeight(v, u);
                if (!Double.isNaN(aux))
                {
                    val += aux;
                }
            }
            if (!orient.equals(EdgeOrientation.IN))
            {
                double aux = graph.getEdgeWeight(u, v);
                if (!Double.isNaN(aux))
                {
                    val += aux;
                }
            }
        }
        else
        {
            double aux = graph.getEdgeWeight(u, v);
            if (!Double.isNaN(aux))
            {
                val += graph.getEdgeWeight(u, v);
            }
        }

        return val;
    }

    /**
     * Obtains the weight of an edge.
     *
     * @param uidx   the first node identifier
     * @param vidx   the second node identifier
     * @param orient the orientation.
     *
     * @return the weight. In case of UND edge orientation, we take the sum of both possible weights (if the graph is weighted),
     * or 1.0 if any of them exists (if the graph is unweighted).
     */
    protected double getFreq(int uidx, int vidx, EdgeOrientation orient)
    {
        double val = 0.0;

        if (graph.isDirected())
        {
            if (!orient.equals(EdgeOrientation.OUT))
            {
                val += graph.getEdgeWeight(vidx, uidx);
            }

            if (!orient.equals(EdgeOrientation.IN))
            {
                val += graph.getEdgeWeight(uidx, vidx);
            }
        }
        else
        {
            val += graph.getEdgeWeight(uidx, vidx);
        }

        return val;
    }
}
