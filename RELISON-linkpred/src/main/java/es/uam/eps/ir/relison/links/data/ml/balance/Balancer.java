/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.data.ml.balance;


import es.uam.eps.ir.relison.links.data.letor.InstanceSet;

/**
 * Definition of an algorithm that modifies a set of instances, so the classes are balanced.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public interface Balancer<U>
{
    /**
     * Given an unbalanced dataset, creates a new dataset where every class has the same number of examples.
     * @param original the original dataset.
     * @return the balanced dataset.
     */
    InstanceSet<U> balance(InstanceSet<U> original);
}
