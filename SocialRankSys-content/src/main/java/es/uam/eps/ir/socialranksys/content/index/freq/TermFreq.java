package es.uam.eps.ir.socialranksys.content.index.freq;

import java.io.IOException;

/**
 * Element for storing a term and its frequency.
 * @author Pablo Castells
 */
public interface TermFreq
{
    /**
     * Obtains the term.
     * @return the term.
     * @throws IOException if something fails while reading the term.
     */
    String getTerm() throws IOException;

    /**
     * Obtains the frequency.
     * @return the frequency.
     * @throws IOException if something fails while obtaining the frequency.
     */
    long getFreq() throws IOException;
}
