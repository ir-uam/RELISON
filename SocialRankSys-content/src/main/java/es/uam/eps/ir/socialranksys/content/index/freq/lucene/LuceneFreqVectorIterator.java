/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index.freq.lucene;

import es.uam.eps.ir.socialranksys.content.index.freq.TermFreq;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Iterator;

/**
 * Iterator for the Lucene frequency vector
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class LuceneFreqVectorIterator implements Iterator<TermFreq>
{
    /**
     * The frequency vector.
     */
    TermsEnum terms;
    /**
     * Length of the vector.
     */
    long size;
    /**
     * Pointer of the iterator.
     */
    long pointer;

    public LuceneFreqVectorIterator(Terms t) throws IOException
    {
        terms = t.iterator();
        size = t.size();
        pointer = 0;
    }

    @Override
    public boolean hasNext()
    {
        return pointer < size;
    }

    @Override
    public TermFreq next()
    {
        try
        {
            terms.next();
            pointer++;
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return new LuceneTermFreq(terms);
    }

    /**
     * Given a term, obtains its frequency in the current position.
     * @param term the term.
     * @return the frequency in the position.
     * @throws IOException if something fails while retrieving the frequency vector.
     */
    public long getFreq(String term) throws IOException
    {
        if (terms.seekExact(new BytesRef(term)))
        {
            return terms.totalTermFreq();
        }
        else
        {
            return 0;
        }
    }
}
