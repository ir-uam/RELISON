/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index;

import java.io.IOException;

/**
 * Interface for an object that builds an index.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public interface IndexBuilder<C>
{
    /**
     * Initializes the builder.
     *
     * @param indexPath path containing the index.
     *
     * @throws IOException if something fails while initializing the index.
     */
    void init(String indexPath) throws IOException;

    /**
     * Writes the contents of a user in the index.
     *
     * @param content   the published content.
     * @param contentId the user.
     *
     * @return the identifier of the new document.
     *
     * @throws IOException if something fails while writing the contents.
     */
    int indexText(String content, C contentId) throws IOException;

    /**
     * Closes the builder.
     *
     * @param indexPath path containing the index.
     *
     * @throws IOException if something fails while closing the index.
     */
    void close(String indexPath) throws IOException;
}
