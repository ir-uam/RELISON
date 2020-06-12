package es.uam.eps.ir.socialranksys.content.index.structure.positional.lucene;

import es.uam.eps.ir.socialranksys.content.index.structure.Posting;
import es.uam.eps.ir.socialranksys.content.index.structure.lucene.LucenePostingsList;
import org.apache.lucene.index.PostingsEnum;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

/**
 * Positional posting list for Lucene.
 * @author Pablo Castells
 */
public class LucenePositionalPostingsList extends LucenePostingsList
{
    /**
     * Posting list.
     */
    PostingsEnum positionPostings;

    public LucenePositionalPostingsList(PostingsEnum p1, PostingsEnum p2, int n) {
        super(p1, n);
        positionPostings = p2;
    }

    @Override
    public Iterator<Posting> iterator() {
        try {
            return new LucenePositionalPostingsIterator(postings, positionPostings);
        } catch (IOException ex) {
            ex.printStackTrace();
            return Collections.emptyIterator();
        }
    }
}
