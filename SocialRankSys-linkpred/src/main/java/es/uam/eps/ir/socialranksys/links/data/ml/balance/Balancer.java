/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.ml.balance;


import es.uam.eps.ir.socialranksys.links.data.letor.InstanceSet;

/**
 * Balances the classes in a dataset
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public interface Balancer<U>
{
    /**
     * Given an unbalanced dataset, creates a new dataset where every class has the same number of examples.
     * @param original The original dataset.
     * @return the balanced dataset.
     */
    InstanceSet<U> balance(InstanceSet<U> original);
}
