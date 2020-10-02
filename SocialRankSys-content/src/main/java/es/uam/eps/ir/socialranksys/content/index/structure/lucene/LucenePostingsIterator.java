/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index.structure.lucene;

import es.uam.eps.ir.socialranksys.content.index.structure.Posting;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.DocIdSetIterator;

import java.io.IOException;
import java.util.Iterator;

/**
 * Iterator of a Lucene posting list.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class LucenePostingsIterator implements Iterator<Posting>
{
    /**
     * The posting list.
     */
    protected PostingsEnum postings;
    /**
     * Current document for the iterator.
     */
    protected int currentDoc;

    /**
     * Constructor.
     *
     * @param p the postings.
     *
     * @throws IOException if something fails while reading the posting list.
     */
    public LucenePostingsIterator(PostingsEnum p) throws IOException
    {
        postings = p;
        if (p == null)
        {
            currentDoc = DocIdSetIterator.NO_MORE_DOCS;
        }
        else
        {
            currentDoc = postings.nextDoc();
        }
    }

    /**
     * Checks if we can advance the iterator.
     *
     * @return true if there is a next posting, false otherwise.
     */
    public boolean hasNext()
    {
        return currentDoc != DocIdSetIterator.NO_MORE_DOCS;
    }

    /**
     * Advances the posting list.
     *
     * @return the next posting in the list.
     */
    public Posting next()
    {
        try
        {
            Posting p = new Posting(postings.docID(), postings.freq());
            currentDoc = postings.nextDoc();
            return p;
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }
}