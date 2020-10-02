/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index.individual;

import es.uam.eps.ir.socialranksys.content.index.IndexBuilder;

import java.io.IOException;

/**
 * Individual content index builder.
 *
 * @param <C> type of the contents.
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface IndividualContentIndexBuilder<C, U> extends IndexBuilder<C>
{
    /**
     * Writes the contents of a user in the index.
     *
     * @param content   the published content.
     * @param contentId the user.
     *
     * @throws IOException if something fails while writing the contents.
     */
    void indexText(String content, C contentId, U userId) throws IOException;
}
