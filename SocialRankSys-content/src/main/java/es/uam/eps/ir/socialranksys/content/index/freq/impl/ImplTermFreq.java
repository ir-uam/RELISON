package es.uam.eps.ir.socialranksys.content.index.freq.impl;

import es.uam.eps.ir.socialranksys.content.index.freq.TermFreq;

/**
 * Implementation of the TermFreq class
 * @author Javier Sanz-Cruzado
 */
public class ImplTermFreq implements TermFreq
{
    /**
     * The term
     */
    private final String term;
    /**
     * The frequency.
     */
    private final long freq;

    /**
     * Constructor.
     * @param term the term.
     * @param freq the frequency of the term.
     */
    public ImplTermFreq(String term, long freq)
    {
        this.term = term;
        this.freq = freq;
    }

    @Override
    public String getTerm()
    {
        return term;
    }

    @Override
    public long getFreq()
    {
        return freq;
    }
}
