/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.community.io;

import es.uam.eps.ir.socialranksys.community.Communities;

/**
 * Interface for writing communities to a file.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface CommunitiesWriter<U>
{
    /**
     * Writes the community structure into a file.
     *
     * @param comm community partition.
     * @param file route of the file.
     *
     * @return true if everything goes OK, false otherwise.
     */
    boolean write(Communities<U> comm, String file);
}
