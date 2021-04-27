/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.foaf;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Recommender using the cosine similarity to produce recommendations. It considers each user as a vector, where the
 * i-th coordinate is the weight of the link between u and the i-th user, and takes the cosine between two user vectors
 * as the recommendation score.
 *
 * <p>
 * <b>Reference: </b>Lü, L., Zhou. T. Link Prediction in Complex Networks: A survey. Physica A: Statistical Mechanics and its Applications, 390(6), March 2011, pp. 1150-1170.
 * </p>
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Cosine<U> extends UserFastRankingRecommender<U>
{
    /**
     * Map containing the length of the neighborhoods of the target users.
     */
    private final Map<Integer, Double> uSizes;
    /**
     * Map containing the length of the neighborhoods of the candidate users.
     */
    private final Map<Integer, Double> vSizes;
    /**
     * Neighborhood selection for the target users.
     */
    private final EdgeOrientation uSel;
    /**
     * Neighborhood selection for the candidate users.
     */
    private final EdgeOrientation vSel;

    /**
     * Constructor.
     *
     * @param graph the graph.
     * @param uSel  the neighborhood selection for the target user.
     * @param vSel  the neighborhood selection for the candidate user.
     */
    public Cosine(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        uSizes = new HashMap<>();
        vSizes = new HashMap<>();

        if (!graph.isWeighted())
        {
            graph.getAllNodes().forEach(u -> uSizes.put(graph.object2idx(u), graph.getNeighbourhoodSize(u, uSel) + 0.0));
            if (uSel.equals(vSel) || !graph.isDirected())
            {
                vSizes.putAll(uSizes);
            }
            else
            {
                graph.getAllNodes().forEach(v -> vSizes.put(graph.object2idx(v), graph.getNeighbourhoodSize(v, vSel) + 0.0));
            }
        }
        else
        {
            graph.getAllNodes().forEach(u -> uSizes.put(graph.object2idx(u), graph.getNeighborhoodWeights(graph.object2idx(u), uSel).mapToDouble(x -> x.v2).sum()));
            if (uSel.equals(vSel) || !graph.isDirected())
            {
                vSizes.putAll(uSizes);
            }
            else
            {
                graph.getAllNodes().forEach(v -> vSizes.put(graph.object2idx(v), graph.getNeighborhoodWeights(graph.object2idx(v), vSel).mapToDouble(x -> x.v2).sum()));
            }
        }
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        graph.getNeighborhoodWeights(uidx, uSel).forEach(widx ->
                graph.getNeighborhoodWeights(widx.v1, vSel).forEach(vidx ->
                        scoresMap.put(vidx.v1(), scoresMap.getOrDefault(vidx.v1, 0.0) + widx.v2 * vidx.v2)
                )
        );

        scoresMap.replaceAll((vidx, sim) -> sim / (Math.sqrt(this.vSizes.get(vidx) * this.uSizes.get(uidx))+1.0));
        return scoresMap;
    }


}
