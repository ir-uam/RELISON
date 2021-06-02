/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content.index.structure.positional.lucene;

import es.uam.eps.ir.sonalire.content.index.structure.Posting;
import es.uam.eps.ir.sonalire.content.index.structure.lucene.LucenePostingsList;
import org.apache.lucene.index.PostingsEnum;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

/**
 * Positional posting list for Lucene.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class LucenePositionalPostingsList extends LucenePostingsList
{
    /**
     * Posting list.
     */
    PostingsEnum positionPostings;

    /**
     * Constructor.
     *
     * @param p1     the posting list.
     * @param p2     the positional posting list.
     * @param length the length of the posting list.
     */
    public LucenePositionalPostingsList(PostingsEnum p1, PostingsEnum p2, int length)
    {
        super(p1, length);
        positionPostings = p2;
    }

    @Override
    public Iterator<Posting> iterator()
    {
        try
        {
            return new LucenePositionalPostingsIterator(postings, positionPostings);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return Collections.emptyIterator();
        }
    }
}
