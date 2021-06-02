/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content.index.structure.lucene;

import es.uam.eps.ir.sonalire.content.index.structure.Posting;
import es.uam.eps.ir.sonalire.content.index.structure.PostingsList;
import org.apache.lucene.index.PostingsEnum;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

/**
 * Lucene posting list.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class LucenePostingsList implements PostingsList
{
    /**
     * The posting list.
     */
    protected PostingsEnum postings;
    /**
     * The length of the posting list
     */
    int length;

    /**
     * Constructor.
     *
     * @param postings the posting list.
     * @param length   the length of the posting list
     */
    public LucenePostingsList(PostingsEnum postings, int length)
    {
        this.postings = postings;
        this.length = length;
    }

    /**
     * Iterator for the list.
     *
     * @return the iterator if everything is OK, null otherwise.
     */
    @Override
    public Iterator<Posting> iterator()
    {
        try
        {
            return new LucenePostingsIterator(postings);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return Collections.emptyIterator();
        }
    }

    /**
     * Obtains the length of the list.
     *
     * @return the length of the posting list.
     */
    @Override
    public int size()
    {
        return length;
    }
}
