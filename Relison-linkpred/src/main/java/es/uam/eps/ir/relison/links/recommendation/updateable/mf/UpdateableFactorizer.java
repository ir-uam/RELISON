/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.updateable.mf;

import es.uam.eps.ir.relison.links.data.updateable.preference.fast.updateable.FastUpdateablePreferenceData;

/**
 * UpdateableFactorizer. Abstract class for matrix factorization algorithms.
 *
 * @author Saúl Vargas (saul.vargas@uam.es)
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the items
 */
public abstract class UpdateableFactorizer<U, I> 
{
    /**
     * Global loss of the factorization.
     *
     * @param factorization matrix factorization
     * @param data          preference data
     * @return the global loss
     */
    public abstract double error(UpdateableFactorization<U, I> factorization, FastUpdateablePreferenceData<U, I> data);

    /**
     * Creates and calculates a factorization.
     *
     * @param K     size of the latent feature space.
     * @param data  preference data
     * @return a matrix factorization
     */
    public abstract UpdateableFactorization<U, I> factorize(int K, FastUpdateablePreferenceData<U, I> data);

    /**
     * Calculates the factorization by using a previously generate matrix
     * factorization.
     *
     * @param factorization matrix factorization
     * @param data          preference data
     */
    public abstract void factorize(UpdateableFactorization<U, I> factorization, FastUpdateablePreferenceData<U, I> data);

    /**
     * Updates the factorization.
     * @param factorization an updateable factorization
     * @param data          preference data
     * @param u             the user
     * @param i             the item
     * @param weight        the rating for the item i provided by the user u
     */
    public abstract void update(UpdateableFactorization<U,I> factorization, FastUpdateablePreferenceData<U,I> data, U u, I i, double weight);

    /**
     * Updates the factorization when a rating is removed.
     * @param factorization an updateable factorization
     * @param data          preference data
     * @param u             the user
     * @param i             the item
     */
    public abstract void updateDelete(UpdateableFactorization<U,I> factorization, FastUpdateablePreferenceData<U,I> data, U u, I i);
}
