package es.uam.eps.ir.socialranksys.content.index.structure.lucene;

import es.uam.eps.ir.socialranksys.content.index.structure.Posting;
import es.uam.eps.ir.socialranksys.content.index.structure.PostingsList;
import org.apache.lucene.index.PostingsEnum;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

/**
 * Lucene posting list.
 * @author Pablo Castells
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
     * @param postings the posting list.
     * @param length the length of the posting list
     */
    public LucenePostingsList(PostingsEnum postings, int length)
    {
        this.postings = postings;
        this.length = length;
    }

    /**
     * Iterator for the list.
     * @return the iterator if everything is OK, null otherwise.
     */
    @Override
    public Iterator<Posting> iterator()
    {
        try
        {
            return new LucenePostingsIterator(postings);
        } catch (IOException ex)
        {
            ex.printStackTrace();
            return Collections.emptyIterator();
        }
    }

    /**
     * Obtains the length of the list.
     * @return the length of the posting list.
     */
    @Override
    public int size()
    {
        return length;
    }
}
