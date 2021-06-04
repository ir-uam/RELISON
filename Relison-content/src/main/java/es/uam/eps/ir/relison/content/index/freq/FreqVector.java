/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.content.index.freq;

import java.io.IOException;

/**
 * Frequency vector for forward indexes.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public interface FreqVector extends Iterable<TermFreq>
{
    /**
     * The number of terms in the vector.
     *
     * @return the number of terms in the vector.
     *
     * @throws IOException if something fails while reading the term vector.
     */
    long size() throws IOException;

    /**
     * The frequency of the term in the vector.
     *
     * @param term the term.
     *
     * @return the frequency of the term in the vector.
     *
     * @throws IOException if something fails while reading the vector.
     */
    long getFreq(String term) throws IOException;
}
