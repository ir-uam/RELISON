/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index.structure;

/**
 * Interface for posting lists
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface PostingsList extends Iterable<Posting>
{
    /**
     * The size of the list.
     *
     * @return the size.
     */
    int size();
}
