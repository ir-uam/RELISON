/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
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
 * Recommender that uses the hub depressed index of the neighbors: given the number of common neighbors
 * between two users, the recommendation score is divided by the size of either the target user or the candidate user:
 * the user with a larger number of them. This way, nodes with small degree are promoted.
 *
 * <br>
 *  <b>References: </b>
 *  <ol>
 *      <li>E. Ravasz, A.L. Somera, D.A. Mongru, Z.N. Oltvai, A-L. Barabasi. Hierarchical Organization in Metabolic Networks, Science 297 (2002)</li>
 *      <li>T. Zhou, L. Lü, Y. Zhang. Predicting missing links via local information. European Physical Journal B 71, 623-630 (2009)</li>
 *  </ol>
 *
 * 
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class HubDepressedIndex<U> extends UserFastRankingRecommender<U>
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
     * @param graph the graph.
     * @param uSel  the neighborhood selection for the target user.
     * @param vSel  the neighborhood selection for the candidate user.
     */
    public HubDepressedIndex(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        uSizes = new Int2DoubleOpenHashMap();
        
        if(uSel.equals(vSel) || !graph.isDirected())
        {
            graph.getAllNodesIds().forEach(uidx -> uSizes.put(uidx, graph.getNeighborhood(uidx, uSel).count()+0.0));
            vSizes = uSizes;
        }
        else
        {
            vSizes = new Int2DoubleOpenHashMap();
            this.getAllUidx().forEach(uidx -> 
            {
               uSizes.put(uidx, graph.getNeighborhood(uidx, uSel).count()+0.0);
               vSizes.put(uidx, graph.getNeighborhood(uidx, vSel).count()+0.0);
            });
        }
    }
    
    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        graph.getNeighborhood(uidx, uSel).forEach(widx -> graph.getNeighborhood(widx, vSel).forEach(vidx -> scoresMap.addTo(vidx, 1.0)));
        
        scoresMap.replaceAll((vidx, sim) -> sim/(Math.max(this.vSizes.get((int) vidx), this.uSizes.get(uidx))+1.0));
        return scoresMap;
    }
}
