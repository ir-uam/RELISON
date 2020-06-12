package es.uam.eps.ir.socialranksys.content.index.freq.lucene;

import es.uam.eps.ir.socialranksys.content.index.freq.TermFreq;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Iterator;

/**
 * Iterator for the Lucene frequency vector
 * @author Pablo Castells
 */
public class LuceneFreqVectorIterator implements Iterator<TermFreq>
{
    /**
     * The frequency vector.
     */
    TermsEnum terms;
    /**
     * Length of the vector.
     */
    long size;
    /**
     * Pointer of the iterator.
     */
    long pointer;

    public LuceneFreqVectorIterator(Terms t) throws IOException
    {
        terms = t.iterator();
        size = t.size();
        pointer = 0;
    }

    @Override
    public boolean hasNext()
    {
        return pointer < size;
    }

    @Override
    public TermFreq next() {
        try {
            terms.next();
            pointer++;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return new LuceneTermFreq(terms);
    }

    public long getFreq(String term) throws IOException
    {
        if (terms.seekExact(new BytesRef(term))) return terms.totalTermFreq();
        else return 0;
    }
}
