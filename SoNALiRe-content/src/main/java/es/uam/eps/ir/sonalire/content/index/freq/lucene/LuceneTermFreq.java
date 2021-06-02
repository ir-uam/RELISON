/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content.index.freq.lucene;

import es.uam.eps.ir.sonalire.content.index.freq.TermFreq;
import org.apache.lucene.index.TermsEnum;

import java.io.IOException;

/**
 * Lucene implementation of the TermFreq object.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class LuceneTermFreq implements TermFreq
{
    /**
     * The term.
     */
    TermsEnum terms;

    /**
     * Constructor
     *
     * @param t the term.
     */
    public LuceneTermFreq(TermsEnum t)
    {
        terms = t;
    }

    @Override
    public String getTerm() throws IOException
    {
        return terms.term().utf8ToString();
    }

    @Override
    public long getFreq() throws IOException
    {
        return terms.totalTermFreq();
    }
}
