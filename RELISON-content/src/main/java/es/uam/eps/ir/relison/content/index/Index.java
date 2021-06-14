/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.content.index;

import es.uam.eps.ir.relison.content.index.structure.PostingsList;

import java.io.IOException;
import java.util.Collection;

/**
 * Interface for a content index.
 *
 * @param <C> type of the content identifier.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public interface Index<C> extends DocumentMap<C>
{
    /**
     * Obtains the posting list of a term.
     *
     * @param term the term to look up.
     *
     * @return the posting list of the term.
     *
     * @throws IOException if something fails while reading the posting list.
     */
    PostingsList getPostings(String term) throws IOException;

    /**
     * Obtains the set of all terms in the index dictionary.
     *
     * @return the collection of terms.
     *
     * @throws IOException if something fails while reading the list of terms.
     */
    Collection<String> getAllTerms() throws IOException;

    /**
     * Obtains the total frequency of a term in the collection.
     *
     * @param term the term to look up.
     *
     * @return the total frequency of the term in the collection.
     *
     * @throws IOException if something fails while reading the frequency.
     */
    long getTotalFreq(String term) throws IOException;

    /**
     * Obtains the number of documents containing the term.
     *
     * @param term the term to look up.
     *
     * @return the number of documents containing the term.
     *
     * @throws IOException if something fails while reading the doc frequency.
     */
    long getDocFreq(String term) throws IOException;
}
