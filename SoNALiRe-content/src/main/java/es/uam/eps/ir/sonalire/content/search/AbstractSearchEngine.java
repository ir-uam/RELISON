/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content.search;

import es.uam.eps.ir.sonalire.content.index.Index;

/**
 * Abstract implementation of a search engine.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class AbstractSearchEngine implements SearchEngine
{
    /**
     * The index in which to perform the search.
     */
    protected final Index<?> contentIndex;

    /**
     * Constructor.
     *
     * @param index the index in which to perform the search.
     */
    public AbstractSearchEngine(Index<?> index)
    {
        this.contentIndex = index;
    }
}
