/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.content.index.individual;

import es.uam.eps.ir.relison.content.index.Index;

import java.util.List;

/**
 * Index that stores individual pieces for each user.
 *
 * @param <C> type of the content identifiers.
 * @param <U> type of the user identifiers.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface IndividualContentIndex<C, U> extends Index<C>
{
    /**
     * Given a content identifier, obtains the user that created it.
     *
     * @param contentId the content identifier.
     *
     * @return the creator of the content.
     */
    U getUser(int contentId);

    /**
     * Given a user identifier, obtains the list of contents published by the user.
     *
     * @param user the user identifier
     *
     * @return the list of identifiers of the contents in the index.
     */
    List<Integer> getContents(U user);
}
