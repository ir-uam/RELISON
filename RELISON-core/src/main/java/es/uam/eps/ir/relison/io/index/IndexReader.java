/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autonoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.relison.io.index;

import es.uam.eps.ir.relison.index.Index;

import java.io.InputStream;

/**
 * Interface for creating classes that read indexes.
 *
 * @param <U> Type of the elements of the index.
 *
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface IndexReader<U>
{
    /**
     * Reads an index from a file.
     *
     * @param file the file.
     *
     * @return the index if everything is OK, null otherwise.
     */
    Index<U> read(String file);

    /**
     * Reads an index from an input stream.
     *
     * @param stream the stream.
     *
     * @return the index if everything is OK, null otherwise.
     */
    Index<U> read(InputStream stream);
}
