/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.foaf;

import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Recommender that uses the Adamic-Adar coefficient of the neighbours. It weights the common neighbors between
 * the target and the candidate users by the inverse of the logarithm of the degree of such common user.
 *
 * <p>
 * <b>Reference: </b> Adamic, L.A., Adar, E.: Friends and neighbors on the Web. Social Networks, 25(3), 211–230 (2003)
 * </p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AdamicAdar<U> extends UserFastRankingRecommender<U>
{
    /**
     * Map containing the length of the common neighborhoods between target and candidate users.
     */
    private final Int2DoubleMap wSizes;
    /**
     * Neighborhood selection for the target users.
     */
    private final EdgeOrientation uSel;
    /**
     * Neighborhood selection for the candidate users.
     */
    private final EdgeOrientation vSel;
    /**
     * Neighborhood selection for the intermediate users
     */
    private final EdgeOrientation wSel;

    /**
     * Constructor.
     *
     * @param graph the graph.
     * @param uSel  the neighborhood selection for the target user.
     * @param vSel  the neighborhood selection for the candidate user.
     * @param wSel  the neighborhood selection for the users in the intersection
     */
    public AdamicAdar(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, EdgeOrientation wSel)
    {
        super(graph);

        wSizes = new Int2DoubleOpenHashMap();
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        this.wSel = wSel;

        this.getAllUidx().forEach(widx -> wSizes.put(widx, graph.getNeighborhood(widx, wSel).count() + 0.0));
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        graph.getNeighborhood(uidx, uSel).forEach(widx ->
        {
            double weight = Math.log(2.0) / Math.log(wSizes.get((int) widx) + 2.0);
            graph.getNeighborhood(widx, vSel).forEach(vidx -> scoresMap.addTo(vidx, weight));
        });

        return scoresMap;
    }
}
