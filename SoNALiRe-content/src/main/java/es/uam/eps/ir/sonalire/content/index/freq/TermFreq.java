/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content.index.freq;

import java.io.IOException;

/**
 * Element for storing a term and its frequency.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public interface TermFreq
{
    /**
     * Obtains the term.
     *
     * @return the term.
     *
     * @throws IOException if something fails while reading the term.
     */
    String getTerm() throws IOException;

    /**
     * Obtains the frequency.
     *
     * @return the frequency.
     *
     * @throws IOException if something fails while obtaining the frequency.
     */
    long getFreq() throws IOException;
}
