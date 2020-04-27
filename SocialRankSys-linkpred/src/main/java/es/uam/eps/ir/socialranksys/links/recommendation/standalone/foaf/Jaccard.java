/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.standalone.foaf;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Recommended based on the Jaccard similarity.
 * <p>
 * Jaccard,  P.: Etude comparative de la distribution florale dans une portion des Alpes et des Jura.
 * Bulletin de la Societe Vaudoise des Sciences Naturelles 37(142),547–579 (1901)
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Jaccard<U> extends UserFastRankingRecommender<U>
{
    /**
     * Map containing the length of the neighborhoods of the target users.
     */
    private final Int2DoubleMap uSizes;
    /**
     * Map containing the length of the neighborhoods of the candidate users.
     */
    private final Int2DoubleMap vSizes;
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
    public Jaccard(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        uSizes = new Int2DoubleOpenHashMap();
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();

        if (uSel.equals(vSel) || !graph.isDirected())
        {
            this.getAllUidx().forEach(uidx -> uSizes.put(uidx, graph.getNeighborhood(uidx, uSel).count() + 0.0));
            vSizes = uSizes;
        }
        else
        {
            vSizes = new Int2DoubleOpenHashMap();
            this.getAllUidx().forEach(uidx ->
            {
                uSizes.put(uidx, graph.getNeighborhood(uidx, uSel).count() + 0.0);
                vSizes.put(uidx, graph.getNeighborhood(uidx, vSel).count() + 0.0);
            });
        }
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        double uSize = this.uSizes.get(uidx);
        graph.getNeighborhood(uidx, uSel).forEach(widx ->
            graph.getNeighborhood(widx, vSel).forEach(vidx -> scoresMap.addTo(vidx, 1.0)));

        scoresMap.replaceAll((vidx, sim) -> sim / (uSize + this.vSizes.get((int) vidx) - sim));
        return scoresMap;
    }
}
