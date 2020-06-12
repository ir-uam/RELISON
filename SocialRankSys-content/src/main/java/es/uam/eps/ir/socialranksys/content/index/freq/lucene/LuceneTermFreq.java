package es.uam.eps.ir.socialranksys.content.index.freq.lucene;

import es.uam.eps.ir.socialranksys.content.index.freq.TermFreq;
import org.apache.lucene.index.TermsEnum;

import java.io.IOException;

/**
 * Lucene implementation of the TermFreq object.
 * @author Pablo Castells.
 */
public class LuceneTermFreq implements TermFreq
{
    /**
     * The term.
     */
    TermsEnum terms;

    /**
     * Constructor
     * @param t the term.
     */
    public LuceneTermFreq (TermsEnum t)
    {
        terms = t;
    }

    @Override
    public String getTerm() throws IOException
    {
        return terms.term().utf8ToString();
    }

    @Override
    public long getFreq() throws IOException
    {
        return terms.totalTermFreq();
    }
}
