/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.community.io;

import es.uam.eps.ir.relison.community.Communities;

/**
 * Interface for reading communities from a file.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface CommunitiesReader<U>
{
    /**
     * Reads the community structure from a file.
     *
     * @param file the file containing the community structure.
     *
     * @return the community partition.
     */
    Communities<U> read(String file);
}
