package es.uam.eps.ir.socialranksys.content.index;

import es.uam.eps.ir.socialranksys.content.index.freq.FreqVector;

import java.io.IOException;

/**
 * Interface for defining a forward index.
 * @author Pablo Castells
 * @author Javier Sanz-Cruzado
 */
public interface ForwardIndex<C> extends Index<C>
{
    /**
     * Obtains a user vector.
     * @param contentId identifier of the content in the index.
     * @return a frequency vector for the corresponding content.
     * @throws IOException if something fails while reading the index.
     */
    FreqVector getContentVector(int contentId) throws IOException;

    /**
     * Obtains the term frequency of a term in a user vector.
     * @param term the term to look up.
     * @param contentId the content identifier
     * @return the frequency of the term in the content vector
     * @throws IOException if something fails while reading the value.
     */
    long getTermFreq(String term, int contentId) throws IOException;
}
