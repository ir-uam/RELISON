/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.updateable;

import es.uam.eps.ir.ranksys.rec.Recommender;
import org.jooq.lambda.tuple.Tuple3;

import java.util.stream.Stream;

/**
 * Interface for defining recommendation algorithms which can be updated over time.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 */
public interface UpdateableRecommender<U,I> extends Recommender<U,I>
{
    /**
     * Updates the values of the recommender, considering that the following 
     * preferences have been added.
     * @param tuples the preferences which have been added.
     */
    void update(Stream<Tuple3<U, I, Double>> tuples);

    /**
     * Updates the values of the recommender, considering that the following
     * preferences have been removed.
     * @param tuples the preferences which have been removed.
     */
    void updateDelete(Stream<Tuple3<U, I, Double>> tuples);
    
    /**
     * Updates the values of the recommender, considering that a new user has been
     * added.
     * @param u the user which has been added.
     */
    void updateAddUser(U u);
    
    /**
     * Updates the values of the recommender, considering that a new item has been
     * added.
     * @param i the item which has been added.
     */
    void updateAddItem(I i);
    
    /**
     * Updates the values of the recommender, considering that a new rating has
     * been added.
     * @param u the user.
     * @param i the item
     * @param val the value.
     */
    void update(U u, I i, double val);
    
    /**
     * Updates the values of the recommender, considering that a rating has been
     * deleted.
     * @param u the user.
     * @param i the item.
     * @param val the value.
     */
    void updateDelete(U u, I i, double val);
}
