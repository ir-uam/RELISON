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
 * Non Reciprocal Preferential Attachment recommender. Recommender based on the Preferential Attachment link prediction method.
 * Instead of counting all the neighbors, we count only the non-reciprocated links. In case of undirected selection of the
 * neighbors, we remove both from the count.
 * When the selected neighbourhood is formed by the incoming nodes, then this method is equal to the Popularity
 * recommender method.
 * <p>
 * <b>Reference: </b> Newman, M.E.J. Clustering and Preferential Attachment in Growing Networks. Physical Review Letters E, 64(025102), April 2001.
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class NonReciprocalPreferentialAttachment<U> extends UserFastRankingRecommender<U>
{
    /**
     * Link orientation for selecting the neighbours of the candidate node.
     */
    private final EdgeOrientation vSel;
    
    /**
     * Map for storing the popularities
     */
    private final Int2DoubleMap popularities;
    
    /**
     * Constructor for recommendation mode.
     * @param graph Graph.
     * @param vSel Link orientation for selecting the neighbours of the candidate node.
     */
    public NonReciprocalPreferentialAttachment(FastGraph<U> graph, EdgeOrientation vSel)
    {
        super(graph);
        this.vSel = vSel;
        this.popularities = new Int2DoubleOpenHashMap();
        this.popularities.defaultReturnValue(-1.0);
        
        this.getAllUsers().forEach(v -> 
        {
            double value = this.getGraph().getNeighbourhood(v, vSel).filter(w -> 
            {
                if(this.vSel.equals(EdgeOrientation.IN))
                {
                    return !this.getGraph().containsEdge(v, w);
                }
                else if(this.vSel.equals(EdgeOrientation.OUT))
                {
                    return !this.getGraph().containsEdge(w,v);
                }
                else
                {
                    return !(this.getGraph().containsEdge(v,w) && this.getGraph().containsEdge(w,v));
                }
            }).count() + 0.0;
            
            this.popularities.put(this.item2iidx(v), value);
        });
    }    

    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        return this.popularities;
    }
}
