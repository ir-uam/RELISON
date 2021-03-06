/* 
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.foaf;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Recommender based on Sorensen similarity. It divides the number of common neighbors by the sum of the
 * degrees of the target and candidate users.
 * <p>
 * <b>Reference: </b>L. Lü, T. Zhou. Link Prediction in Complex Networks: A survey. Physica A: Statistical Mechanics and its Applications, 390(6), 1150-1170 (2011)
 * </p>
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Sorensen<U> extends UserFastRankingRecommender<U>
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
     * @param graph the graph.
     * @param uSel  the neighborhood selection for the target user.
     * @param vSel  the neighborhood selection for the candidate user.
     */
    public Sorensen(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        uSizes = new HashMap<>();
        vSizes = new HashMap<>();
        
        graph.getAllNodes().forEach(u -> uSizes.put(graph.object2idx(u), graph.getNeighbourhoodSize(u, uSel) + 0.0));
        if(uSel.equals(vSel) || !graph.isDirected())
        {
            vSizes.putAll(uSizes);
        }
        else
        {
            graph.getAllNodes().forEach(v -> vSizes.put(graph.object2idx(v), graph.getNeighbourhoodSize(v, vSel)+0.0));           
        }
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        graph.getNeighborhood(uidx, uSel).forEach(widx -> graph.getNeighborhood(widx, vSel).forEach(vidx -> scoresMap.addTo(vidx, 2.0)));
        
        scoresMap.replaceAll((vidx, sim) -> sim/(this.vSizes.get(vidx)+this.uSizes.get(uidx)));
        return scoresMap;
    }

    
}
