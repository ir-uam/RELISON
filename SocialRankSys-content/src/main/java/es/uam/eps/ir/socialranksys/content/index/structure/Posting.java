package es.uam.eps.ir.socialranksys.content.index.structure;

import java.io.Serializable;

/**
 * Posting in an index.
 * @author Pablo Castells
 */
public class Posting implements Comparable<Posting>, Serializable
{
    int docID;
    long freq;

    /**
     * Constructor.
     * @param id document identifier.
     * @param f frequency of the term in the document.
     */
    public Posting(int id, long f)
    {
        docID = id;
        freq = f;
    }

    /**
     * Obtains the document identifier.
     * @return the identifier.
     */
    public int getDocID()
    {
        return docID;
    }

    /**
     * Obtains the frequency of the term in the document.
     * @return the frequency of the term in the document.
     */
    public long getFreq()
    {
        return freq;
    }

    /**
     * Compares two postings (by docId)
     * @param p the posting
     * @return this.docId - p.getDocId
     */
    public int compareTo(Posting p)
    {
        return getDocID() - p.getDocID();
    }
}
