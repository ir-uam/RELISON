/* 
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
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
 * Recommender based on the preferential attachment phenomena. It takes the product of the degrees of the
 * target and candidate user, considering that the probability of creating a link is proportional to such product.
 * When the selected neighbourhood is formed by the incoming nodes, then this method is equal to the Popularity
 * recommender method.
 * <p>
 *  <b>Reference: </b>M.E.J. Newman. Clustering and Preferential Attachment in Growing Networks. Physical Review Letters E, 64(025102) (2001).
 * </p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * 
 */
public class PreferentialAttachment<U> extends UserFastRankingRecommender<U>
{

    /**
     * Link orientation for selecting the neighbours of the target node
     */
    private final EdgeOrientation uSel;
    
    /**
     * Link orientation for selecting the neighbours of the candidate node.
     */
    private final EdgeOrientation vSel;
    
    /**
     * Indicates if the mode is link prediction
     */
    private final boolean linkprediction;
    
    /**
     * Constructor for recommendation mode.

     * @param graph     training graph.
     * @param vSel      link orientation for selecting the neighbours of the candidate node.
     */
    public PreferentialAttachment(FastGraph<U> graph, EdgeOrientation vSel)
    {
        super(graph);
        this.vSel = vSel;
        this.uSel = EdgeOrientation.UND;
        linkprediction = false;
    }
    
    /**
     * Constructor for link prediction mode.
     * @param graph training graph.
     * @param uSel  link orientation for selecting the neighbours of the target node.
     * @param vSel  link orientation for selecting the neighbours of the candidate node.
     */
    public PreferentialAttachment(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel;
        linkprediction = true;
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        U u = this.uidx2user(uidx);
        
        Int2DoubleMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(-1.0);
        double uNeigh = this.getGraph().getNeighbourhoodSize(this.uidx2user(uidx), uSel);
        
        this.getAllUsers().forEach(v -> scoresMap.put(this.item2iidx(v), (linkprediction ? uNeigh*this.getGraph().getNeighbourhoodSize(v, vSel)+0.0 : this.getGraph().getNeighbourhoodSize(v, vSel)+0.0)));
        return scoresMap;
    }

    
    
}
