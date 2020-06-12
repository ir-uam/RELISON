package es.uam.eps.ir.socialranksys.content.index.lucene;

import es.uam.eps.ir.socialranksys.content.index.ForwardIndex;
import es.uam.eps.ir.socialranksys.content.index.freq.FreqVector;
import es.uam.eps.ir.socialranksys.content.index.freq.lucene.LuceneFreqVector;
import org.ranksys.formats.parsing.Parser;

import java.io.IOException;

/**
 * Lucene implementation of a forward index.
 * @author Pablo Castells
 * @author Javier Sanz-Cruzado
 */
public class LuceneForwardIndex<C> extends LuceneIndex<C> implements ForwardIndex<C>
{
    /**
     * Constructor.
     * @param indexFolder folder containing the index.
     * @param uParser user parser.
     * @throws IOException if something fails while opening the index.
     */
    public LuceneForwardIndex(String indexFolder, Parser<C> uParser) throws IOException
    {
        super(indexFolder, uParser);
    }

    @Override
    public FreqVector getContentVector(int docID) throws IOException
    {
        return new LuceneFreqVector(index.getTermVector(docID, "text"));
    }

    @Override
    public long getTermFreq(String term, int docID) throws IOException
    {
        return getContentVector(docID).getFreq(term);
    }
}
