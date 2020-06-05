/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
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

/**
 * Recommender that uses the Jaccard coefficient of the neighbours.
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 */
public class ResourceAllocation<U> extends UserFastRankingRecommender<U>
{
    /**
     * Map containing the length of the common neighborhoods between target and candidate users.
     */
    private final Int2DoubleMap wSizes;
    /**
     * Neighborhood selection for the intermediate users
     */
    private final EdgeOrientation wSel;
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
     * @param uSel the neighborhood selection for the target user.
     * @param vSel the neighborhood selection for the candidate user.
     * @param wSel the neighborhood selection for the users in the intersection
     */
    public ResourceAllocation(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, EdgeOrientation wSel)
    {
        super(graph);
        wSizes = new Int2DoubleOpenHashMap();
        this.wSel = wSel;
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        
        this.getAllUidx().forEach(widx -> wSizes.put(widx,graph.getNeighborhood(widx, wSel).count() + 0.0));
    }
    
    
    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);
        graph.getNeighborhood(uidx, uSel).forEach(widx -> 
        {
            double weight = 1.0/(wSizes.get((int)widx) + 2.0);
            graph.getNeighborhood(widx, vSel).forEach(vidx -> scoresMap.addTo(vidx, weight));
        });
       
        return scoresMap;
    }

}
