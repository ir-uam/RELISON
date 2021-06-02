/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content.index.freq.impl;

import es.uam.eps.ir.sonalire.content.index.freq.TermFreq;

/**
 * Implementation of the TermFreq class
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ImplTermFreq implements TermFreq
{
    /**
     * The term
     */
    private final String term;
    /**
     * The frequency.
     */
    private final long freq;

    /**
     * Constructor.
     *
     * @param term the term.
     * @param freq the frequency of the term.
     */
    public ImplTermFreq(String term, long freq)
    {
        this.term = term;
        this.freq = freq;
    }

    @Override
    public String getTerm()
    {
        return term;
    }

    @Override
    public long getFreq()
    {
        return freq;
    }
}
