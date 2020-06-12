package es.uam.eps.ir.socialranksys.content.index.freq;

import java.io.IOException;

/**
 * Frequency vector for forward indexes.
 * @author Pablo Castells
 */
public interface FreqVector extends Iterable<TermFreq>
{
    /**
     * The number of terms in the vector.
     * @return the number of terms in the vector.
     * @throws IOException if something fails while reading the term vector.
     */
    long size() throws IOException;

    /**
     * The frequency of the term in the vector.
     * @param term the term.
     * @return the frequency of the term in the vector.
     * @throws IOException if something fails while reading the vector.
     */
    long getFreq(String term) throws IOException;
}
