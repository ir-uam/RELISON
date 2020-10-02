/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index.lucene;

import es.uam.eps.ir.socialranksys.content.index.ForwardIndex;
import es.uam.eps.ir.socialranksys.content.index.freq.FreqVector;
import es.uam.eps.ir.socialranksys.content.index.freq.lucene.LuceneFreqVector;
import org.apache.lucene.index.Terms;
import org.ranksys.formats.parsing.Parser;

import java.io.IOException;

/**
 * Lucene implementation of a forward index.
 *
 * @param <C> Type of the contents.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class LuceneForwardIndex<C> extends LuceneIndex<C> implements ForwardIndex<C>
{
    /**
     * Constructor.
     *
     * @param indexFolder folder containing the index.
     * @param uParser     user parser.
     *
     * @throws IOException if something fails while opening the index.
     */
    public LuceneForwardIndex(String indexFolder, Parser<C> uParser) throws IOException
    {
        super(indexFolder, uParser);
    }

    @Override
    public FreqVector getContentVector(int docID) throws IOException
    {
        Terms terms = index.getTermVector(docID, "text");
        if (terms != null)
        {
            return new LuceneFreqVector(terms);
        }
        return null;
    }

    @Override
    public long getTermFreq(String term, int docID) throws IOException
    {
        return getContentVector(docID).getFreq(term);
    }
}
