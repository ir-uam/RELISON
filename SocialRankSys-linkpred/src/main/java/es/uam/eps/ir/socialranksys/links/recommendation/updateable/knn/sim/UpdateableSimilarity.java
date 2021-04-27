/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.updateable.knn.sim;

import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Generic updateable similarity for fast data. This is the interface that is 
 * under the hood of user and item similarities. It does not need to be
 * symmetric.
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface UpdateableSimilarity extends Similarity
{
    /**
     * Updates the similarities between user/items after adding/modifying a
     * rating.
     * @param idx1  index of user
     * @param idx2  index of item
     * @param val   rating value
     * @return a pair which contains a) a list of users which have been fully updated
     * b) a list of other updated similarities.
     */
    IntList updateAdd(int idx1, int idx2, double val);
    
    /**
     * Updates the similarities between users/items after removing a rating.
     * @param idx1 index of user
     * @param idx2 index of item
     */
    void updateDel(int idx1, int idx2);
    
    /**
     * Updates the similarities between users/items after adding a new element.
     */
    void updateAddElement();
}
