/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.content.search;

import es.uam.eps.ir.relison.content.index.freq.FreqVector;

import java.io.IOException;
import java.util.Map;

/**
 * Interface defining the methods for a search engine.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface SearchEngine
{
    /**
     * Searches from a term frequency vector.
     *
     * @param termFreq the term frequency vector.
     *
     * @return a map containing the results of the search.
     *
     * @throws IOException if something fails while searching.
     */
    Map<Integer, Double> search(FreqVector termFreq) throws IOException;

    /**
     * Searches from a query.
     *
     * @param query the query.
     *
     * @return a map containing the results of the search.
     *
     * @throws IOException if something fails while searching.
     */
    Map<Integer, Double> search(String query) throws IOException;
}
