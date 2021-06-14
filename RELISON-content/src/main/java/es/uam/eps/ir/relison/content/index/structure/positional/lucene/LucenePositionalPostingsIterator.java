/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.content.index.structure.positional.lucene;

import es.uam.eps.ir.relison.content.index.structure.Posting;
import es.uam.eps.ir.relison.content.index.structure.lucene.LucenePostingsIterator;
import es.uam.eps.ir.relison.content.index.structure.positional.PositionalPosting;
import org.apache.lucene.index.PostingsEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Iterator for running over a Lucene positional posting list.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class LucenePositionalPostingsIterator extends LucenePostingsIterator
{
    /**
     * Positional postings.
     */
    PostingsEnum positionPostings;

    /**
     * Constructor.
     *
     * @param p1 posting list.
     * @param p2 positional posting list.
     *
     * @throws IOException if something fails while creating the iterator.
     */
    public LucenePositionalPostingsIterator(PostingsEnum p1, PostingsEnum p2) throws IOException
    {
        super(p1);
        positionPostings = p2;
    }

    /**
     * Obtains the next element in the posting list.
     *
     * @return the posting if everything is OK, null otherwise.
     */
    public Posting next()
    {
        super.next();
        try
        {
            positionPostings.nextDoc();
            List<Integer> positions = new ArrayList<>(positionPostings.freq());
            for (int i = 0; i < positionPostings.freq(); i++)
            {
                positions.add(positionPostings.nextPosition());
            }
            return new PositionalPosting(positionPostings.docID(), positionPostings.freq(), positions);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }
}
