/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index.structure;

import java.io.Serializable;

/**
 * Posting in an index.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class Posting implements Comparable<Posting>, Serializable
{
    /**
     * Frequency of the term associated to the posting in the document.
     */
    long freq;
    /**
     * Document identifier for the posting.
     */
    private int docID;

    /**
     * Constructor.
     *
     * @param id document identifier.
     * @param f  frequency of the term in the document.
     */
    public Posting(int id, long f)
    {
        docID = id;
        freq = f;
    }

    /**
     * Obtains the document identifier.
     *
     * @return the identifier.
     */
    public int getDocID()
    {
        return docID;
    }

    /**
     * Obtains the frequency of the term in the document.
     *
     * @return the frequency of the term in the document.
     */
    public long getFreq()
    {
        return freq;
    }

    /**
     * Compares two postings (by docId)
     *
     * @param p the posting
     *
     * @return this.docId - p.getDocId
     */
    public int compareTo(Posting p)
    {
        return getDocID() - p.getDocID();
    }
}
