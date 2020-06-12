package es.uam.eps.ir.socialranksys.content.index.freq.lucene;

import es.uam.eps.ir.socialranksys.content.index.freq.FreqVector;
import es.uam.eps.ir.socialranksys.content.index.freq.TermFreq;
import org.apache.lucene.index.Terms;

import java.io.IOException;
import java.util.Iterator;

/**
 * Lucene implementation of a term vector.
 * @author Pablo Castells
 */
public class LuceneFreqVector implements FreqVector
{
    /**
     * Lucene iterator.
     */
    LuceneFreqVectorIterator iterator;

    /**
     * Constructor.
     * @param terms the set of terms.
     * @throws IOException if something fails while creating the iterator.
     */
    public LuceneFreqVector(Terms terms) throws IOException
    {
        iterator = new LuceneFreqVectorIterator(terms);

    }

    @Override
    public long size()
    {
        return iterator.size;
    }

    @Override
    public Iterator<TermFreq> iterator() {
        return iterator;
    }

    @Override
    public long getFreq(String term) throws IOException
    {
        return iterator.getFreq(term);
    }
}
