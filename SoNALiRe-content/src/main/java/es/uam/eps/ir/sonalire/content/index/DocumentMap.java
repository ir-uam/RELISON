/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content.index;

import java.io.IOException;

/**
 * Document map containing information about the documents in an index.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public interface DocumentMap<C>
{
    /**
     * Obtains the user corresponding to an identifier.
     *
     * @param contentId document identifier.
     *
     * @return the content corresponding to such identifier.
     *
     * @throws IOException if something fails while reading the value.
     */
    C getContent(int contentId) throws IOException;

    /**
     * Obtains the identifier of a content in the index.
     *
     * @param content the content to look up.
     *
     * @return the identifier of the content.
     */
    int getContentId(C content);

    /**
     * Obtains the number of contents.
     *
     * @return the number of contents.
     */
    int numDocs();
}
