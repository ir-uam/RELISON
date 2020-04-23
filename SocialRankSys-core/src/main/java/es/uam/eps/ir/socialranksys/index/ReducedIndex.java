/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.index;

/**
 * Index that cannot be modified.
 *
 * @param <T> Type of the objects.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface ReducedIndex<T>
{
    /**
     * Gets the index of a given object.
     *
     * @param i Object to obtain.
     *
     * @return the index if the object exists, -1 if not.
     */
    int object2idx(T i);

    /**
     * Gets the object corresponding to a certain index.
     *
     * @param idx The index.
     *
     * @return the object corresponding to the index.
     */
    T idx2object(int idx);
}
