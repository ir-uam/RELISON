/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content.index.freq.lucene;

import es.uam.eps.ir.sonalire.content.index.freq.FreqVector;
import es.uam.eps.ir.sonalire.content.index.freq.TermFreq;
import org.apache.lucene.index.Terms;

import java.io.IOException;
import java.util.Iterator;

/**
 * Lucene implementation of a term vector.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class LuceneFreqVector implements FreqVector
{
    /**
     * Lucene iterator.
     */
    LuceneFreqVectorIterator iterator;

    /**
     * Constructor.
     *
     * @param terms the set of terms.
     *
     * @throws IOException if something fails while creating the iterator.
     */
    public LuceneFreqVector(Terms terms) throws IOException
    {
        iterator = new LuceneFreqVectorIterator(terms);
    }

    @Override
    public long size()
    {
        return iterator.size;
    }

    @Override
    public Iterator<TermFreq> iterator()
    {
        return iterator;
    }

    @Override
    public long getFreq(String term) throws IOException
    {
        return iterator.getFreq(term);
    }
}
